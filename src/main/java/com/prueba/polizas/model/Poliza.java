package com.prueba.polizas.model;

import com.prueba.polizas.exception.ReglaNegocioException;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Raíz del agregado Póliza: todos los cambios sobre sus riesgos pasan por aquí,
 * así las reglas de negocio quedan en un solo lugar.
 */
@Entity
@Table(name = "poliza")
public class Poliza {

    private static final BigDecimal CIEN = new BigDecimal("100");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPoliza tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPoliza estado;

    @Column(nullable = false)
    private String tomador;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false)
    private int mesesVigencia;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal canonMensual;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal prima;

    @Version
    private Long version;

    @OneToMany(mappedBy = "poliza", cascade = CascadeType.ALL)
    private List<Riesgo> riesgos = new ArrayList<>();

    protected Poliza() {
    }

    private Poliza(TipoPoliza tipo, String tomador, LocalDate fechaInicio,
                   int mesesVigencia, BigDecimal canonMensual) {
        if (mesesVigencia <= 0) {
            throw new ReglaNegocioException("La vigencia debe ser de al menos un mes");
        }
        if (canonMensual == null || canonMensual.signum() <= 0) {
            throw new ReglaNegocioException("El canon mensual debe ser mayor a cero");
        }
        this.tipo = tipo;
        this.tomador = tomador;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaInicio.plusMonths(mesesVigencia);
        this.mesesVigencia = mesesVigencia;
        this.canonMensual = canonMensual.setScale(2, RoundingMode.HALF_UP);
        this.prima = calcularPrima();
        this.estado = EstadoPoliza.ACTIVA;
    }

    public static Poliza crearIndividual(String arrendatario, String arrendador, String direccionInmueble,
                                         LocalDate fechaInicio, int mesesVigencia, BigDecimal canonMensual) {
        Poliza poliza = new Poliza(TipoPoliza.INDIVIDUAL, arrendatario, fechaInicio, mesesVigencia, canonMensual);
        poliza.riesgos.add(new Riesgo(poliza, direccionInmueble, arrendatario, arrendador));
        return poliza;
    }

    public static Poliza crearColectiva(String tomador, LocalDate fechaInicio, int mesesVigencia,
                                        BigDecimal canonMensual, List<DatosRiesgo> riesgosIniciales) {
        if (riesgosIniciales == null || riesgosIniciales.isEmpty()) {
            throw new ReglaNegocioException("Una póliza colectiva debe tener al menos un riesgo");
        }
        Poliza poliza = new Poliza(TipoPoliza.COLECTIVA, tomador, fechaInicio, mesesVigencia, canonMensual);
        riesgosIniciales.forEach(datos -> poliza.riesgos.add(
                new Riesgo(poliza, datos.direccionInmueble(), datos.arrendatario(), datos.arrendador())));
        return poliza;
    }

    public Riesgo agregarRiesgo(DatosRiesgo datos) {
        validarNoCancelada("agregar riesgos a");
        if (tipo != TipoPoliza.COLECTIVA) {
            throw new ReglaNegocioException(
                    "Solo se pueden agregar riesgos a pólizas colectivas; una póliza individual tiene un único riesgo");
        }
        Riesgo riesgo = new Riesgo(this, datos.direccionInmueble(), datos.arrendatario(), datos.arrendador());
        riesgos.add(riesgo);
        return riesgo;
    }

    public void cancelarRiesgo(Riesgo riesgo) {
        validarNoCancelada("cancelar riesgos de");
        if (tipo == TipoPoliza.INDIVIDUAL) {
            throw new ReglaNegocioException(
                    "El riesgo de una póliza individual no se cancela por separado; cancele la póliza");
        }
        // Una colectiva nunca queda sin riesgos activos: para eso se cancela la póliza.
        if (riesgo.estaActivo() && contarRiesgosActivos() == 1) {
            throw new ReglaNegocioException(
                    "No se puede cancelar el último riesgo activo de la póliza; cancele la póliza");
        }
        riesgo.cancelar();
    }

    /**
     * @param ipcPorcentaje IPC en porcentaje, ej. 5.20 para 5,20 %
     */
    public void renovar(BigDecimal ipcPorcentaje) {
        validarNoCancelada("renovar");
        if (ipcPorcentaje == null) {
            throw new ReglaNegocioException("No hay IPC vigente para renovar la póliza");
        }
        BigDecimal factor = BigDecimal.ONE.add(ipcPorcentaje.divide(CIEN, 6, RoundingMode.HALF_UP));
        this.canonMensual = canonMensual.multiply(factor).setScale(2, RoundingMode.HALF_UP);
        this.prima = calcularPrima();
        this.fechaInicio = fechaFin;
        this.fechaFin = fechaFin.plusMonths(mesesVigencia);
        this.estado = EstadoPoliza.RENOVADA;
    }

    public void cancelar() {
        validarNoCancelada("cancelar");
        this.estado = EstadoPoliza.CANCELADA;
        riesgos.stream()
                .filter(Riesgo::estaActivo)
                .forEach(Riesgo::cancelar);
    }

    private long contarRiesgosActivos() {
        return riesgos.stream().filter(Riesgo::estaActivo).count();
    }

    private BigDecimal calcularPrima() {
        return canonMensual.multiply(BigDecimal.valueOf(mesesVigencia)).setScale(2, RoundingMode.HALF_UP);
    }

    private void validarNoCancelada(String accion) {
        if (estado == EstadoPoliza.CANCELADA) {
            throw new ReglaNegocioException("No se puede " + accion + " una póliza cancelada");
        }
    }

    public Long getId() {
        return id;
    }

    public TipoPoliza getTipo() {
        return tipo;
    }

    public EstadoPoliza getEstado() {
        return estado;
    }

    public String getTomador() {
        return tomador;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public int getMesesVigencia() {
        return mesesVigencia;
    }

    public BigDecimal getCanonMensual() {
        return canonMensual;
    }

    public BigDecimal getPrima() {
        return prima;
    }

    public List<Riesgo> getRiesgos() {
        return Collections.unmodifiableList(riesgos);
    }
}
