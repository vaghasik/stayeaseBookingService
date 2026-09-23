package com.apexon.stayease.model;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Auth Response DTO
 * Response payload for successful authentication.
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response payload for successful authentication")
public class AuthResponse {

    @Schema(
            description = "JWT access token",
            example =
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U")
    private String token;

    @Schema(description = "Token type", example = "Bearer")
    private String type;

    @Schema(
            description = "User's unique identifier",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID userId;

    @Schema(description = "User's email address", example = "john.doe@example.com")
    private String email;

    @Schema(description = "User's full name", example = "John Doe")
    private String name;

    @Schema(description = "User's role", example = "GUEST")
    private String role;

    @Schema(description = "Token type identifier", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";
}
