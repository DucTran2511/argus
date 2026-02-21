package com.argus.infra.persistence.entity;

import com.argus.domain.model.AddressLabel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "address_labels", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "address", "label" }))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressLabelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 42)
    private String address;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(length = 50)
    private String category;

    @Column(length = 50)
    @Builder.Default
    private String source = "manual";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (address != null) {
            address = address.toLowerCase();
        }
    }

    public AddressLabel toDomain() {
        return AddressLabel.builder()
                .id(id)
                .userId(userId)
                .address(address)
                .label(label)
                .category(category)
                .source(source)
                .createdAt(createdAt)
                .build();
    }

    public static AddressLabelEntity fromDomain(AddressLabel domain) {
        return AddressLabelEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .address(domain.getAddress() != null ? domain.getAddress().toLowerCase() : null)
                .label(domain.getLabel())
                .category(domain.getCategory())
                .source(domain.getSource())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
