package com.argus.infra.blockchain;

import com.argus.core.exception.BlockchainException;
import com.argus.domain.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthTransaction;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Web3jBlockchainAdapter Unit Tests")
class Web3jBlockchainAdapterTest {

    @Mock
    private Web3j web3j;

    private Web3jBlockchainAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new Web3jBlockchainAdapter(
                "https://test-rpc.example.com/v2/test-key",
                5,
                2);
        injectWeb3j(adapter, web3j);
    }

    private void injectWeb3j(Web3jBlockchainAdapter adapter, Web3j web3j) {
        try {
            var field = Web3jBlockchainAdapter.class.getDeclaredField("web3j");
            field.setAccessible(true);
            field.set(adapter, web3j);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject Web3j mock", e);
        }
    }

    @Nested
    @DisplayName("getLatestBlockNumber Tests")
    class GetLatestBlockNumberTests {

        @Test
        @DisplayName("should return block number on successful RPC call")
        void shouldReturnBlockNumber_WhenRpcSucceeds() throws IOException {
            BigInteger expectedBlockNumber = BigInteger.valueOf(18500000L);
            EthBlockNumber ethBlockNumber = mock(EthBlockNumber.class);
            Request request = mock(Request.class);

            doReturn(request).when(web3j).ethBlockNumber();
            doReturn(ethBlockNumber).when(request).send();
            when(ethBlockNumber.hasError()).thenReturn(false);
            when(ethBlockNumber.getBlockNumber()).thenReturn(expectedBlockNumber);

            long result = adapter.getLatestBlockNumber();

            assertThat(result).isEqualTo(18500000L);
            verify(web3j).ethBlockNumber();
        }

        @Test
        @DisplayName("should retry on transient failure and succeed")
        void shouldRetry_WhenFirstAttemptFails() throws IOException {
            BigInteger expectedBlockNumber = BigInteger.valueOf(18500000L);
            EthBlockNumber ethBlockNumber = mock(EthBlockNumber.class);
            Request request = mock(Request.class);

            doReturn(request).when(web3j).ethBlockNumber();
            when(request.send())
                    .thenThrow(new IOException("Connection timeout"))
                    .thenReturn(ethBlockNumber);
            when(ethBlockNumber.hasError()).thenReturn(false);
            when(ethBlockNumber.getBlockNumber()).thenReturn(expectedBlockNumber);

            long result = adapter.getLatestBlockNumber();

            assertThat(result).isEqualTo(18500000L);
            verify(request, times(2)).send();
        }

        @Test
        @DisplayName("should throw BlockchainException after exhausting retries")
        void shouldThrowException_WhenAllRetriesFail() throws IOException {
            Request request = mock(Request.class);

            doReturn(request).when(web3j).ethBlockNumber();
            when(request.send())
                    .thenThrow(new IOException("Persistent network failure"));

            assertThatThrownBy(() -> adapter.getLatestBlockNumber())
                    .isInstanceOf(BlockchainException.class)
                    .hasMessageContaining("Failed to get latest block number after 2 attempts");

            verify(request, times(2)).send();
        }

        @Test
        @DisplayName("should throw BlockchainException on RPC error response")
        void shouldThrowException_WhenRpcReturnsError() throws IOException {
            EthBlockNumber ethBlockNumber = mock(EthBlockNumber.class);
            Request request = mock(Request.class);
            org.web3j.protocol.core.Response.Error rpcError = new org.web3j.protocol.core.Response.Error(429,
                    "Rate limit exceeded");

            doReturn(request).when(web3j).ethBlockNumber();
            doReturn(ethBlockNumber).when(request).send();
            when(ethBlockNumber.hasError()).thenReturn(true);
            when(ethBlockNumber.getError()).thenReturn(rpcError);

            assertThatThrownBy(() -> adapter.getLatestBlockNumber())
                    .isInstanceOf(BlockchainException.class)
                    .hasMessageContaining("Failed to get latest block number after 2 attempts")
                    .hasRootCauseMessage("RPC error: Rate limit exceeded");
        }
    }

    @Nested
    @DisplayName("getTransactionByHash Tests")
    class GetTransactionByHashTests {

        private static final String VALID_TX_HASH = "0x123abc456def789";

        @Test
        @DisplayName("should return transaction when found")
        void shouldReturnTransaction_WhenFound() throws IOException {
            EthTransaction ethTransaction = mock(EthTransaction.class);
            org.web3j.protocol.core.methods.response.Transaction web3jTx = mock(
                    org.web3j.protocol.core.methods.response.Transaction.class);
            Request request = mock(Request.class);

            doReturn(request).when(web3j).ethGetTransactionByHash(VALID_TX_HASH);
            doReturn(ethTransaction).when(request).send();
            when(ethTransaction.hasError()).thenReturn(false);
            when(ethTransaction.getTransaction()).thenReturn(Optional.of(web3jTx));
            when(web3jTx.getHash()).thenReturn(VALID_TX_HASH);
            when(web3jTx.getBlockNumber()).thenReturn(BigInteger.valueOf(18500000L));
            when(web3jTx.getGas()).thenReturn(BigInteger.valueOf(21000L));
            when(web3jTx.getGasPrice()).thenReturn(BigInteger.valueOf(50_000_000_000L));

            Optional<Transaction> result = adapter.getTransactionByHash(VALID_TX_HASH);

            assertThat(result).isPresent();
            assertThat(result.get().getTxHash()).isEqualTo(VALID_TX_HASH);
            assertThat(result.get().getBlockNumber()).isEqualTo(18500000L);
            assertThat(result.get().getChain()).isEqualTo("ethereum");
        }

        @Test
        @DisplayName("should return empty when transaction not found")
        void shouldReturnEmpty_WhenTransactionNotFound() throws IOException {
            EthTransaction ethTransaction = mock(EthTransaction.class);
            Request request = mock(Request.class);

            doReturn(request).when(web3j).ethGetTransactionByHash(VALID_TX_HASH);
            doReturn(ethTransaction).when(request).send();
            when(ethTransaction.hasError()).thenReturn(false);
            when(ethTransaction.getTransaction()).thenReturn(Optional.empty());

            Optional<Transaction> result = adapter.getTransactionByHash(VALID_TX_HASH);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for null hash")
        void shouldThrowException_WhenHashIsNull() {

            assertThatThrownBy(() -> adapter.getTransactionByHash(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null or empty");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for empty hash")
        void shouldThrowException_WhenHashIsEmpty() {

            assertThatThrownBy(() -> adapter.getTransactionByHash("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null or empty");
        }

        @Test
        @DisplayName("should retry on transient failure")
        void shouldRetry_WhenNetworkFails() throws IOException {

            EthTransaction ethTransaction = mock(EthTransaction.class);
            org.web3j.protocol.core.methods.response.Transaction web3jTx = mock(
                    org.web3j.protocol.core.methods.response.Transaction.class);
            Request request = mock(Request.class);

            doReturn(request).when(web3j).ethGetTransactionByHash(VALID_TX_HASH);
            when(request.send())
                    .thenThrow(new IOException("Network timeout"))
                    .thenReturn(ethTransaction);
            when(ethTransaction.hasError()).thenReturn(false);
            when(ethTransaction.getTransaction()).thenReturn(Optional.of(web3jTx));
            when(web3jTx.getHash()).thenReturn(VALID_TX_HASH);
            when(web3jTx.getBlockNumber()).thenReturn(BigInteger.valueOf(18500000L));
            when(web3jTx.getGas()).thenReturn(null);
            when(web3jTx.getGasPrice()).thenReturn(null);

            Optional<Transaction> result = adapter.getTransactionByHash(VALID_TX_HASH);

            assertThat(result).isPresent();
            verify(request, times(2)).send();
        }

        @Test
        @DisplayName("should throw BlockchainException after retry exhaustion")
        void shouldThrowException_WhenAllRetriesFail() throws IOException {
            Request request = mock(Request.class);

            doReturn(request).when(web3j).ethGetTransactionByHash(VALID_TX_HASH);
            when(request.send())
                    .thenThrow(new IOException("Persistent failure"));

            assertThatThrownBy(() -> adapter.getTransactionByHash(VALID_TX_HASH))
                    .isInstanceOf(BlockchainException.class)
                    .hasMessageContaining("Failed to get transaction");

            verify(request, times(2)).send();
        }
    }
}
