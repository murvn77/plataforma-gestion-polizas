package com.prueba.polizas.repository;

import com.prueba.polizas.model.Riesgo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Solo para consultas. Los cambios sobre riesgos se hacen a través de su Poliza.
 */
public interface RiesgoRepository extends JpaRepository<Riesgo, Long> {

    List<Riesgo> findByPolizaIdOrderById(Long polizaId);

    @Query("SELECT r FROM Riesgo r JOIN FETCH r.poliza WHERE r.id = :id")
    Optional<Riesgo> buscarConPoliza(@Param("id") Long id);
}
