package com.argus.domain.port.persistence;

import com.argus.domain.model.AddressLabel;

import java.util.List;

public interface AddressBookPersistencePort {
    AddressLabel save(AddressLabel label);

    List<AddressLabel> saveAll(List<AddressLabel> labels);

    List<AddressLabel> findByAddress(String address);

    List<AddressLabel> findByLabel(String label);

    List<AddressLabel> findByLabelContaining(String labelPart);

    List<AddressLabel> findByCategory(String category);

    boolean existsByAddressAndLabel(String address, String label);

    long countByAddress(String address);

    void deleteByAddressAndLabel(String address, String label);

    void deleteAllByAddress(String address);
}
