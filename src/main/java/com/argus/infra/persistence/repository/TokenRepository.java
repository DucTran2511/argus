package com.argus.infra.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.argus.infra.persistence.entity.TokenEntity;

public interface TokenRepository extends JpaRepository<TokenEntity, UUID> {

}
