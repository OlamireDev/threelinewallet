package com.olamireDev.threelineswallet.service;

import com.olamireDev.threelineswallet.constants.TransactionKeyState;
import com.olamireDev.threelineswallet.constants.TransactionType;
import com.olamireDev.threelineswallet.data.dto.TransactionInfoResponseDTO;
import com.olamireDev.threelineswallet.data.dto.TransactionResponseDTO;
import com.olamireDev.threelineswallet.data.dto.UserTransactionRequestDTO;
import com.olamireDev.threelineswallet.data.model.TransactionEntity;
import com.olamireDev.threelineswallet.data.model.TransactionKeyEntity;
import com.olamireDev.threelineswallet.repository.TransactionKeyRepository;
import com.olamireDev.threelineswallet.repository.TransactionRepository;
import com.olamireDev.threelineswallet.repository.UserRepository;
import com.olamireDev.threelineswallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionKeyRepository transactionKeyRepository;

    private final TransactionRepository transactionRepository;

    private final UserRepository userRepository;

    private final WalletRepository walletRepository;

    public String createTransactionSession(){
        try{
            log.info("Creating a new transaction");
            var userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
            var user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
            log.info("Generating transaction key for user: {}", userId);
            return transactionKeyRepository.save(TransactionKeyEntity.builder()
                    .user(user)
                    .build()).getId();
        } catch (Exception e) {
            log.error("An error occurred while creating a transaction {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(noRollbackFor = Exception.class)
    public TransactionResponseDTO doUserTransaction(UserTransactionRequestDTO requestDTO){
        try{
            log.info("Creating a new transaction {}", requestDTO);

            if(requestDTO.getUserWalletId().equals(requestDTO.getCreditedWalletId())){
                throw new RuntimeException("Failed Transaction: User cannot transfer to the same wallet");
            }

            var userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
            if(!userRepository.existsById(userId)) throw new RuntimeException("User not found");

            //validate key
            var transactionKey = transactionKeyRepository.findByIdAndUser_IdAndTransactionKeyStateAndExpiryDateIsAfter(
                    requestDTO.getTransactionKey(),
                    userId, TransactionKeyState.CREATED, LocalDateTime.now()).orElseThrow(() -> new RuntimeException("Failed Transaction: Invalid transaction key"));

            transactionKey.setTransactionKeyState(TransactionKeyState.USED);
            transactionKey =transactionKeyRepository.save(transactionKey);

            var debitWallet = walletRepository.findByIdForTransaction(requestDTO.getUserWalletId())
                    .orElseThrow(() -> new RuntimeException("Failed Transaction: User wallet not found"));

            if(!debitWallet.getForUser().getId().equals(userId)){
                throw new RuntimeException("Failed Transaction: User wallet does not belong to the user");
            }

            var creditWallet = walletRepository.findByIdForTransaction(requestDTO.getCreditedWalletId())
                    .orElseThrow(() -> new RuntimeException("Failed Transaction: Destination wallet not found"));

            if(!creditWallet.getCurrency().equals(debitWallet.getCurrency())){
                throw new RuntimeException("Failed Transaction: Wallets are not in the same currency...development in progress");
            }

            var debitTransaction = transactionRepository.save(TransactionEntity.builder()
                    .primaryWallet(debitWallet)
                    .secondaryWallet(creditWallet)
                    .transactionKeyEntity(transactionKey)
                    .transactionType(TransactionType.DEBIT)
                    .amount(requestDTO.getAmount())
                    .build());

            var creditTransaction = transactionRepository.save(TransactionEntity.builder()
                    .primaryWallet(creditWallet)
                    .secondaryWallet(debitWallet)
                    .transactionKeyEntity(transactionKey)
                    .transactionType(TransactionType.CREDIT)
                    .amount(requestDTO.getAmount())
                    .build());

            debitTransaction.setLinkedTransaction(creditTransaction);
            creditTransaction.setLinkedTransaction(debitTransaction);

            transactionRepository.saveAll(List.of(debitTransaction, creditTransaction));

            if(requestDTO.getAmount().compareTo(debitWallet.getBalance()) > 0){
                throw new RuntimeException("Failed Transaction: Insufficient funds");
            }

            debitWallet.setBalance(debitWallet.getBalance().subtract(requestDTO.getAmount()));

            creditWallet.setBalance(creditWallet.getBalance().add(requestDTO.getAmount()));

            walletRepository.saveAll(List.of(debitWallet, creditWallet));

            return new TransactionResponseDTO(true,
                    Base64.getEncoder().encodeToString(debitTransaction.getId().toString().getBytes()));
        } catch (Exception e) {
            log.error("An error occurred while creating a transaction {}", e.getMessage(), e);
            throw e;
        }
    }

    public List<TransactionInfoResponseDTO> getWalletTransactionHistory(Long walletId){
        try {
            var userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
            if(!userRepository.existsById(userId))  throw new RuntimeException("User not found");

            var wallet = walletRepository.findByIdAndForUser_Id(walletId, userId).orElseThrow(() -> new RuntimeException("Wallet not found"));
            if(!wallet.getForUser().getId().equals(userId)){
                throw new RuntimeException("User does not own this wallet");
            }
            return transactionRepository.findByPrimaryWallet_IdOrderByCreatedAtDesc(walletId, Limit.of(100))
                    .stream()
                    .map(TransactionInfoResponseDTO::fromEntity).toList();
        }catch (Exception e){
            log.error("An error occurred while getting user wallet transactions {}", e.getMessage(), e);
            throw e;
        }
    }

}
