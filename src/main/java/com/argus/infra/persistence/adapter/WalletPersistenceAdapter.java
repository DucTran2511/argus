package com.argus.infra.persistence.adapter;

import com.argus.domain.model.Wallet;
import com.argus.domain.port.persistence.WalletPersistencePort;
import com.argus.infra.persistence.entity.WalletEntity;
import com.argus.infra.persistence.repository.WalletRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletPersistenceAdapter implements WalletPersistencePort {

    private final WalletRepository repository;

    @Override
    @Transactional
    public Wallet save(Wallet wallet) {
        validateWallet(wallet);
        WalletEntity entity = toEntity(wallet);
        WalletEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Wallet> findById(UUID id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Optional<Wallet> findByAddress(String address) {
        return repository.findByAddress(address)
                .map(this::toDomain);
    }

    @Override
    public List<Wallet> findByUserId(UUID userId) {
        return repository.findByUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Wallet> findByIdAndUserId(UUID id, UUID userId) {
        return repository.findByIdAndUserId(id, userId)
                .map(this::toDomain);
    }

    @Override
    public Optional<Wallet> findByAddressAndUserId(String address, UUID userId) {
        return repository.findByAddressAndUserId(address, userId)
                .map(this::toDomain);
    }

    @Override
    public List<Wallet> findAll() {
        return repository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Wallet> findByChain(String chain) {
        return repository.findByChain(chain).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Wallet> findByType(Wallet.WalletType type) {
        WalletEntity.WalletType entityType = mapToEntityType(type);
        return repository.findByType(entityType).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Wallet> findByChainAndType(String chain, Wallet.WalletType type) {
        WalletEntity.WalletType entityType = mapToEntityType(type);
        return repository.findByChainAndType(chain, entityType).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Wallet> findTopPerformers(double minWinRate) {
        return repository.findTopPerformers(minWinRate).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByAddress(String address) {
        return repository.existsByAddress(address);
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public void deleteByAddress(String address) {
        repository.findByAddress(address)
                .ifPresent(repository::delete);
    }

    @Override
    public Set<String> getAllAddresses() {
        return repository.findAllAddresses();
    }

    private void validateWallet(Wallet wallet) {
        if (wallet == null) {
            throw new IllegalArgumentException("Wallet cannot be null");
        }
    }

    private Wallet toDomain(WalletEntity entity) {
        if (entity == null) {
            return null;
        }

        return Wallet.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .address(entity.getAddress())
                .chain(entity.getChain())
                .label(entity.getLabel())
                .type(mapToDomainType(entity.getType()))
                .totalPnl(entity.getTotalPnl())
                .winRate(entity.getWinRate())
                .firstSeenAt(entity.getFirstSeenAt())
                .lastActivityAt(entity.getLastActivityAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private WalletEntity toEntity(Wallet domain) {
        if (domain == null) {
            return null;
        }

        WalletEntity entity = new WalletEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setAddress(domain.getAddress());
        entity.setChain(domain.getChain());
        entity.setLabel(domain.getLabel());
        entity.setType(mapToEntityType(domain.getType()));
        entity.setTotalPnl(domain.getTotalPnl());
        entity.setWinRate(domain.getWinRate());
        entity.setFirstSeenAt(domain.getFirstSeenAt());
        entity.setLastActivityAt(domain.getLastActivityAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        return entity;
    }

    private Wallet.WalletType mapToDomainType(WalletEntity.WalletType entityType) {
        if (entityType == null) {
            return Wallet.WalletType.UNKNOWN;
        }

        try {
            return Wallet.WalletType.valueOf(entityType.name());
        } catch (IllegalArgumentException e) {
            return Wallet.WalletType.UNKNOWN;
        }
    }

    private WalletEntity.WalletType mapToEntityType(Wallet.WalletType domainType) {
        if (domainType == null) {
            return null;
        }

        try {
            return WalletEntity.WalletType.valueOf(domainType.name());
        } catch (IllegalArgumentException e) {
            return WalletEntity.WalletType.UNKNOWN;
        }
    }
}
