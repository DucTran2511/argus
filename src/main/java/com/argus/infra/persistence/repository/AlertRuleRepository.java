package com.argus.infra.persistence.repository;

import com.argus.infra.persistence.entity.AlertRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRuleRepository extends JpaRepository<AlertRuleEntity, UUID> {

    Optional<AlertRuleEntity> findByIdAndUserId(UUID id, UUID userId);

    List<AlertRuleEntity> findByUserId(UUID userId);

    List<AlertRuleEntity> findByEnabledTrue();

    void deleteByIdAndUserId(UUID id, UUID userId);
}
