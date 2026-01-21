package com.argus.api.spec;

import com.argus.api.dto.ErrorResponse;
import com.argus.api.dto.LabelRequest;
import com.argus.api.dto.response.LabelResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Address Book", description = "Label any Ethereum address (tracked or not)")
public interface AddressBookApi {

    @Operation(summary = "Add label to address", description = "Add a label to any Ethereum address (max 5 per address)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Label added successfully", content = @Content(schema = @Schema(implementation = LabelResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or max labels exceeded", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Label already exists for address", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<?> addLabel(@Valid @RequestBody LabelRequest request);

    @Operation(summary = "Get labels for address", description = "Retrieve all labels for a specific Ethereum address")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Labels retrieved successfully", content = @Content(schema = @Schema(implementation = LabelResponse.class))),
            @ApiResponse(responseCode = "404", description = "No labels found for address", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<LabelResponse> getLabels(
            @Parameter(description = "Ethereum address (0x...)", example = "0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045") @PathVariable String address);

    @Operation(summary = "Remove label from address", description = "Delete a specific label from an address")
    @ApiResponse(responseCode = "204", description = "Label removed successfully")
    ResponseEntity<Void> removeLabel(
            @Parameter(description = "Ethereum address (0x...)") @PathVariable String address,
            @Parameter(description = "Label to remove") @PathVariable String label);

    @Operation(summary = "Search/filter labels", description = "Search by label or filter by category")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results"),
            @ApiResponse(responseCode = "400", description = "Must provide label, q, or category parameter")
    })
    ResponseEntity<List<LabelResponse>> searchLabels(
            @Parameter(description = "Exact label match") @RequestParam(required = false) String label,
            @Parameter(description = "Partial label match (contains)") @RequestParam(required = false) String q,
            @Parameter(description = "Filter by category") @RequestParam(required = false) String category);

    @Operation(summary = "Bulk import labels", description = "Import labels from JSON array")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Import completed"),
            @ApiResponse(responseCode = "400", description = "Invalid import data", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<?> importLabels(@RequestBody List<LabelRequest> requests);
}
