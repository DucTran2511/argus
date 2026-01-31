package com.argus.core.exception;

import com.argus.api.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
        @ExceptionHandler(TransactionNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleTransactionNotFound(
                        TransactionNotFoundException ex,
                        WebRequest request) {

                log.warn("Transaction not found: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.of(
                                ex.getHttpStatus().value(),
                                ex.getHttpStatus().getReasonPhrase(),
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""),
                                ex.getErrorCode());

                return new ResponseEntity<>(error, ex.getHttpStatus());
        }

        @ExceptionHandler(BlockchainException.class)
        public ResponseEntity<ErrorResponse> handleBlockchainException(
                        BlockchainException ex,
                        WebRequest request) {

                log.error("Blockchain error: {}", ex.getMessage(), ex);

                ErrorResponse error = ErrorResponse.of(
                                ex.getHttpStatus().value(),
                                ex.getHttpStatus().getReasonPhrase(),
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""),
                                ex.getErrorCode());

                return new ResponseEntity<>(error, ex.getHttpStatus());
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(
                        IllegalArgumentException ex,
                        WebRequest request) {

                log.warn("Validation error: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.of(
                                HttpStatus.BAD_REQUEST.value(),
                                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""),
                                "VALIDATION_ERROR");

                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGlobalException(
                        Exception ex,
                        WebRequest request) {

                log.error("Unexpected error: {}", ex.getMessage(), ex);

                ErrorResponse error = ErrorResponse.of(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                                "An unexpected error occurred: " + ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""),
                                "INTERNAL_ERROR");

                return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @ExceptionHandler(LabelNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleLabelNotFound(
                        LabelNotFoundException ex,
                        WebRequest request) {

                log.warn("Label not found: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.of(
                                ex.getHttpStatus().value(),
                                ex.getHttpStatus().getReasonPhrase(),
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""),
                                ex.getErrorCode());

                return new ResponseEntity<>(error, ex.getHttpStatus());
        }

        @ExceptionHandler(MaxLabelsExceededException.class)
        public ResponseEntity<ErrorResponse> handleMaxLabelsExceeded(
                        MaxLabelsExceededException ex,
                        WebRequest request) {

                log.warn("Max labels exceeded: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.of(
                                ex.getHttpStatus().value(),
                                ex.getHttpStatus().getReasonPhrase(),
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""),
                                ex.getErrorCode());

                return new ResponseEntity<>(error, ex.getHttpStatus());
        }

        @ExceptionHandler(LabelAlreadyExistsException.class)
        public ResponseEntity<ErrorResponse> handleLabelAlreadyExists(
                        LabelAlreadyExistsException ex,
                        WebRequest request) {

                log.warn("Label already exists: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.of(
                                ex.getHttpStatus().value(),
                                ex.getHttpStatus().getReasonPhrase(),
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""),
                                ex.getErrorCode());

                return new ResponseEntity<>(error, ex.getHttpStatus());
        }

        @ExceptionHandler(WalletNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleWalletNotFound(
                        WalletNotFoundException ex,
                        WebRequest request) {

                log.warn("Wallet not found: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.of(
                                ex.getHttpStatus().value(),
                                ex.getHttpStatus().getReasonPhrase(),
                                ex.getMessage(),
                                request.getDescription(false).replace("uri=", ""),
                                ex.getErrorCode());

                return new ResponseEntity<>(error, ex.getHttpStatus());
        }
}
