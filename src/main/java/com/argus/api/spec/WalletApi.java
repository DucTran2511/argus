package com.argus.api.spec;

import com.argus.api.dto.ErrorResponse;
import com.argus.api.dto.WalletRequest;
import com.argus.api.dto.WalletResponse;
import com.argus.api.dto.response.SyncResponse;
import com.argus.api.dto.response.WalletTimelineResponse;
import com.argus.domain.model.Wallet;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Tag(name = "Wallets", description = "Wallet tracking and management operations")
public interface WalletApi {

        @Operation(summary = "Create a new wallet", description = "Add a blockchain wallet address to track. Automatically syncs transaction history in background.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Wallet created successfully", content = @Content(schema = @Schema(implementation = WalletResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid wallet address format", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "409", description = "Wallet already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        ResponseEntity<WalletResponse> createWallet(
                        @Valid @RequestBody WalletRequest request);

        @Operation(summary = "Get wallet by ID", description = "Retrieve wallet details by its UUID")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Wallet found", content = @Content(schema = @Schema(implementation = WalletResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Wallet not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        ResponseEntity<WalletResponse> getWalletById(
                        @Parameter(description = "Wallet UUID", required = true) @PathVariable UUID id);

        @Operation(summary = "Get wallet by address", description = "Retrieve wallet details by blockchain address")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Wallet found", content = @Content(schema = @Schema(implementation = WalletResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Wallet not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        ResponseEntity<WalletResponse> getWalletByAddress(
                        @Parameter(description = "Ethereum address (0x...)", example = "0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045") @PathVariable String address);

        @Operation(summary = "List all wallets", description = "Get all tracked wallets, optionally filtered by type")
        @ApiResponse(responseCode = "200", description = "List of wallets")
        ResponseEntity<List<WalletResponse>> getAllWallets(
                        @Parameter(description = "Filter by wallet type", example = "WHALE") @RequestParam(required = false) Wallet.WalletType type);

        @Operation(summary = "Update wallet", description = "Update wallet metadata (label, type, PnL, win rate). Address and chain cannot be modified.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Wallet updated", content = @Content(schema = @Schema(implementation = WalletResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Wallet not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        ResponseEntity<WalletResponse> updateWallet(
                        @Parameter(description = "Wallet UUID", required = true) @PathVariable UUID id,
                        @Valid @RequestBody WalletRequest request);

        @Operation(summary = "Delete wallet", description = "Remove wallet from tracking")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Wallet deleted"),
                        @ApiResponse(responseCode = "404", description = "Wallet not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        ResponseEntity<Void> deleteWallet(
                        @Parameter(description = "Wallet UUID", required = true) @PathVariable UUID id);

        @Operation(summary = "Check if wallet exists", description = "Check if a wallet address is being tracked")
        @ApiResponse(responseCode = "200", description = "Returns true/false")
        ResponseEntity<Boolean> walletExists(
                        @Parameter(description = "Ethereum address", example = "0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045") @PathVariable String address);

        @Operation(summary = "Get wallet transaction timeline", description = "Retrieve transaction history for a wallet with pagination")
        @ApiResponse(responseCode = "200", description = "Transaction timeline")
        ResponseEntity<WalletTimelineResponse> getWalletTimeline(
                        @Parameter(description = "Ethereum address", required = true) @PathVariable @Pattern(regexp = "^0x[a-fA-F0-9]{40}$") String address,

                        @Parameter(description = "Number of transactions to return (1-500)", example = "50") @RequestParam(defaultValue = "50") @Min(1) @Max(500) int limit,

                        @Parameter(description = "Sort order: asc or desc", example = "desc") @RequestParam(defaultValue = "desc") @Pattern(regexp = "^(asc|desc)$") String order);

        @Operation(summary = "Sync wallet transaction history", description = "Fetch and store transaction history from blockchain (Alchemy API)")
        @ApiResponse(responseCode = "200", description = "Sync completed")
        ResponseEntity<SyncResponse> syncWalletHistory(
                        @Parameter(description = "Ethereum address", required = true) @PathVariable @Pattern(regexp = "^0x[a-fA-F0-9]{40}$") String address,

                        @Parameter(description = "Maximum transactions to sync", example = "500") @RequestParam(defaultValue = "500") int maxCount);
}
