package com.argus.infra.persistence.repository;

import com.argus.infra.persistence.entity.AddressLabelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressLabelRepository extends JpaRepository<AddressLabelEntity, Long> {

    List<AddressLabelEntity> findByAddressIgnoreCase(String address);

    List<AddressLabelEntity> findByLabelIgnoreCase(String label);

    List<AddressLabelEntity> findByLabelContainingIgnoreCase(String labelPart);

    List<AddressLabelEntity> findByCategory(String category);

    boolean existsByAddressIgnoreCaseAndLabelIgnoreCase(String address, String label);

    long countByAddressIgnoreCase(String address);

    List<AddressLabelEntity> findByAddressIgnoreCaseIn(java.util.Collection<String> addresses);

    @Modifying
    @Query("DELETE FROM AddressLabelEntity e WHERE lower(e.address) = lower(:address) AND lower(e.label) = lower(:label)")
    void deleteByAddressAndLabel(@Param("address") String address, @Param("label") String label);

    @Modifying
    void deleteAllByAddressIgnoreCase(String address);
}
