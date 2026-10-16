package com.prueba.polizas.client;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envía al CORE solo los cambios que quedaron confirmados en la base de datos
 * (AFTER_COMMIT), y lo hace en otro hilo (@Async) para no hacer esperar al usuario.
 */
@Component
public class CoreEventListener {

    private final CoreClient coreClient;

    public CoreEventListener(CoreClient coreClient) {
        this.coreClient = coreClient;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alConfirmarCambio(EventoCore evento) {
        coreClient.enviar(evento);
    }
}
