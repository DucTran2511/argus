package com.argus.config;

import com.argus.domain.model.TokenPrice;
import com.argus.domain.port.blockchain.BlockChainPort;
import com.argus.domain.port.blockchain.DexDecoderPort;
import com.argus.domain.port.blockchain.PricePort;
import com.argus.domain.port.cache.CachePort;
import com.argus.domain.port.persistence.TransactionPersistencePort;
import com.argus.domain.port.persistence.WalletPersistencePort;
import com.argus.domain.service.PriceService;
import com.argus.domain.service.TransactionService;
import com.argus.domain.service.WalletService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public WalletService walletService(WalletPersistencePort walletPersistencePort) {
        return new WalletService(walletPersistencePort);
    }

    @Bean
    public TransactionService transactionService(
            BlockChainPort blockChainPort,
            TransactionPersistencePort transactionPersistencePort,
            DexDecoderPort dexDecoder) {
        return new TransactionService(blockChainPort, transactionPersistencePort, dexDecoder);
    }

    @Bean
    public PriceService priceService(
            PricePort pricePort,
            CachePort<String, TokenPrice> priceCache) {
        return new PriceService(pricePort, priceCache);
    }
}
