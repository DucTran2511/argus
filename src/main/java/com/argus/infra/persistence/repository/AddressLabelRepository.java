package com.argus.infra.persistence.repository;

import com.argus.infra.persistence.entity.AddressLabelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface AddressLabelRepository extends JpaRepository<AddressLabelEntity, Long> {

    List<AddressLabelEntity> findByAddressIgnoreCaseAndUserId(String address, UUID userId);

    List<AddressLabelEntity> findByLabelIgnoreCaseAndUserId(String label, UUID userId);

    List<AddressLabelEntity> findByLabelContainingIgnoreCaseAndUserId(String labelPart, UUID userId);

    List<AddressLabelEntity> findByCategoryAndUserId(String category, UUID userId);

    boolean existsByAddressIgnoreCaseAndLabelIgnoreCaseAndUserId(String address, String label, UUID userId);

    long countByAddressIgnoreCaseAndUserId(String address, UUID userId);

    List<AddressLabelEntity> findByAddressIgnoreCaseInAndUserId(Collection<String> addresses, UUID userId);

    @Modifying
    @Query("DELETE FROM AddressLabelEntity e WHERE lower(e.address) = lower(:address) AND lower(e.label) = lower(:label) AND e.userId = :userId")
    void deleteByAddressAndLabelAndUserId(@Param("address") String address, @Param("label") String label,
            @Param("userId") UUID userId);

    @Modifying
    void deleteAllByAddressIgnoreCaseAndUserId(String address, UUID userId);
}
