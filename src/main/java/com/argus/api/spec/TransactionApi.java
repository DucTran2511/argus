package com.argus.api.spec;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import com.argus.api.dto.ErrorResponse;
import com.argus.api.dto.TransactionResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Transactions", description = "Transaction related operations")
public interface TransactionApi {
    @Operation(summary = "Get transaction by hash")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction found", content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<TransactionResponse> getTransaction(@PathVariable String txHash);

    @Operation(summary = "Save transaction by hash")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transaction saved", content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<TransactionResponse> saveTransaction(@PathVariable String txHash);
}
