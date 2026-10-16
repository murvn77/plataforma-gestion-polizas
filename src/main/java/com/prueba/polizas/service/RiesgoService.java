package com.prueba.polizas.service;

import com.prueba.polizas.client.EventoCore;
import com.prueba.polizas.client.TipoEventoCore;
import com.prueba.polizas.dto.AgregarRiesgoRequest;
import com.prueba.polizas.dto.RiesgoResponse;
import com.prueba.polizas.exception.RecursoNoEncontradoException;
import com.prueba.polizas.model.Poliza;
import com.prueba.polizas.model.Riesgo;
import com.prueba.polizas.repository.PolizaRepository;
import com.prueba.polizas.repository.RiesgoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RiesgoService {

    private final PolizaRepository polizaRepository;
    private final RiesgoRepository riesgoRepository;
    private final ApplicationEventPublisher eventos;

    public RiesgoService(PolizaRepository polizaRepository,
                         RiesgoRepository riesgoRepository,
                         ApplicationEventPublisher eventos) {
        this.polizaRepository = polizaRepository;
        this.riesgoRepository = riesgoRepository;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public List<RiesgoResponse> listarPorPoliza(Long polizaId) {
        if (!polizaRepository.existsById(polizaId)) {
            throw new RecursoNoEncontradoException("No existe la póliza " + polizaId);
        }
        return riesgoRepository.findByPolizaIdOrderById(polizaId).stream()
                .map(RiesgoResponse::desde)
                .toList();
    }

    @Transactional
    public RiesgoResponse agregar(Long polizaId, AgregarRiesgoRequest request) {
        Poliza poliza = polizaRepository.findById(polizaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la póliza " + polizaId));

        Riesgo riesgo = poliza.agregarRiesgo(request.aDatosRiesgo());
        // flush y no save(): save() haría merge y persistiría una copia, dejando a 'riesgo' sin id
        polizaRepository.flush();

        eventos.publishEvent(new EventoCore(TipoEventoCore.ADICION_RIESGO, polizaId));
        return RiesgoResponse.desde(riesgo);
    }

    @Transactional
    public RiesgoResponse cancelar(Long riesgoId) {
        Riesgo riesgo = riesgoRepository.buscarConPoliza(riesgoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el riesgo " + riesgoId));

        Poliza poliza = riesgo.getPoliza();
        poliza.cancelarRiesgo(riesgo);

        eventos.publishEvent(new EventoCore(TipoEventoCore.CANCELACION_RIESGO, poliza.getId()));
        return RiesgoResponse.desde(riesgo);
    }
}
