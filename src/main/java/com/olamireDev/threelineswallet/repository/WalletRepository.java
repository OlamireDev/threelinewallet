package com.olamireDev.threelineswallet.repository;

import com.olamireDev.threelineswallet.constants.Currency;
import com.olamireDev.threelineswallet.data.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    boolean existsByForUser_Id(Long id);

    Optional<Wallet> findWalletByForUser_IdAndCurrency(Long forUser_id, Currency currency);

}
