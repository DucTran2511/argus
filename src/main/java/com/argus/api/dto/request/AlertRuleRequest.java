package com.argus.api.dto.request;

import com.argus.domain.model.AlertConditions;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "conditions is required")
    private AlertConditions conditions;

    private List<String> channels;

    @Builder.Default
    private boolean enabled = true;
}
