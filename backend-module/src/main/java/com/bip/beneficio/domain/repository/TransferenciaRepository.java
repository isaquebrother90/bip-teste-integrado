package com.bip.beneficio.domain.repository;

import com.bip.beneficio.domain.entity.Transferencia;
import com.bip.beneficio.domain.entity.TransferenciaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransferenciaRepository extends JpaRepository<Transferencia, Long> {

    Optional<Transferencia> findByCorrelacaoId(String correlacaoId);

    boolean existsByCorrelacaoId(String correlacaoId);

    Page<Transferencia> findByStatus(TransferenciaStatus status, Pageable pageable);

    @Query("SELECT t FROM Transferencia t WHERE t.origemId = :beneficioId OR t.destinoId = :beneficioId ORDER BY t.iniciadaEm DESC")
    Page<Transferencia> findByBeneficioId(@Param("beneficioId") Long beneficioId, Pageable pageable);

    Page<Transferencia> findByOrigemId(Long origemId, Pageable pageable);

    Page<Transferencia> findByDestinoId(Long destinoId, Pageable pageable);
}
