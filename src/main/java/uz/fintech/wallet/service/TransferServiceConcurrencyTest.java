package uz.fintech.wallet.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uz.fintech.wallet.entity.LedgerEntry;
import uz.fintech.wallet.entity.TransactionEntity;
import uz.fintech.wallet.entity.User;
import uz.fintech.wallet.entity.Wallet;
import uz.fintech.wallet.enums.TransactionStatus;
import uz.fintech.wallet.enums.WalletStatus;
import uz.fintech.wallet.enums.WalletType;
import uz.fintech.wallet.repository.LedgerEntryRepository;
import uz.fintech.wallet.repository.TransactionRepository;
import uz.fintech.wallet.repository.UserRepository;
import uz.fintech.wallet.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TransferServiceConcurrencyTest {

    @Autowired
    TransferService transferService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    WalletRepository walletRepository;
    @Autowired
    TransactionRepository transactionRepository;
    @Autowired
    LedgerEntryRepository ledgerEntryRepository;

    private Long systemWalletId;
    private Long aliWalletId;
    private Long valiWalletId;

    @BeforeEach
    void setUp() {
        ledgerEntryRepository.deleteAll();
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();

        systemWalletId = createWallet("SYSTEM", "system", "SYSTEM-CASH", WalletType.SYSTEM);
        aliWalletId = createWallet("Ali", "901", "1001", WalletType.USER);
        valiWalletId = createWallet("Vali", "902", "1002", WalletType.USER);
    }

    private Long createWallet(String name, String phone, String number, WalletType type) {
        User user = userRepository.save(new User(name, phone, "hash"));
        Wallet wallet = new Wallet(user, number, type, "UZS", BigDecimal.ZERO, WalletStatus.ACTIVE);
        return walletRepository.save(wallet).getId();
    }

    @Test
    void topUpAddsMoneyToAli(){
        TransactionEntity result = transferService.transfer(systemWalletId, aliWalletId, new BigDecimal("1000"), "topup-1");
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.SUCCESS);

        BigDecimal aliBalance = walletRepository.findById(aliWalletId).orElseThrow().getBalance();
        assertThat(aliBalance).isEqualByComparingTo("1000");

        BigDecimal systemBalance = walletRepository.findById(systemWalletId).orElseThrow().getBalance();
        assertThat(systemBalance).isEqualByComparingTo("-1000");
    }

    @Test
    void twentyParallelTransfers() throws Exception {
        transferService.transfer(systemWalletId, aliWalletId, new BigDecimal("1000"), "topup-1");
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startSignal = new CountDownLatch(1);
        List<Future<TransactionEntity>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            String key = "parallel-" + i;
            futures.add(executor.submit(() -> {
                startSignal.await();
                return transferService.transfer(aliWalletId, valiWalletId, new BigDecimal("100"), key);
            }));
        }
        startSignal.countDown();

        int success = 0;
        int failed = 0;
        for (Future<TransactionEntity> future : futures) {
            TransactionEntity result = future.get();
            if (result.getStatus() == TransactionStatus.SUCCESS) {
                success++;
            } else {
                failed++;
            }
        }
        executor.shutdown();
        assertThat(success).isEqualTo(10);
        assertThat(failed).isEqualTo(10);
        BigDecimal aliBalance = walletRepository.findById(aliWalletId).orElseThrow().getBalance();
        BigDecimal valiBalance = walletRepository.findById(valiWalletId).orElseThrow().getBalance();
        BigDecimal systemBalance = walletRepository.findById(systemWalletId).orElseThrow().getBalance();
        assertThat(aliBalance).isEqualByComparingTo("0");
        assertThat(valiBalance).isEqualByComparingTo("1000");
        assertThat(systemBalance).isEqualByComparingTo("-1000");

        BigDecimal ledgerTotal = ledgerEntryRepository.findAll().stream()
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(ledgerTotal).isEqualByComparingTo("0");

        assertBalanceMatchesLedger(systemWalletId);
        assertBalanceMatchesLedger(aliWalletId);
        assertBalanceMatchesLedger(valiWalletId);
    }

    private void assertBalanceMatchesLedger(Long walletId){
        BigDecimal balance = walletRepository.findById(walletId).orElseThrow().getBalance();
        BigDecimal ledgerSum = ledgerEntryRepository.sumByWalletId(walletId);
        assertThat(balance).isEqualByComparingTo(ledgerSum);
    }
}