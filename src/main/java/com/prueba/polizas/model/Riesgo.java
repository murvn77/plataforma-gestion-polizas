package com.prueba.polizas.model;

import com.prueba.polizas.exception.ReglaNegocioException;
import jakarta.persistence.*;

/**
 * Inmueble asegurado dentro de una póliza. Solo se crea y se cancela
 * a través de su Poliza, que valida las reglas del agregado.
 */
@Entity
@Table(name = "riesgo")
public class Riesgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poliza_id", nullable = false)
    private Poliza poliza;

    @Column(nullable = false)
    private String direccionInmueble;

    // Asegurado
    @Column(nullable = false)
    private String arrendatario;

    // Beneficiario en caso de siniestro
    @Column(nullable = false)
    private String arrendador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRiesgo estado;

    protected Riesgo() {
    }

    Riesgo(Poliza poliza, String direccionInmueble, String arrendatario, String arrendador) {
        this.poliza = poliza;
        this.direccionInmueble = direccionInmueble;
        this.arrendatario = arrendatario;
        this.arrendador = arrendador;
        this.estado = EstadoRiesgo.ACTIVO;
    }

    void cancelar() {
        if (estado == EstadoRiesgo.CANCELADO) {
            throw new ReglaNegocioException("El riesgo ya se encuentra cancelado");
        }
        this.estado = EstadoRiesgo.CANCELADO;
    }

    public boolean estaActivo() {
        return estado == EstadoRiesgo.ACTIVO;
    }

    public Long getId() {
        return id;
    }

    public Poliza getPoliza() {
        return poliza;
    }

    public String getDireccionInmueble() {
        return direccionInmueble;
    }

    public String getArrendatario() {
        return arrendatario;
    }

    public String getArrendador() {
        return arrendador;
    }

    public EstadoRiesgo getEstado() {
        return estado;
    }
}
