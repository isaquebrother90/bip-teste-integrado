package com.bip.beneficio.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade que representa um benefício no sistema.
 *
 * <p>Utiliza <b>Optimistic Locking</b> através do campo {@code version} para prevenir
 * lost updates em operações concorrentes. Em operações críticas como transferências,
 * também é utilizado Pessimistic Locking no repositório.</p>
 *
 * <p><b>Soft Delete</b>: registros nunca são deletados fisicamente. O campo {@code deletadoEm}
 * marca o momento da exclusão. A anotação {@code @SQLRestriction} filtra automaticamente
 * todos os registros deletados em qualquer consulta JPA, sem necessidade de filtro manual.</p>
 *
 * @see com.bip.beneficio.domain.service.BeneficioService Service com lógica de negócio
 * @see com.bip.beneficio.domain.repository.BeneficioRepository Repository com pessimistic lock
 */
@Entity
@Table(name = "beneficio", indexes = {
        @Index(name = "idx_beneficio_nome", columnList = "nome"),
        @Index(name = "idx_beneficio_ativo", columnList = "ativo"),
        @Index(name = "idx_beneficio_deletado", columnList = "deletado_em")
})
@SQLRestriction("deletado_em IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"descricao"})
public class Beneficio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "descricao", length = 255)
    private String descricao;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    /**
     * Campo de versão para Optimistic Locking.
     * Previne lost updates em operações concorrentes.
     */
    @Version
    @Column(name = "version")
    private Long version;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    /**
     * Data/hora de remoção lógica (soft delete).
     * Nulo = registro ativo. Preenchido = removido.
     * O filtro {@code @SQLRestriction("deletado_em IS NULL")} garante que registros
     * deletados nunca aparecem em consultas JPA normais.
     */
    @Column(name = "deletado_em")
    private LocalDateTime deletadoEm;

    /**
     * Motivo da remoção — rastreabilidade operacional.
     * Exemplo: "Benefício encerrado por reestruturação", "Duplicado de #42".
     */
    @Column(name = "motivo_desativacao", length = 255)
    private String motivoDesativacao;
}
