package com.prueba.polizas.client;

/**
 * Puerto hacia el CORE de seguros (arquitectura hexagonal). Los servicios dependen
 * de esta interfaz, no de cómo se llega al CORE (mock HTTP, WebLogic, SOAP...).
 */
public interface CoreClient {

    void enviar(EventoCore evento);
}
