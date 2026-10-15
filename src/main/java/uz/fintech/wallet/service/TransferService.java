package uz.fintech.wallet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.fintech.wallet.entity.LedgerEntry;
import uz.fintech.wallet.entity.TransactionEntity;
import uz.fintech.wallet.entity.Wallet;
import uz.fintech.wallet.enums.TransactionStatus;
import uz.fintech.wallet.enums.WalletStatus;
import uz.fintech.wallet.enums.WalletType;
import uz.fintech.wallet.exception.WalletNotFoundException;
import uz.fintech.wallet.repository.LedgerEntryRepository;
import uz.fintech.wallet.repository.TransactionRepository;
import uz.fintech.wallet.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional
    public TransactionEntity transfer(Long fromWalletId, Long toWalletId,
                                      BigDecimal amount, String idempotencyKey) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (fromWalletId.equals(toWalletId)){
            throw new IllegalArgumentException("From wallet id must not be equal to To wallet id");
        }
        Optional<TransactionEntity> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        Long firstId = Math.min(fromWalletId, toWalletId);
        Long secondId = Math.max(fromWalletId, toWalletId);
        Wallet first = walletRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new WalletNotFoundException(firstId));
        Wallet second = walletRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new WalletNotFoundException(secondId));

        Wallet fromWallet = fromWalletId.equals(first.getId()) ? first : second;
        Wallet toWallet = toWalletId.equals(first.getId()) ? first : second;

        Optional<TransactionEntity> existingAfterLock = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingAfterLock.isPresent()) {
            return existingAfterLock.get();
        }

        if (!(fromWallet.getStatus() == WalletStatus.ACTIVE && toWallet.getStatus() == WalletStatus.ACTIVE)) {
            return saveFailed(amount, "WALLET_BLOCKED", idempotencyKey, fromWallet, toWallet);
        }
        if (fromWallet.getWalletType() != WalletType.SYSTEM && fromWallet.getBalance().compareTo(amount) < 0) {
            return saveFailed(amount, "INSUFFICIENT_FUNDS", idempotencyKey, fromWallet, toWallet);
        }
        TransactionEntity transaction = new TransactionEntity();
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setFromWallet(fromWallet);
        transaction.setToWallet(toWallet);
        fromWallet.setBalance(fromWallet.getBalance().subtract(amount));
        toWallet.setBalance(toWallet.getBalance().add(amount));
        TransactionEntity save = transactionRepository.save(transaction);
        LedgerEntry ledgerEntryMinus = new LedgerEntry(save, save.getFromWallet(), save.getAmount().negate());
        ledgerEntryRepository.save(ledgerEntryMinus);
        LedgerEntry ledgerEntryPlus = new LedgerEntry(save, save.getToWallet(), save.getAmount());
        ledgerEntryRepository.save(ledgerEntryPlus);
        return save;
    }



    private TransactionEntity saveFailed(BigDecimal amount, String reason, String idempotencyKey, Wallet fromWallet, Wallet toWallet) {
        TransactionEntity failed = new TransactionEntity();
        failed.setAmount(amount);
        failed.setStatus(TransactionStatus.FAILED);
        failed.setFailReason(reason);
        failed.setIdempotencyKey(idempotencyKey);
        failed.setFromWallet(fromWallet);
        failed.setToWallet(toWallet);
        return transactionRepository.save(failed);
    }
}
