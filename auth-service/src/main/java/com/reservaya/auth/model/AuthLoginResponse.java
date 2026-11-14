package com.reservaya.auth.model;

import jakarta.validation.constraints.NotBlank;

public record AuthLoginResponse(@NotBlank String username,
							  @NotBlank String message,
							  @NotBlank String jwt) {}
