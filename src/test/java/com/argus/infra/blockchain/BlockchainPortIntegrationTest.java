package com.argus.infra.blockchain;

import com.argus.domain.port.blockchain.BlockChainPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("BlockChainPort Spring DI Integration Tests")
class BlockchainPortIntegrationTest {

    @Autowired
    private BlockChainPort blockChainPort;

    @Autowired
    private Web3jBlockchainAdapter web3jBlockchainAdapter;

    @Test
    @DisplayName("should inject BlockChainPort via Spring DI")
    void shouldInjectBlockChainPort() {

        assertThat(blockChainPort).isNotNull();
        assertThat(blockChainPort).isInstanceOf(Web3jBlockchainAdapter.class);
    }

    @Test
    @DisplayName("should inject Web3jBlockchainAdapter as implementation")
    void shouldInjectWeb3jBlockchainAdapter() {

        assertThat(web3jBlockchainAdapter).isNotNull();
    }

    @Test
    @DisplayName("BlockChainPort and Web3jBlockchainAdapter should be the same bean")
    void portAndAdapterShouldBeSameBean() {
        assertThat(blockChainPort).isSameAs(web3jBlockchainAdapter);
    }
}
