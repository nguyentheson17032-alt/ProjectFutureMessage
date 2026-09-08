package com.futuremessage.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Soft-disable or re-enable a user. Role cannot be changed through this API.")
public record UpdateUserEnabledRequest(
        @NotNull
        @Schema(example = "false", description = "false = cannot login/refresh; existing messages stay.")
        Boolean enabled
) {
}
