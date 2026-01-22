package com.argus.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.argus.infra.persistence.entity.SignalEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SignalRepository extends JpaRepository<SignalEntity, Long> {
        boolean existsByTxHashAndType(String txHash, String type);

        @Query("SELECT COUNT(DISTINCT s.walletId) FROM SignalEntity s " +
                        "WHERE s.tokenAddress = :tokenAddress " +
                        "AND s.type = :type " +
                        "AND s.createdAt > :since " +
                        "AND s.walletId IS NOT NULL")
        long countDistinctWalletsByTokenAndTypeAfter(
                        @Param("tokenAddress") String tokenAddress,
                        @Param("type") String type,
                        @Param("since") LocalDateTime since);

        @Query("SELECT DISTINCT s.walletId FROM SignalEntity s " +
                        "WHERE s.tokenAddress = :tokenAddress " +
                        "AND s.type = :type " +
                        "AND s.createdAt > :since " +
                        "AND s.walletId IS NOT NULL")
        List<UUID> findDistinctWalletIdsByTokenAndTypeAfter(
                        @Param("tokenAddress") String tokenAddress,
                        @Param("type") String type,
                        @Param("since") LocalDateTime since);

        boolean existsByTokenAddressAndTypeAndCreatedAtAfter(
                        String tokenAddress, String type, LocalDateTime since);

        @Query("SELECT COUNT(s) FROM SignalEntity s " +
                        "WHERE s.walletId = :walletId " +
                        "AND s.tokenAddress = :tokenAddress " +
                        "AND s.type = :type " +
                        "AND s.createdAt > :since")
        long countByWalletAndTokenAndTypeAfter(
                        @Param("walletId") UUID walletId,
                        @Param("tokenAddress") String tokenAddress,
                        @Param("type") String type,
                        @Param("since") LocalDateTime since);

        @Query("SELECT COALESCE(SUM(s.usdValue), 0) FROM SignalEntity s " +
                        "WHERE s.walletId = :walletId " +
                        "AND s.tokenAddress = :tokenAddress " +
                        "AND s.type = :type " +
                        "AND s.createdAt > :since")
        BigDecimal sumUsdValueByWalletAndTokenAndTypeAfter(
                        @Param("walletId") UUID walletId,
                        @Param("tokenAddress") String tokenAddress,
                        @Param("type") String type,
                        @Param("since") LocalDateTime since);

        boolean existsByWalletIdAndTokenAddressAndTypeAndCreatedAtAfter(
                        UUID walletId, String tokenAddress, String type, LocalDateTime since);

}
