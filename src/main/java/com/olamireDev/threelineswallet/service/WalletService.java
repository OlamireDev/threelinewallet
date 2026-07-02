package com.olamireDev.threelineswallet.service;

import com.olamireDev.threelineswallet.constants.Currency;
import com.olamireDev.threelineswallet.data.dto.GetWalletInfoResponseDTO;
import com.olamireDev.threelineswallet.data.model.UserEntity;
import com.olamireDev.threelineswallet.data.model.Wallet;
import com.olamireDev.threelineswallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;

    public void createDefaultWalletForUser(UserEntity user){
        try {
            if(walletRepository.existsByForUser_Id(user.getId())){
                log.info("Wallet already exists for user with id {}", user.getId());
                return;
            }
            var newWallet = walletRepository.save(Wallet.builder()
                    .forUser(user)
                    .build());
            log.info("Wallet created for user with id {} with balance: {}", user.getId(),  newWallet.getBalance());
        }catch (Exception e){
            log.error("An error occurred while creating an account for user {}", user.getId());
            throw e;
        }
    }

    public GetWalletInfoResponseDTO getUserDefaultWalletInfo(){
        try {
            var userId = Long.parseLong(SecurityContextHolder.getContext().getAuthentication().getName());
            if(Objects.isNull(userId)){
                throw new RuntimeException("User is not logged in");
            }
            log.info("Getting wallet info for user with id {}", userId);
            var walletOpt = walletRepository.findWalletByForUser_IdAndCurrency(userId, Currency.NGN);
            if(walletOpt.isEmpty()){
                throw new RuntimeException("No NGN wallet for user with id " + userId);
            }
            var wallet = walletOpt.get();
            return GetWalletInfoResponseDTO.builder()
                    .walletId(wallet.getId())
                    .balance(wallet.getBalance())
                    .currency(wallet.getCurrency().getCurrencyName())
                    .build();
        }catch (Exception e){
            log.error("An error occurred while getting user default wallet info {}", e.getMessage(), e);
            throw e;
        }
    }

}
