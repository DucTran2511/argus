package com.argus.api.dto.response;

import com.argus.domain.model.AddressLabel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelResponse {

        private String address;
        private List<LabelDto> labels;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class LabelDto {
                private Long id;
                private String label;
                private String category;
                private String source;
                private LocalDateTime createdAt;
        }

        public static LabelResponse fromDomainList(String address, List<AddressLabel> labels) {
                return LabelResponse.builder()
                                .address(address)
                                .labels(labels.stream()
                                                .map(l -> LabelDto.builder()
                                                                .id(l.getId())
                                                                .label(l.getLabel())
                                                                .category(l.getCategory())
                                                                .source(l.getSource())
                                                                .createdAt(l.getCreatedAt())
                                                                .build())
                                                .collect(Collectors.toList()))
                                .build();
        }

        public static LabelResponse fromDomain(AddressLabel label) {
                return LabelResponse.builder()
                                .address(label.getAddress())
                                .labels(List.of(LabelDto.builder()
                                                .id(label.getId())
                                                .label(label.getLabel())
                                                .category(label.getCategory())
                                                .source(label.getSource())
                                                .createdAt(label.getCreatedAt())
                                                .build()))
                                .build();
        }

        public static List<LabelResponse> fromDomainListGrouped(List<AddressLabel> labels) {
                return labels.stream()
                                .collect(Collectors.groupingBy(AddressLabel::getAddress))
                                .entrySet().stream()
                                .map(e -> fromDomainList(e.getKey(), e.getValue()))
                                .collect(Collectors.toList());
        }
}
