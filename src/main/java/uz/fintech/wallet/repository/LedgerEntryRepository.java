package uz.fintech.wallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.fintech.wallet.entity.LedgerEntry;

import java.math.BigDecimal;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    @Query("select coalesce(sum(l.amount), 0) from LedgerEntry l where l.wallet.id = :walletId")
    BigDecimal sumByWalletId(@Param("walletId") Long walletId);

}
