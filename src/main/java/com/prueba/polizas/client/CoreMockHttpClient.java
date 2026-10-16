package com.prueba.polizas.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Component
public class CoreMockHttpClient implements CoreClient {

    private static final Logger log = LoggerFactory.getLogger(CoreMockHttpClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final RestClient restClient;

    public CoreMockHttpClient(RestClient.Builder builder,
                              @Value("${core.url}") String coreUrl,
                              @Value("${seguridad.api-key}") String apiKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT);
        requestFactory.setReadTimeout(TIMEOUT);

        this.restClient = builder
                .baseUrl(coreUrl)
                .requestFactory(requestFactory)
                .defaultHeader("x-api-key", apiKey)
                .build();
    }

    @Override
    public void enviar(EventoCore evento) {
        try {
            restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(evento)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Evento enviado al CORE: {}", evento);
        } catch (RestClientException e) {
            // La operación ya quedó guardada; en producción el evento iría a reintentos y luego a una DLQ.
            log.error("No se pudo enviar el evento {} al CORE: {}", evento, e.getMessage());
        }
    }
}
