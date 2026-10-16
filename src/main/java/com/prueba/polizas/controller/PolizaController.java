package com.prueba.polizas.controller;

import com.prueba.polizas.dto.AgregarRiesgoRequest;
import com.prueba.polizas.dto.PolizaResponse;
import com.prueba.polizas.dto.RiesgoResponse;
import com.prueba.polizas.model.EstadoPoliza;
import com.prueba.polizas.model.TipoPoliza;
import com.prueba.polizas.service.PolizaService;
import com.prueba.polizas.service.RiesgoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/polizas")
@Tag(name = "Pólizas")
public class PolizaController {

    private final PolizaService polizaService;
    private final RiesgoService riesgoService;

    public PolizaController(PolizaService polizaService, RiesgoService riesgoService) {
        this.polizaService = polizaService;
        this.riesgoService = riesgoService;
    }

    @GetMapping
    @Operation(summary = "Lista pólizas, filtrando opcionalmente por tipo y estado")
    public List<PolizaResponse> listar(@RequestParam(required = false) TipoPoliza tipo,
                                       @RequestParam(required = false) EstadoPoliza estado) {
        return polizaService.listar(tipo, estado);
    }

    @GetMapping("/{id}/riesgos")
    @Operation(summary = "Lista los riesgos de una póliza")
    public List<RiesgoResponse> listarRiesgos(@PathVariable Long id) {
        return riesgoService.listarPorPoliza(id);
    }

    @PostMapping("/{id}/renovar")
    @Operation(summary = "Renueva la póliza: incrementa canon y prima según el IPC y pasa a RENOVADA")
    public PolizaResponse renovar(@PathVariable Long id) {
        return polizaService.renovar(id);
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancela la póliza y todos sus riesgos")
    public PolizaResponse cancelar(@PathVariable Long id) {
        return polizaService.cancelar(id);
    }

    @PostMapping("/{id}/riesgos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agrega un riesgo a una póliza colectiva")
    public RiesgoResponse agregarRiesgo(@PathVariable Long id, @Valid @RequestBody AgregarRiesgoRequest request) {
        return riesgoService.agregar(id, request);
    }
}
