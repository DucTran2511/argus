package com.argus.infra.persistence.adapter;

import com.argus.domain.model.AddressLabel;
import com.argus.domain.port.persistence.AddressBookPersistencePort;
import com.argus.infra.persistence.entity.AddressLabelEntity;
import com.argus.infra.persistence.repository.AddressLabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AddressBookPersistenceAdapter implements AddressBookPersistencePort {

    private final AddressLabelRepository repository;

    @Override
    @Transactional
    public AddressLabel save(AddressLabel label) {
        AddressLabelEntity entity = AddressLabelEntity.fromDomain(label);
        return repository.save(entity).toDomain();
    }

    @Override
    @Transactional
    public List<AddressLabel> saveAll(List<AddressLabel> labels) {
        List<AddressLabelEntity> entities = labels.stream()
                .map(AddressLabelEntity::fromDomain)
                .collect(Collectors.toList());
        return repository.saveAll(entities).stream()
                .map(AddressLabelEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressLabel> findByAddressAndUserId(String address, UUID userId) {
        return repository.findByAddressIgnoreCaseAndUserId(address, userId).stream()
                .map(AddressLabelEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressLabel> findByAddressesAndUserId(Set<String> addresses, UUID userId) {
        if (addresses == null || addresses.isEmpty()) {
            return List.of();
        }
        return repository.findByAddressIgnoreCaseInAndUserId(addresses, userId).stream()
                .map(AddressLabelEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressLabel> findByLabelAndUserId(String label, UUID userId) {
        return repository.findByLabelIgnoreCaseAndUserId(label, userId).stream()
                .map(AddressLabelEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressLabel> findByLabelContainingAndUserId(String labelPart, UUID userId) {
        return repository.findByLabelContainingIgnoreCaseAndUserId(labelPart, userId).stream()
                .map(AddressLabelEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressLabel> findByCategoryAndUserId(String category, UUID userId) {
        return repository.findByCategoryAndUserId(category, userId).stream()
                .map(AddressLabelEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByAddressAndLabelAndUserId(String address, String label, UUID userId) {
        return repository.existsByAddressIgnoreCaseAndLabelIgnoreCaseAndUserId(address, label, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByAddressAndUserId(String address, UUID userId) {
        return repository.countByAddressIgnoreCaseAndUserId(address, userId);
    }

    @Override
    @Transactional
    public void deleteByAddressAndLabelAndUserId(String address, String label, UUID userId) {
        repository.deleteByAddressAndLabelAndUserId(address, label, userId);
    }
}
