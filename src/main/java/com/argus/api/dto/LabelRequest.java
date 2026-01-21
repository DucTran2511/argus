package com.argus.api.dto;

import com.argus.domain.model.AddressLabel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelRequest {

    @NotBlank(message = "Address is required")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "Invalid Ethereum address format")
    private String address;

    @NotBlank(message = "Label is required")
    @Size(max = 100, message = "Label must be 100 characters or less")
    private String label;

    @Size(max = 50, message = "Category must be 50 characters or less")
    private String category;

    public static List<AddressLabel> toDomainList(List<LabelRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Empty import data");
        }
        return requests.stream()
                .map(r -> AddressLabel.builder()
                        .address(r.getAddress())
                        .label(r.getLabel())
                        .category(r.getCategory())
                        .build())
                .collect(Collectors.toList());
    }
}
