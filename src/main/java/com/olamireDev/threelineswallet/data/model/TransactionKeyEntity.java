package com.olamireDev.threelineswallet.data.model;

import com.olamireDev.threelineswallet.constants.TransactionKeyState;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "transaction_keys", indexes = {
        @Index(name  = "tx_user_tx", columnList = "user_id, id", unique = true)
})
public class TransactionKeyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    private TransactionKeyState transactionKeyState;

    @Builder.Default
    private LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(5);

    @Version
    private int version;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;


}
