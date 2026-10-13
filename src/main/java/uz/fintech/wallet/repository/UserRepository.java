package uz.fintech.wallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.fintech.wallet.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
