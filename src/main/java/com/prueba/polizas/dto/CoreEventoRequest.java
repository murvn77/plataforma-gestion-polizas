package com.prueba.polizas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CoreEventoRequest(@NotBlank String evento, @NotNull Long polizaId) {
}
