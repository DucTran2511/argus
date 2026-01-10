package com.argus.api.spec;

import com.argus.api.dto.ErrorResponse;
import com.argus.api.dto.response.PriceResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Prices", description = "Price related operations")
public interface PriceApi {
    @Operation(summary = "Get price by token address")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Price found", content = @Content(schema = @Schema(implementation = PriceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Price not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<PriceResponse> getPrice(@PathVariable String tokenAddress);
}
