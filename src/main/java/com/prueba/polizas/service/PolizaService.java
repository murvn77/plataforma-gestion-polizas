package com.prueba.polizas.service;

import com.prueba.polizas.client.EventoCore;
import com.prueba.polizas.client.TipoEventoCore;
import com.prueba.polizas.dto.PolizaResponse;
import com.prueba.polizas.exception.RecursoNoEncontradoException;
import com.prueba.polizas.model.EstadoPoliza;
import com.prueba.polizas.model.Poliza;
import com.prueba.polizas.model.TipoPoliza;
import com.prueba.polizas.repository.PolizaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PolizaService {

    private final PolizaRepository polizaRepository;
    private final ApplicationEventPublisher eventos;
    private final BigDecimal ipcPorcentaje;

    public PolizaService(PolizaRepository polizaRepository,
                         ApplicationEventPublisher eventos,
                         @Value("${polizas.ipc-porcentaje}") BigDecimal ipcPorcentaje) {
        this.polizaRepository = polizaRepository;
        this.eventos = eventos;
        this.ipcPorcentaje = ipcPorcentaje;
    }

    @Transactional(readOnly = true)
    public List<PolizaResponse> listar(TipoPoliza tipo, EstadoPoliza estado) {
        return polizaRepository.buscar(tipo, estado).stream()
                .map(PolizaResponse::desde)
                .toList();
    }

    @Transactional
    public PolizaResponse renovar(Long id) {
        Poliza poliza = obtener(id);
        poliza.renovar(ipcPorcentaje);
        eventos.publishEvent(new EventoCore(TipoEventoCore.RENOVACION, poliza.getId()));
        return PolizaResponse.desde(poliza);
    }

    @Transactional
    public PolizaResponse cancelar(Long id) {
        Poliza poliza = obtener(id);
        poliza.cancelar();
        eventos.publishEvent(new EventoCore(TipoEventoCore.CANCELACION, poliza.getId()));
        return PolizaResponse.desde(poliza);
    }

    private Poliza obtener(Long id) {
        return polizaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la póliza " + id));
    }
}
