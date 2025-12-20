package com.argus.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.argus.infra.persistence.entity.TransactionEntity;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

}
