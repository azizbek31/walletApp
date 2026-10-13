package uz.fintech.wallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.fintech.wallet.entity.LedgerEntry;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

}
