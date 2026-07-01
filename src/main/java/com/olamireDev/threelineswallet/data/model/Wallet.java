package com.olamireDev.threelineswallet.data.model;

import com.olamireDev.threelineswallet.constants.Currency;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet",
        uniqueConstraints = @UniqueConstraint(
        name = "uk_wallet_user_currency",
        columnNames = {"for_user_id", "currency"}
))
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "for_user_id")
    private UserEntity forUser;

    @Enumerated(EnumType.STRING)
    private Currency currency =  Currency.NGN;

    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
