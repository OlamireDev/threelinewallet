package com.olamireDev.threelineswallet.service;

import com.olamireDev.threelineswallet.constants.Currency;
import com.olamireDev.threelineswallet.constants.TransactionKeyState;
import com.olamireDev.threelineswallet.constants.TransactionType;
import com.olamireDev.threelineswallet.data.dto.TransactionInfoResponseDTO;
import com.olamireDev.threelineswallet.data.dto.TransactionResponseDTO;
import com.olamireDev.threelineswallet.data.dto.UserTransactionRequestDTO;
import com.olamireDev.threelineswallet.data.model.TransactionEntity;
import com.olamireDev.threelineswallet.data.model.TransactionKeyEntity;
import com.olamireDev.threelineswallet.data.model.UserEntity;
import com.olamireDev.threelineswallet.data.model.Wallet;
import com.olamireDev.threelineswallet.repository.TransactionKeyRepository;
import com.olamireDev.threelineswallet.repository.TransactionRepository;
import com.olamireDev.threelineswallet.repository.UserRepository;
import com.olamireDev.threelineswallet.repository.WalletRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionKeyRepository transactionKeyRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private TransactionService transactionService;

    private MockedStatic<SecurityContextHolder> securityContextHolderMock;

    @AfterEach
    void tearDown() {
        if (securityContextHolderMock != null) {
            securityContextHolderMock.close();
        }
    }

    private void mockAuthenticatedUser(String userIdAsString) {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(userIdAsString);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        securityContextHolderMock = Mockito.mockStatic(SecurityContextHolder.class);
        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
    }


    @Test
    void createTransactionSession_whenUserExists_savesAndReturnsKeyId() {
        mockAuthenticatedUser("5");
        var user = UserEntity.builder().id(5L).name("Jane").build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(transactionKeyRepository.save(any(TransactionKeyEntity.class)))
                .thenAnswer(inv -> {
                    TransactionKeyEntity passed = inv.getArgument(0);
                    return TransactionKeyEntity.builder().id("generated-key-id").user(passed.getUser()).build();
                });

        String result = transactionService.createTransactionSession();

        assertThat(result).isEqualTo("generated-key-id");
        ArgumentCaptor<TransactionKeyEntity> captor = ArgumentCaptor.forClass(TransactionKeyEntity.class);
        verify(transactionKeyRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
    }

    @Test
    void createTransactionSession_whenUserDoesNotExist_throwsAndDoesNotCreateKey() {
        mockAuthenticatedUser("404");
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.createTransactionSession())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verifyNoInteractions(transactionKeyRepository);
    }


    @Test
    void doUserTransaction_whenSourceAndDestinationWalletsAreTheSame_throwsBeforeAnyLookup() {
        mockAuthenticatedUser("1");
        var request = new UserTransactionRequestDTO("key-1", 10L, BigDecimal.TEN, 10L);

        assertThatThrownBy(() -> transactionService.doUserTransaction(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cannot transfer to the same wallet");

        verifyNoInteractions(userRepository, transactionKeyRepository, walletRepository, transactionRepository);
    }

    @Test
    void doUserTransaction_whenUserDoesNotExist_throwsUserNotFound() {
        mockAuthenticatedUser("1");
        when(userRepository.existsById(1L)).thenReturn(false);
        var request = new UserTransactionRequestDTO("key-1", 10L, BigDecimal.TEN, 20L);

        assertThatThrownBy(() -> transactionService.doUserTransaction(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verifyNoInteractions(transactionKeyRepository, walletRepository, transactionRepository);
    }

    @Test
    void doUserTransaction_whenTransactionKeyInvalidOrExpired_throws() {
        mockAuthenticatedUser("1");
        when(userRepository.existsById(1L)).thenReturn(true);
        when(transactionKeyRepository.findByIdAndUser_IdAndTransactionKeyStateAndExpiryDateIsAfter(
                eq("bad-key"), eq(1L), eq(TransactionKeyState.CREATED), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        var request = new UserTransactionRequestDTO("bad-key", 10L, BigDecimal.TEN, 20L);

        assertThatThrownBy(() -> transactionService.doUserTransaction(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid transaction key");

        verifyNoInteractions(walletRepository, transactionRepository);
    }

    @Test
    void doUserTransaction_whenDebitWalletDoesNotBelongToUser_throws() {
        mockAuthenticatedUser("1");
        when(userRepository.existsById(1L)).thenReturn(true);

        var transactionKey = TransactionKeyEntity.builder().id("key-1").build();
        when(transactionKeyRepository.findByIdAndUser_IdAndTransactionKeyStateAndExpiryDateIsAfter(
                anyString(), eq(1L), eq(TransactionKeyState.CREATED), any(LocalDateTime.class)))
                .thenReturn(Optional.of(transactionKey));
        when(transactionKeyRepository.save(any(TransactionKeyEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var otherUser = UserEntity.builder().id(999L).build();
        var debitWallet = Wallet.builder().id(10L).forUser(otherUser).currency(Currency.NGN)
                .balance(BigDecimal.valueOf(1000)).build();
        when(walletRepository.findByIdForTransaction(10L)).thenReturn(Optional.of(debitWallet));

        var request = new UserTransactionRequestDTO("key-1", 10L, BigDecimal.TEN, 20L);

        assertThatThrownBy(() -> transactionService.doUserTransaction(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("does not belong to the user");

        verifyNoInteractions(transactionRepository);
    }

    @Test
    void doUserTransaction_whenCurrenciesDiffer_throws() {
        mockAuthenticatedUser("1");
        when(userRepository.existsById(1L)).thenReturn(true);

        var transactionKey = TransactionKeyEntity.builder().id("key-1").build();
        when(transactionKeyRepository.findByIdAndUser_IdAndTransactionKeyStateAndExpiryDateIsAfter(
                anyString(), eq(1L), eq(TransactionKeyState.CREATED), any(LocalDateTime.class)))
                .thenReturn(Optional.of(transactionKey));
        when(transactionKeyRepository.save(any(TransactionKeyEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var user = UserEntity.builder().id(1L).build();
        var debitWallet = Wallet.builder().id(10L).forUser(user).currency(Currency.NGN)
                .balance(BigDecimal.valueOf(1000)).build();
        var creditWallet = Wallet.builder().id(20L).currency(Currency.USD)
                .balance(BigDecimal.ZERO).build();
        when(walletRepository.findByIdForTransaction(10L)).thenReturn(Optional.of(debitWallet));
        when(walletRepository.findByIdForTransaction(20L)).thenReturn(Optional.of(creditWallet));

        var request = new UserTransactionRequestDTO("key-1", 10L, BigDecimal.TEN, 20L);

        assertThatThrownBy(() -> transactionService.doUserTransaction(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not in the same currency");
    }

    @Test
    void doUserTransaction_happyPath_movesFundsAndReturnsSuccessResponse() {
        mockAuthenticatedUser("1");
        when(userRepository.existsById(1L)).thenReturn(true);

        var transactionKey = TransactionKeyEntity.builder().id("key-1")
                .transactionKeyState(TransactionKeyState.CREATED).build();
        when(transactionKeyRepository.findByIdAndUser_IdAndTransactionKeyStateAndExpiryDateIsAfter(
                anyString(), eq(1L), eq(TransactionKeyState.CREATED), any(LocalDateTime.class)))
                .thenReturn(Optional.of(transactionKey));
        when(transactionKeyRepository.save(any(TransactionKeyEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var user = UserEntity.builder().id(1L).build();
        var debitWallet = Wallet.builder().id(10L).forUser(user).currency(Currency.NGN)
                .balance(BigDecimal.valueOf(1000)).build();
        var creditWallet = Wallet.builder().id(20L).currency(Currency.NGN)
                .balance(BigDecimal.valueOf(200)).build();
        when(walletRepository.findByIdForTransaction(10L)).thenReturn(Optional.of(debitWallet));
        when(walletRepository.findByIdForTransaction(20L)).thenReturn(Optional.of(creditWallet));

        when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(inv -> {
            TransactionEntity passed = inv.getArgument(0);
            passed.setId(passed.getTransactionType() == TransactionType.DEBIT ? 501L : 502L);
            return passed;
        });

        var request = new UserTransactionRequestDTO("key-1", 10L, BigDecimal.valueOf(300), 20L);

        TransactionResponseDTO response = transactionService.doUserTransaction(request);

        assertThat(response.success()).isTrue();
        assertThat(response.transactionRef()).isNotBlank();

        ArgumentCaptor<List<Wallet>> walletsCaptor = ArgumentCaptor.forClass(List.class);
        verify(walletRepository).saveAll(walletsCaptor.capture());
        var savedWallets = walletsCaptor.getValue();
        var savedDebit = savedWallets.stream().filter(w -> w.getId().equals(10L)).findFirst().orElseThrow();
        var savedCredit = savedWallets.stream().filter(w -> w.getId().equals(20L)).findFirst().orElseThrow();
        assertThat(savedDebit.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(700));
        assertThat(savedCredit.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(500));

        assertThat(transactionKey.getTransactionKeyState()).isEqualTo(TransactionKeyState.USED);

        ArgumentCaptor<TransactionEntity> txCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository, times(2)).save(txCaptor.capture());
        var savedTypes = txCaptor.getAllValues().stream().map(TransactionEntity::getTransactionType).toList();
        assertThat(savedTypes).containsExactlyInAnyOrder(TransactionType.DEBIT, TransactionType.CREDIT);
    }

    @Test
    void doUserTransaction_whenInsufficientFunds_throwsAndDoesNotMoveBalances() {
        mockAuthenticatedUser("1");
        when(userRepository.existsById(1L)).thenReturn(true);

        var transactionKey = TransactionKeyEntity.builder().id("key-1")
                .transactionKeyState(TransactionKeyState.CREATED).build();
        when(transactionKeyRepository.findByIdAndUser_IdAndTransactionKeyStateAndExpiryDateIsAfter(
                anyString(), eq(1L), eq(TransactionKeyState.CREATED), any(LocalDateTime.class)))
                .thenReturn(Optional.of(transactionKey));
        when(transactionKeyRepository.save(any(TransactionKeyEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var user = UserEntity.builder().id(1L).build();
        var debitWallet = Wallet.builder().id(10L).forUser(user).currency(Currency.NGN)
                .balance(BigDecimal.valueOf(50)).build();
        var creditWallet = Wallet.builder().id(20L).currency(Currency.NGN)
                .balance(BigDecimal.ZERO).build();
        when(walletRepository.findByIdForTransaction(10L)).thenReturn(Optional.of(debitWallet));
        when(walletRepository.findByIdForTransaction(20L)).thenReturn(Optional.of(creditWallet));
        when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new UserTransactionRequestDTO("key-1", 10L, BigDecimal.valueOf(300), 20L);

        assertThatThrownBy(() -> transactionService.doUserTransaction(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient funds");

        verify(walletRepository, never()).saveAll(anyList());
    }


    @Test
    void getWalletTransactionHistory_whenWalletOwnedByUser_returnsMappedHistory() {
        mockAuthenticatedUser("1");
        when(userRepository.existsById(1L)).thenReturn(true);

        var user = UserEntity.builder().id(1L).build();
        var wallet = Wallet.builder().id(10L).forUser(user).currency(Currency.NGN).build();
        when(walletRepository.findByIdAndForUser_Id(10L, 1L)).thenReturn(Optional.of(wallet));

        var tx = TransactionEntity.builder().id(501L).transactionType(TransactionType.DEBIT)
                .amount(BigDecimal.valueOf(300)).build();
        when(transactionRepository.findByPrimaryWallet_IdOrderByCreatedAtDesc(eq(10L), any(Limit.class)))
                .thenReturn(List.of(tx));

        List<TransactionInfoResponseDTO> result = transactionService.getWalletTransactionHistory(10L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().type()).isEqualTo(TransactionType.DEBIT);
        assertThat(result.getFirst().amount()).isEqualByComparingTo(BigDecimal.valueOf(300));
    }

    @Test
    void getWalletTransactionHistory_whenUserDoesNotExist_throws() {
        mockAuthenticatedUser("404");
        when(userRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> transactionService.getWalletTransactionHistory(10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verifyNoInteractions(walletRepository, transactionRepository);
    }

    @Test
    void getWalletTransactionHistory_whenWalletNotFoundForUser_throws() {
        mockAuthenticatedUser("1");
        when(userRepository.existsById(1L)).thenReturn(true);
        when(walletRepository.findByIdAndForUser_Id(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getWalletTransactionHistory(10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Wallet not found");

        verifyNoInteractions(transactionRepository);
    }
}