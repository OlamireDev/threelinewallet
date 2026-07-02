package com.olamireDev.threelineswallet.service;

import com.olamireDev.threelineswallet.constants.Currency;
import com.olamireDev.threelineswallet.data.dto.GetWalletInfoResponseDTO;
import com.olamireDev.threelineswallet.data.model.UserEntity;
import com.olamireDev.threelineswallet.data.model.Wallet;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletService walletService;

    private MockedStatic<SecurityContextHolder> securityContextHolderMock;

    @AfterEach
    void tearDown() {
        if (securityContextHolderMock != null) {
            securityContextHolderMock.close();
        }
    }


    @Test
    void createDefaultWalletForUser_whenWalletAlreadyExists_doesNotCreateNewWallet() {
        var user = UserEntity.builder().id(1L).name("Jane Doe").build();
        when(walletRepository.existsByForUser_Id(1L)).thenReturn(true);

        walletService.createDefaultWalletForUser(user);

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void createDefaultWalletForUser_whenNoWalletExists_createsWalletWithDefaults() {
        var user = UserEntity.builder().id(2L).name("John Smith").build();
        when(walletRepository.existsByForUser_Id(2L)).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        walletService.createDefaultWalletForUser(user);

        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(captor.capture());

        Wallet savedWallet = captor.getValue();
        assertThat(savedWallet.getForUser()).isEqualTo(user);
        assertThat(savedWallet.getCurrency()).isEqualTo(Currency.NGN);
        assertThat(savedWallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void createDefaultWalletForUser_whenRepositoryThrows_propagatesException() {
        var user = UserEntity.builder().id(3L).name("Error User").build();
        when(walletRepository.existsByForUser_Id(3L)).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> walletService.createDefaultWalletForUser(user))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db down");
    }

    @Test
    void getUserDefaultWalletInfo_whenWalletExists_returnsMappedDto() {
        mockAuthenticatedUser("10");

        var wallet = Wallet.builder()
                .id(99L)
                .balance(BigDecimal.valueOf(500))
                .currency(Currency.NGN)
                .build();
        when(walletRepository.findWalletByForUser_IdAndCurrency(10L, Currency.NGN))
                .thenReturn(Optional.of(wallet));

        GetWalletInfoResponseDTO result = walletService.getUserDefaultWalletInfo();

        assertThat(result.walletId()).isEqualTo(99L);
        assertThat(result.balance()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(result.currency()).isEqualTo(Currency.NGN.getCurrencyName());
    }

    @Test
    void getUserDefaultWalletInfo_whenNoWalletFound_throwsRuntimeException() {
        mockAuthenticatedUser("11");
        when(walletRepository.findWalletByForUser_IdAndCurrency(11L, Currency.NGN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getUserDefaultWalletInfo())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No NGN wallet for user with id 11");
    }

    @Test
    void getUserDefaultWalletInfo_whenAuthenticationNameNotNumeric_throwsException() {
        mockAuthenticatedUser("not-a-number");

        assertThatThrownBy(() -> walletService.getUserDefaultWalletInfo())
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(walletRepository);
    }

    private void mockAuthenticatedUser(String userIdAsString) {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(userIdAsString);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        securityContextHolderMock = Mockito.mockStatic(SecurityContextHolder.class);
        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
    }
}