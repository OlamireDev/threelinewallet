package com.olamireDev.threelineswallet.repository;

import com.olamireDev.threelineswallet.constants.TransactionKeyState;
import com.olamireDev.threelineswallet.data.model.TransactionKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TransactionKeyRepository extends JpaRepository<TransactionKeyEntity, String> {

    Optional<TransactionKeyEntity> findByIdAndUser_IdAndTransactionKeyStateAndExpiryDateIsAfter(String id, Long user_id,
                                                                          TransactionKeyState transactionKeyState,
                                                                          LocalDateTime expiryDate);

}
