package com.argus.infra.persistence.adapter;

import com.argus.domain.model.AddressLabel;
import com.argus.domain.port.persistence.AddressBookPersistencePort;
import com.argus.infra.persistence.entity.AddressLabelEntity;
import com.argus.infra.persistence.repository.AddressLabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    public List<AddressLabel> findByAddress(String address) {
        return repository.findByAddressIgnoreCase(address).stream()
                .map(AddressLabelEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressLabel> findByLabel(String label) {
        return repository.findByLabelIgnoreCase(label).stream()
                .map(AddressLabelEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressLabel> findByLabelContaining(String labelPart) {
        return repository.findByLabelContainingIgnoreCase(labelPart).stream()
                .map(AddressLabelEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressLabel> findByCategory(String category) {
        return repository.findByCategory(category).stream()
                .map(AddressLabelEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByAddressAndLabel(String address, String label) {
        return repository.existsByAddressIgnoreCaseAndLabelIgnoreCase(address, label);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByAddress(String address) {
        return repository.countByAddressIgnoreCase(address);
    }

    @Override
    @Transactional
    public void deleteByAddressAndLabel(String address, String label) {
        repository.deleteByAddressAndLabel(address, label);
    }

    @Override
    @Transactional
    public void deleteAllByAddress(String address) {
        repository.deleteAllByAddressIgnoreCase(address);
    }
}
