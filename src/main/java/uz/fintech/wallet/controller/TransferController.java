package uz.fintech.wallet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.fintech.wallet.dto.TransferRequest;
import uz.fintech.wallet.dto.TransferResponse;
import uz.fintech.wallet.entity.TransactionEntity;
import uz.fintech.wallet.enums.TransactionStatus;
import uz.fintech.wallet.service.TransferService;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        TransactionEntity transfer = transferService.transfer(request.fromWalletId(), request.toWalletId(), request.amount(), request.idempotencyKey());
        TransferResponse build = TransferResponse.builder()
                .transactionId(transfer.getId())
                .amount(transfer.getAmount())
                .status(transfer.getStatus())
                .failReason(transfer.getFailReason())
                .build();
        if (transfer.getStatus() == TransactionStatus.FAILED) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(build);
        }
        return ResponseEntity.ok(build);
    }
}
