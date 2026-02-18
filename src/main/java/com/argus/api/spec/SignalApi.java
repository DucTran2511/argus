package com.argus.api.spec;

import com.argus.api.dto.response.SignalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Signals", description = "Endpoints for whale and smart money signals")
public interface SignalApi {

    @Operation(summary = "Get latest signals")
    @GetMapping
    ResponseEntity<List<SignalResponse>> getSignals(
            @Parameter(description = "Include MEV bot signals (default: false)") @RequestParam(required = false, defaultValue = "false") boolean includeMev,
            @Parameter(description = "Limit the number of results") @RequestParam(required = false, defaultValue = "50") int limit);
}
