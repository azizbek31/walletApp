package uz.fintech.wallet;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import uz.fintech.wallet.service.TransferService;

import java.math.BigDecimal;

@SpringBootApplication
@EnableJpaAuditing
public class WalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletApplication.class, args);
    }

    @Bean
    CommandLineRunner demo(TransferService transferService) {
        return args -> {
            var tx = transferService.transfer(1L, 3L, new BigDecimal("120000"), "transfer-001");
            System.out.println(tx);
        };
    }
}
