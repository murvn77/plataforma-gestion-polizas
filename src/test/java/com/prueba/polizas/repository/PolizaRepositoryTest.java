package com.prueba.polizas.repository;

import com.prueba.polizas.model.DatosRiesgo;
import com.prueba.polizas.model.EstadoPoliza;
import com.prueba.polizas.model.Poliza;
import com.prueba.polizas.model.Riesgo;
import com.prueba.polizas.model.TipoPoliza;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PolizaRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PolizaRepository polizaRepository;

    @Autowired
    private RiesgoRepository riesgoRepository;

    private Poliza colectiva;

    @BeforeEach
    void setUp() {
        Poliza individualActiva = Poliza.crearIndividual("Ana", "Carlos", "Calle 1",
                LocalDate.of(2026, 1, 1), 12, new BigDecimal("1000000"));
        Poliza individualCancelada = Poliza.crearIndividual("Jorge", "Lucía", "Calle 2",
                LocalDate.of(2026, 1, 1), 12, new BigDecimal("1000000"));
        individualCancelada.cancelar();
        colectiva = Poliza.crearColectiva("Inmobiliaria", LocalDate.of(2026, 1, 1), 12, new BigDecimal("1000000"),
                List.of(new DatosRiesgo("Calle 3", "Laura", "Pedro"),
                        new DatosRiesgo("Calle 4", "Andrés", "Pedro")));

        polizaRepository.saveAll(List.of(individualActiva, individualCancelada, colectiva));
        // Limpia la caché de JPA para que las consultas vayan realmente a la BD
        em.flush();
        em.clear();
    }

    @Test
    void sinFiltrosDevuelveTodas() {
        assertThat(polizaRepository.buscar(null, null)).hasSize(3);
    }

    @Test
    void filtraPorTipo() {
        assertThat(polizaRepository.buscar(TipoPoliza.INDIVIDUAL, null)).hasSize(2);
    }

    @Test
    void filtraPorTipoYEstado() {
        List<Poliza> resultado = polizaRepository.buscar(TipoPoliza.INDIVIDUAL, EstadoPoliza.CANCELADA);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getTomador()).isEqualTo("Jorge");
    }

    @Test
    void listaLosRiesgosDeUnaPoliza() {
        assertThat(riesgoRepository.findByPolizaIdOrderById(colectiva.getId()))
                .extracting(Riesgo::getDireccionInmueble)
                .containsExactly("Calle 3", "Calle 4");
    }

    @Test
    void buscarConPolizaTraeLaPolizaEnLaMismaConsulta() {
        Long riesgoId = riesgoRepository.findByPolizaIdOrderById(colectiva.getId()).get(0).getId();
        em.clear();

        Riesgo riesgo = riesgoRepository.buscarConPoliza(riesgoId).orElseThrow();

        // Con JOIN FETCH la póliza ya viene cargada; sin él sería un proxy LAZY sin inicializar
        assertThat(Hibernate.isInitialized(riesgo.getPoliza())).isTrue();
    }
}
