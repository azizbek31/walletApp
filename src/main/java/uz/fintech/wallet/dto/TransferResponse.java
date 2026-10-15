package uz.fintech.wallet.dto;

import lombok.Builder;
import uz.fintech.wallet.enums.TransactionStatus;

import java.math.BigDecimal;

@Builder
public record TransferResponse(
        Long transactionId,
        TransactionStatus status,
        String failReason,
        BigDecimal amount
) {}
