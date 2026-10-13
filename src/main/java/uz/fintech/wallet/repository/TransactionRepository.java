package uz.fintech.wallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.fintech.wallet.entity.TransactionEntity;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey);

}
