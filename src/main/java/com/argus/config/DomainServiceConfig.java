package com.argus.config;

import com.argus.domain.model.TokenPrice;
import com.argus.domain.port.blockchain.BlockChainPort;
import com.argus.domain.port.blockchain.DexDecoderPort;
import com.argus.domain.port.blockchain.PricePort;
import com.argus.domain.port.persistence.SignalPersistencePort;
import com.argus.domain.port.cache.CachePort;
import com.argus.domain.port.cache.BlockTrackingPort;
import com.argus.domain.port.persistence.TransactionPersistencePort;
import com.argus.domain.port.persistence.WalletPersistencePort;
import com.argus.domain.port.persistence.WalletStatsPersistencePort;
import com.argus.domain.port.persistence.AddressBookPersistencePort;
import com.argus.domain.port.persistence.WalletMetricsPersistencePort;

import com.argus.domain.service.PriceService;
import com.argus.domain.service.SmartMoneySignalEnricher;
import com.argus.domain.service.WhaleDetectorService;
import com.argus.domain.service.TransactionService;
import com.argus.domain.service.HistoricalImportService;
import com.argus.domain.service.WalletService;
import com.argus.domain.service.WalletStatsAggregator;
import com.argus.domain.service.WalletStatsService;
import com.argus.domain.service.SmartMoneyScoringService;

import com.argus.infra.stream.StreamPublisher;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public WalletService walletService(WalletPersistencePort walletPersistencePort,
            TransactionService transactionService,
            BlockTrackingPort blockTrackingPort) {
        return new WalletService(walletPersistencePort, transactionService, blockTrackingPort);
    }

    @Bean
    public TransactionService transactionService(
            BlockChainPort blockChainPort,
            TransactionPersistencePort transactionPersistencePort,
            DexDecoderPort dexDecoder,
            PriceService priceService,
            StreamPublisher streamPublisher) {
        return new TransactionService(blockChainPort, transactionPersistencePort, dexDecoder, priceService,
                streamPublisher);
    }

    @Bean
    public PriceService priceService(
            PricePort pricePort,
            CachePort<String, TokenPrice> priceCache) {
        return new PriceService(pricePort, priceCache);
    }

    @Bean
    public SmartMoneySignalEnricher smartMoneySignalEnricher(
            WalletMetricsPersistencePort walletMetricsPersistencePort,
            SignalPersistencePort signalPersistencePort) {
        return new SmartMoneySignalEnricher(walletMetricsPersistencePort, signalPersistencePort);
    }

    @Bean
    public WhaleDetectorService whaleDetectorService(
            SignalPersistencePort signalPersistencePort,
            SmartMoneySignalEnricher smartMoneySignalEnricher,
            WalletMetricsPersistencePort walletMetricsPersistencePort) {
        return new WhaleDetectorService(signalPersistencePort, smartMoneySignalEnricher, walletMetricsPersistencePort);
    }

    @Bean
    public HistoricalImportService historicalImportService(
            BlockChainPort blockChainPort,
            PricePort pricePort,
            TransactionPersistencePort transactionPort,
            PriceService priceService) {
        return new HistoricalImportService(blockChainPort, pricePort, transactionPort, priceService);
    }

    @Bean
    public WalletStatsService walletStatsService(TransactionPersistencePort transactionPersistencePort,
            WalletStatsPersistencePort walletStatsPersistencePort) {
        return new WalletStatsService(transactionPersistencePort, walletStatsPersistencePort);
    }

    @Bean
    public com.argus.domain.service.AddressBookService addressBookService(
            AddressBookPersistencePort addressBookPersistencePort,
            Clock clock) {
        return new com.argus.domain.service.AddressBookService(addressBookPersistencePort, clock);
    }

    @Bean
    public SmartMoneyScoringService smartMoneyScoringService(
            WalletStatsPersistencePort walletStatsPort,
            TransactionPersistencePort transactionPort,
            WalletMetricsPersistencePort walletMetricsPort,
            WalletStatsAggregator walletStatsAggregator,
            Clock clock) {
        return new SmartMoneyScoringService(walletStatsPort, transactionPort,
                walletMetricsPort, walletStatsAggregator, clock);
    }

    @Bean
    public WalletStatsAggregator walletStatsAggregator(
            WalletStatsPersistencePort walletStatsPort) {
        return new WalletStatsAggregator(walletStatsPort);
    }
}
