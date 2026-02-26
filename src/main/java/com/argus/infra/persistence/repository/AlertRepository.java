package com.argus.infra.persistence.repository;

import com.argus.infra.persistence.entity.AlertEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<AlertEntity, UUID> {

    List<AlertEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE AlertEntity a SET a.isRead = true WHERE a.id = :id AND a.userId = :userId")
    int markAsRead(@Param("id") UUID id, @Param("userId") UUID userId);
}
