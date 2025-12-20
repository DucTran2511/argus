package com.argus.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.argus.infra.persistence.entity.SignalEntity;

public interface SignalRepository extends JpaRepository<SignalEntity, Long> {

}
