package com.argus.domain.service;

import com.argus.core.exception.LabelAlreadyExistsException;
import com.argus.core.exception.LabelNotFoundException;
import com.argus.core.exception.MaxLabelsExceededException;
import com.argus.domain.model.AddressLabel;
import com.argus.domain.port.persistence.AddressBookPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;

@Slf4j
@RequiredArgsConstructor
public class AddressBookService {

    private final AddressBookPersistencePort addressBookPersistencePort;
    private final Clock clock;

    public AddressLabel addLabel(String address, String label, String category, UUID userId) {
        String normalizedAddress = normalizeAddress(address);
        validateLabel(label);
        validateCategory(category);

        long currentCount = addressBookPersistencePort.countByAddressAndUserId(normalizedAddress, userId);
        if (currentCount >= AddressLabel.MAX_LABELS_PER_ADDRESS) {
            throw new MaxLabelsExceededException(normalizedAddress, AddressLabel.MAX_LABELS_PER_ADDRESS);
        }

        AddressLabel newLabel = AddressLabel.builder()
                .userId(userId)
                .address(normalizedAddress)
                .label(label.trim())
                .category(category != null ? category.trim() : null)
                .source("manual")
                .createdAt(LocalDateTime.now(clock))
                .build();

        try {
            log.info("Adding label '{}' to address {} for user {}", label, normalizedAddress, userId);
            return addressBookPersistencePort.save(newLabel);
        } catch (DataIntegrityViolationException e) {
            throw new LabelAlreadyExistsException(normalizedAddress, label);
        }
    }

    public List<AddressLabel> getLabels(String address, UUID userId) {
        List<AddressLabel> labels = addressBookPersistencePort.findByAddressAndUserId(normalizeAddress(address),
                userId);
        if (labels.isEmpty()) {
            throw new LabelNotFoundException(address);
        }
        return labels;
    }

    public void removeLabel(String address, String label, UUID userId) {
        String normalizedAddress = normalizeAddress(address);
        if (!addressBookPersistencePort.existsByAddressAndLabelAndUserId(normalizedAddress, label, userId)) {
            throw new LabelNotFoundException(normalizedAddress);
        }
        log.info("Removing label '{}' from address {} for user {}", label, normalizedAddress, userId);
        addressBookPersistencePort.deleteByAddressAndLabelAndUserId(normalizedAddress, label, userId);
    }

    public List<AddressLabel> search(String label, String q, String category, UUID userId) {
        if (label != null) {
            return addressBookPersistencePort.findByLabelAndUserId(label.trim(), userId);
        } else if (q != null) {
            return addressBookPersistencePort.findByLabelContainingAndUserId(q.trim(), userId);
        } else if (category != null) {
            return addressBookPersistencePort.findByCategoryAndUserId(category.trim(), userId);
        }
        throw new IllegalArgumentException("Must provide label, q, or category parameter");
    }

    public List<AddressLabel> importLabels(List<AddressLabel> imports, UUID userId) {
        if (imports == null || imports.isEmpty()) {
            return List.of();
        }

        log.info("Importing {} labels for user {}", imports.size(), userId);

        List<AddressLabel> normalized = imports.stream()
                .filter(l -> isValidLabelForImport(l.getLabel()))
                .map(l -> AddressLabel.builder()
                        .userId(userId)
                        .address(normalizeAddress(l.getAddress()))
                        .label(l.getLabel().trim())
                        .category(l.getCategory() != null ? l.getCategory().trim() : null)
                        .source("csv_import")
                        .createdAt(LocalDateTime.now(clock))
                        .build())
                .toList();

        Set<String> addresses = normalized.stream()
                .map(AddressLabel::getAddress)
                .collect(Collectors.toSet());

        List<AddressLabel> existing = addressBookPersistencePort.findByAddressesAndUserId(addresses, userId);

        Set<String> existingPairs = existing.stream()
                .map(l -> l.getAddress() + "|" + l.getLabel().toLowerCase())
                .collect(Collectors.toSet());

        Map<String, Long> countByAddress = existing.stream()
                .collect(Collectors.groupingBy(AddressLabel::getAddress, Collectors.counting()));

        List<AddressLabel> toSave = new ArrayList<>();
        int skipped = 0;

        for (AddressLabel label : normalized) {
            String pairKey = label.getAddress() + "|" + label.getLabel().toLowerCase();

            if (existingPairs.contains(pairKey)) {
                skipped++;
                continue;
            }

            long currentCount = countByAddress.getOrDefault(label.getAddress(), 0L);
            long pendingCount = toSave.stream()
                    .filter(l -> l.getAddress().equals(label.getAddress()))
                    .count();

            if (currentCount + pendingCount >= AddressLabel.MAX_LABELS_PER_ADDRESS) {
                log.warn("Skipping '{}' for {} - max labels reached", label.getLabel(), label.getAddress());
                skipped++;
                continue;
            }

            toSave.add(label);
            existingPairs.add(pairKey);
        }

        List<AddressLabel> saved = addressBookPersistencePort.saveAll(toSave);
        log.info("Imported {} labels, skipped {}", saved.size(), skipped);

        return saved;
    }

    private String normalizeAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Address cannot be null or empty");
        }
        return address.toLowerCase().trim();
    }

    private void validateLabel(String label) {
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("Label cannot be empty");
        }
        if (label.length() > 100) {
            throw new IllegalArgumentException("Label exceeds 100 characters");
        }
    }

    private void validateCategory(String category) {
        if (category != null && category.length() > 50) {
            throw new IllegalArgumentException("Category exceeds 50 characters");
        }
    }

    private boolean isValidLabelForImport(String label) {
        return label != null && !label.trim().isEmpty() && label.length() <= 100;
    }
}
