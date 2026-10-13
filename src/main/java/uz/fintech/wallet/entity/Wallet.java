package uz.fintech.wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import uz.fintech.wallet.enums.WalletStatus;
import uz.fintech.wallet.enums.WalletType;

import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Wallet extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "number", nullable = false, unique = true, length = 20)
    private String number;

    @Column(name = "wallet_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private WalletType walletType;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private WalletStatus status;
}
