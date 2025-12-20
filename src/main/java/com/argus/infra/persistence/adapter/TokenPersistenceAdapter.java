package com.argus.infra.persistence.adapter;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.argus.domain.model.Token;
import com.argus.domain.port.persistence.TokenPersistencePort;
import com.argus.infra.persistence.entity.TokenEntity;
import com.argus.infra.persistence.repository.TokenRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenPersistenceAdapter implements TokenPersistencePort {

    private final TokenRepository tokenRepository;

    @Override
    @Transactional
    public Token save(Token token) {
        validateToken(token);
        TokenEntity tokenEntity = toEntity(token);
        TokenEntity saved = tokenRepository.save(tokenEntity);
        return toDomain(saved);
    }

    private void validateToken(Token token) {
        if (token == null) {
            throw new IllegalArgumentException("Token cannot be null");
        }
    }

    private Token toDomain(TokenEntity tokenEntity) {
        return Token.builder()
                .id(tokenEntity.getId())
                .chain(tokenEntity.getChain())
                .symbol(tokenEntity.getSymbol())
                .name(tokenEntity.getName())
                .decimals(tokenEntity.getDecimals())
                .marketCap(tokenEntity.getMarketCap())
                .liquidity(tokenEntity.getLiquidity())
                .riskScore(tokenEntity.getRiskScore())
                .createdAt(tokenEntity.getCreatedAt())
                .updatedAt(tokenEntity.getUpdatedAt())
                .build();
    }

    private TokenEntity toEntity(Token token) {
        return TokenEntity.builder()
                .id(token.getId())
                .chain(token.getChain())
                .symbol(token.getSymbol())
                .name(token.getName())
                .decimals(token.getDecimals())
                .marketCap(token.getMarketCap())
                .liquidity(token.getLiquidity())
                .riskScore(token.getRiskScore())
                .createdAt(token.getId() != null ? token.getCreatedAt() : null)
                .updatedAt(token.getId() != null ? token.getUpdatedAt() : null)
                .build();
    }
}