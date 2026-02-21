package com.argus.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressLabel {

    private Long id;
    private UUID userId;
    private String address;
    private String label;
    private String category;

    @Default
    private String source = "manual";
    private LocalDateTime createdAt;

    public static final int MAX_LABELS_PER_ADDRESS = 5;

    public enum Category {
        WHALE("whale"),
        CEX("cex"),
        DEX("dex"),
        SCAM("scam"),
        VC("vc"),
        INFLUENCER("influencer"),
        FOUNDATION("foundation"),
        CUSTOM("custom");

        private final String value;

        Category(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
