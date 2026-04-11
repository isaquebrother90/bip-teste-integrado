package com.bip.beneficio.domain.repository;

import com.bip.beneficio.domain.entity.Beneficio;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficioRepository extends JpaRepository<Beneficio, Long> {

    //Busca com lock pessimista (SELECT ... FOR UPDATE) - utilizado nas transferências para evitar race conditions
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Beneficio b WHERE b.id = :id")
    Optional<Beneficio> findByIdWithLock(@Param("id") Long id);

    // Usa native query para contornar @SQLRestriction e acessar registros soft-deleted
    @Query(value = "SELECT * FROM beneficio WHERE id = :id", nativeQuery = true)
    Optional<Beneficio> findByIdIncluindoDeletados(@Param("id") Long id);

    // clearAutomatically=true garante que o L1 cache seja invalidado após o update,
    // evitando que findById retorne a entidade stale do cache na mesma sessão (importante em testes @Transactional)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Beneficio b SET b.deletadoEm = :deletadoEm, b.motivoDesativacao = :motivo WHERE b.id = :id")
    void softDeleteById(@Param("id") Long id, @Param("deletadoEm") LocalDateTime deletadoEm, @Param("motivo") String motivo);

    List<Beneficio> findByAtivoTrue();

    Page<Beneficio> findByAtivoTrue(Pageable pageable);

    List<Beneficio> findByAtivoFalse();

    Page<Beneficio> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    //Busca benefícios ativos por nome
    Page<Beneficio> findByNomeContainingIgnoreCaseAndAtivoTrue(String nome, Pageable pageable);

    boolean existsByNomeIgnoreCase(String nome);

    //Verifica se existe outro benefício com o mesmo nome (exclui o próprio ID da busca, útil para updates)
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Beneficio b WHERE LOWER(b.nome) = LOWER(:nome) AND b.id <> :id")
    boolean existsByNomeIgnoreCaseAndIdNot(@Param("nome") String nome, @Param("id") Long id);
}
