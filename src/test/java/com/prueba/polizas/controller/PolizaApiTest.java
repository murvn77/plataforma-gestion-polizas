package com.prueba.polizas.controller;

import com.prueba.polizas.client.CoreClient;
import com.prueba.polizas.client.EventoCore;
import com.prueba.polizas.client.TipoEventoCore;
import com.prueba.polizas.model.DatosRiesgo;
import com.prueba.polizas.model.Poliza;
import com.prueba.polizas.repository.PolizaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PolizaApiTest {

    private static final String API_KEY = "123456";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PolizaRepository polizaRepository;

    @MockitoBean
    private CoreClient coreClient;

    @Test
    void sinApiKeyResponde401() throws Exception {
        mvc.perform(get("/polizas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listaPolizasFiltrandoPorTipo() throws Exception {
        mvc.perform(conApiKey(get("/polizas").param("tipo", "COLECTIVA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipo", everyItem(is("COLECTIVA"))));
    }

    @Test
    void tipoInvalidoResponde400() throws Exception {
        mvc.perform(conApiKey(get("/polizas").param("tipo", "OTRO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void polizaInexistenteResponde404() throws Exception {
        mvc.perform(conApiKey(post("/polizas/99999/renovar")))
                .andExpect(status().isNotFound());
    }

    @Test
    void renovarAplicaIpcYNotificaAlCore() throws Exception {
        Poliza poliza = polizaRepository.save(Poliza.crearIndividual("Ana", "Carlos", "Calle 1",
                LocalDate.of(2026, 1, 1), 12, new BigDecimal("1000000")));

        // IPC configurado: 5,20 % -> 1.000.000 x 1,052 = 1.052.000; prima = 12 meses
        mvc.perform(conApiKey(post("/polizas/{id}/renovar", poliza.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RENOVADA"))
                .andExpect(jsonPath("$.canonMensual").value(1052000.00))
                .andExpect(jsonPath("$.prima").value(12624000.00))
                .andExpect(jsonPath("$.fechaInicio").value("2027-01-01"));

        // El envío al CORE es asíncrono y después del commit: se espera hasta 2 s
        verify(coreClient, timeout(2000)).enviar(new EventoCore(TipoEventoCore.RENOVACION, poliza.getId()));
    }

    @Test
    void noSePuedeRenovarUnaPolizaCancelada() throws Exception {
        Poliza poliza = Poliza.crearIndividual("Jorge", "Lucía", "Calle 2",
                LocalDate.of(2026, 1, 1), 12, new BigDecimal("1000000"));
        poliza.cancelar();
        poliza = polizaRepository.save(poliza);

        mvc.perform(conApiKey(post("/polizas/{id}/renovar", poliza.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("No se puede renovar una póliza cancelada"));
    }

    @Test
    void cancelarPolizaCancelaSusRiesgos() throws Exception {
        Poliza poliza = polizaRepository.save(colectivaConDosRiesgos());

        mvc.perform(conApiKey(post("/polizas/{id}/cancelar", poliza.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        mvc.perform(conApiKey(get("/polizas/{id}/riesgos", poliza.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].estado", everyItem(is("CANCELADO"))));
    }

    @Test
    void agregarRiesgoAColectivaResponde201() throws Exception {
        Poliza poliza = polizaRepository.save(colectivaConDosRiesgos());

        mvc.perform(conApiKey(post("/polizas/{id}/riesgos", poliza.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"direccionInmueble": "Calle 5", "arrendatario": "Sofía", "arrendador": "Elena"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void agregarRiesgoAIndividualResponde409() throws Exception {
        Poliza poliza = polizaRepository.save(Poliza.crearIndividual("Ana", "Carlos", "Calle 1",
                LocalDate.of(2026, 1, 1), 12, new BigDecimal("1000000")));

        mvc.perform(conApiKey(post("/polizas/{id}/riesgos", poliza.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"direccionInmueble": "Calle 5", "arrendatario": "Sofía", "arrendador": "Elena"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void agregarRiesgoSinDatosResponde400() throws Exception {
        Poliza poliza = polizaRepository.save(colectivaConDosRiesgos());

        mvc.perform(conApiKey(post("/polizas/{id}/riesgos", poliza.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.direccionInmueble").exists());
    }

    @Test
    void cancelarElUltimoRiesgoActivoResponde409() throws Exception {
        Poliza poliza = polizaRepository.save(colectivaConDosRiesgos());
        Long primero = poliza.getRiesgos().get(0).getId();
        Long segundo = poliza.getRiesgos().get(1).getId();

        mvc.perform(conApiKey(post("/riesgos/{id}/cancelar", primero)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));

        mvc.perform(conApiKey(post("/riesgos/{id}/cancelar", segundo)))
                .andExpect(status().isConflict());
    }

    @Test
    void coreMockAceptaElEvento() throws Exception {
        mvc.perform(conApiKey(post("/core-mock/evento"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"evento": "ACTUALIZACION", "polizaId": 555}
                                """))
                .andExpect(status().isAccepted());
    }

    private MockHttpServletRequestBuilder conApiKey(MockHttpServletRequestBuilder request) {
        return request.header("x-api-key", API_KEY);
    }

    private Poliza colectivaConDosRiesgos() {
        return Poliza.crearColectiva("Inmobiliaria", LocalDate.of(2026, 1, 1), 12, new BigDecimal("1000000"),
                List.of(new DatosRiesgo("Calle 3", "Laura", "Pedro"),
                        new DatosRiesgo("Calle 4", "Andrés", "Pedro")));
    }
}
