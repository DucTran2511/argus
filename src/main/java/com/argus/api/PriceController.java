package com.argus.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.argus.api.dto.response.PriceResponse;
import com.argus.api.spec.PriceApi;
import com.argus.domain.model.TokenPrice;
import com.argus.domain.service.PriceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/prices")
@RequiredArgsConstructor
public class PriceController implements PriceApi {

    private final PriceService priceService;

    @Override
    @GetMapping("/{tokenAddress}")
    public ResponseEntity<PriceResponse> getPrice(@PathVariable String tokenAddress) {
        log.info("GET /api/v1/prices/{}", tokenAddress);
        TokenPrice price = priceService.getPrice(tokenAddress);
        return ResponseEntity.ok(PriceResponse.from(price));
    }
}
