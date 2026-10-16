package com.prueba.polizas.dto;

import com.prueba.polizas.model.DatosRiesgo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgregarRiesgoRequest(
        @NotBlank @Size(max = 255) String direccionInmueble,
        @NotBlank @Size(max = 255) String arrendatario,
        @NotBlank @Size(max = 255) String arrendador) {

    public DatosRiesgo aDatosRiesgo() {
        return new DatosRiesgo(direccionInmueble, arrendatario, arrendador);
    }
}
