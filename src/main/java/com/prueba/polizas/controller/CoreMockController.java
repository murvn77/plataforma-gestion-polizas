package com.prueba.polizas.controller;

import com.prueba.polizas.dto.CoreEventoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/core-mock")
@Tag(name = "CORE (mock)")
public class CoreMockController {

    private static final Logger log = LoggerFactory.getLogger(CoreMockController.class);

    @PostMapping("/evento")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Registra en logs un evento que se intentó enviar al CORE")
    public void recibirEvento(@Valid @RequestBody CoreEventoRequest request) {
        log.info("[CORE-MOCK] Se intentó enviar al CORE: evento={}, polizaId={}",
                request.evento(), request.polizaId());
    }
}
