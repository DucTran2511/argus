package com.argus.api.spec;

import com.argus.api.dto.response.SmartMoneyResponse;
import com.argus.domain.model.SmartMoneyArchetype;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Smart Money", description = "Endpoints for exploring high-performing wallets and archetypes")
public interface SmartMoneyApi {

    @Operation(summary = "List all scored wallets", description = "Retrieves a paginated list of wallets with their smart money metrics and archetypes")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list")
    @GetMapping
    ResponseEntity<Page<SmartMoneyResponse>> getAllMetrics(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Filter by wallet archetype") @RequestParam(required = false) SmartMoneyArchetype archetype);

    @Operation(summary = "Get wallet metrics by address", description = "Retrieves archetypes and scores for a specific wallet address")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved metrics")
    @ApiResponse(responseCode = "404", description = "Wallet metrics not found", content = @Content)
    @GetMapping("/{address}")
    ResponseEntity<SmartMoneyResponse> getWalletMetrics(
            @Parameter(description = "The Ethereum address to lookup") @PathVariable @Pattern(regexp = "^0x[a-fA-F0-9]{40}$") String address);

    @Operation(summary = "Get top performing wallets", description = "Returns the highest scoring wallets based on a specific dimension")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved top wallets")
    @GetMapping("/top")
    ResponseEntity<List<SmartMoneyResponse>> getTopWallets(
            @Parameter(description = "Filter type (total, pnl, consistency, conviction)") @RequestParam(defaultValue = "total") String sortBy,
            @Parameter(description = "Number of wallets to return") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit);

    @Operation(summary = "Refresh wallet metrics", description = "Triggers a real-time recalculation of metrics for the given wallet")
    @ApiResponse(responseCode = "200", description = "Successfully refreshed metrics")
    @ApiResponse(responseCode = "404", description = "Wallet or transaction history not found", content = @Content)
    @PostMapping("/{address}/refresh")
    ResponseEntity<SmartMoneyResponse> refreshMetrics(
            @Parameter(description = "The Ethereum address to refresh") @PathVariable @Pattern(regexp = "^0x[a-fA-F0-9]{40}$") String address);
}
