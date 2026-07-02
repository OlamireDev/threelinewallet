package com.olamireDev.threelineswallet.repository;

import com.olamireDev.threelineswallet.data.model.TransactionEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    List<TransactionEntity> findByPrimaryWallet_IdOrderByCreatedAtDesc(Long primaryWalletId, Limit limit);

}
