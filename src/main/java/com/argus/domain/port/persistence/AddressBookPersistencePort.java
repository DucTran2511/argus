package com.argus.domain.port.persistence;

import com.argus.domain.model.AddressLabel;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AddressBookPersistencePort {
    AddressLabel save(AddressLabel label);

    List<AddressLabel> saveAll(List<AddressLabel> labels);

    List<AddressLabel> findByAddressAndUserId(String address, UUID userId);

    List<AddressLabel> findByAddressesAndUserId(Set<String> addresses, UUID userId);

    List<AddressLabel> findByLabelAndUserId(String label, UUID userId);

    List<AddressLabel> findByLabelContainingAndUserId(String labelPart, UUID userId);

    List<AddressLabel> findByCategoryAndUserId(String category, UUID userId);

    boolean existsByAddressAndLabelAndUserId(String address, String label, UUID userId);

    long countByAddressAndUserId(String address, UUID userId);

    void deleteByAddressAndLabelAndUserId(String address, String label, UUID userId);
}
