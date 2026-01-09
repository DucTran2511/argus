package com.argus.api;

import com.argus.api.dto.TransactionResponse;
import com.argus.domain.model.TransactionWithSwap;
import com.argus.domain.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

        private final TransactionService transactionService;

        @GetMapping("/{txHash}")
        public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String txHash) {
                validateTxHash(txHash);

                TransactionWithSwap result = transactionService.getTransaction(txHash);

                return ResponseEntity.ok(TransactionResponse.from(result));
        }

        @PostMapping("/{txHash}")
        public ResponseEntity<TransactionResponse> saveTransaction(@PathVariable String txHash) {
                validateTxHash(txHash);

                TransactionWithSwap result = transactionService.saveTransaction(txHash);

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(TransactionResponse.from(result));
        }

        private void validateTxHash(String txHash) {
                if (txHash == null || !txHash.startsWith("0x") || txHash.length() != 66) {
                        throw new IllegalArgumentException(
                                        "Invalid transaction hash format. Must start with 0x and be 66 characters long.");
                }
        }
}
