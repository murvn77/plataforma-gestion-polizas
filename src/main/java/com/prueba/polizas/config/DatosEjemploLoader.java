package com.prueba.polizas.config;

import com.prueba.polizas.model.DatosRiesgo;
import com.prueba.polizas.model.Poliza;
import com.prueba.polizas.repository.PolizaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class DatosEjemploLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosEjemploLoader.class);

    private final PolizaRepository polizaRepository;

    public DatosEjemploLoader(PolizaRepository polizaRepository) {
        this.polizaRepository = polizaRepository;
    }

    @Override
    public void run(String... args) {
        if (polizaRepository.count() > 0) {
            log.info("La base ya tiene pólizas; no se cargan datos de ejemplo");
            return;
        }

        Poliza individualActiva = Poliza.crearIndividual(
                "Ana María Gómez", "Carlos Ruiz", "Calle 45 # 12-30, Apto 501, Bogotá",
                LocalDate.of(2026, 1, 15), 12, new BigDecimal("1500000"));

        Poliza individualCancelada = Poliza.crearIndividual(
                "Jorge Pérez", "Lucía Martínez", "Carrera 7 # 72-10, Apto 302, Bogotá",
                LocalDate.of(2025, 11, 1), 12, new BigDecimal("2200000"));
        individualCancelada.cancelar();

        Poliza colectivaInmobiliaria = Poliza.crearColectiva(
                "Inmobiliaria Los Andes S.A.S.", LocalDate.of(2026, 3, 1), 12, new BigDecimal("1800000"),
                List.of(
                        new DatosRiesgo("Calle 100 # 15-20, Apto 201, Bogotá", "Laura Torres", "Inversiones Ríos S.A.S."),
                        new DatosRiesgo("Calle 100 # 15-20, Apto 402, Bogotá", "Andrés Castro", "Inversiones Ríos S.A.S."),
                        new DatosRiesgo("Avenida 19 # 120-45, Local 3, Bogotá", "Panadería El Trigal", "Marta Salazar")));

        Poliza colectivaCopropiedad = Poliza.crearColectiva(
                "Administración Edificio Torres del Parque", LocalDate.of(2026, 6, 1), 6, new BigDecimal("1200000"),
                List.of(
                        new DatosRiesgo("Carrera 5 # 26-60, Apto 1001, Bogotá", "Sofía Herrera", "Pedro Vargas"),
                        new DatosRiesgo("Carrera 5 # 26-60, Apto 1203, Bogotá", "Miguel Ángel Rojas", "Elena Quintero")));

        polizaRepository.saveAll(List.of(
                individualActiva, individualCancelada, colectivaInmobiliaria, colectivaCopropiedad));

        log.info("Datos de ejemplo cargados: {} pólizas", polizaRepository.count());
    }
}
