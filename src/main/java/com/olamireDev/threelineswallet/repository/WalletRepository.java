package com.olamireDev.threelineswallet.repository;

import com.olamireDev.threelineswallet.constants.Currency;
import com.olamireDev.threelineswallet.data.model.Wallet;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    boolean existsByForUser_IdAndCurrency(Long id, Currency currency);

    Optional<Wallet> findByIdAndForUser_Id(Long id, Long user_id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    Optional<Wallet> findWalletByForUser_IdAndCurrency(Long forUser_id, Currency currency);

}
