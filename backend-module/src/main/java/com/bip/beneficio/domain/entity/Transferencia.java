package com.bip.beneficio.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade de domínio que representa uma transferência financeira entre benefícios.
 *
 * <p>Em sistemas financeiros, a transferência é um <b>evento imutável de primeira classe</b>,
 * não apenas um efeito colateral registrado após o fato. Esta entidade captura:</p>
 * <ul>
 *   <li>O ciclo de vida completo via {@link TransferenciaStatus} (PENDENTE → PROCESSANDO → CONCLUIDA/FALHA)</li>
 *   <li>Snapshot dos saldos antes e depois — essencial para auditoria regulatória</li>
 *   <li>Idempotency key ({@code correlacaoId}) — previne processamento duplicado em reenvios</li>
 * </ul>
 *
 * <p><b>Imutabilidade</b>: registros de transferência nunca devem ser deletados ou editados,
 * apenas lidos e ter o status atualizado.</p>
 */
@Entity
@Table(name = "transferencia", indexes = {
        @Index(name = "idx_transferencia_correlacao", columnList = "correlacao_id", unique = true),
        @Index(name = "idx_transferencia_origem", columnList = "origem_id"),
        @Index(name = "idx_transferencia_destino", columnList = "destino_id"),
        @Index(name = "idx_transferencia_status", columnList = "status"),
        @Index(name = "idx_transferencia_iniciada", columnList = "iniciada_em")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Transferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Chave de idempotência (UUID gerado pelo cliente ou pelo servidor).
     * Garante que reenvios da mesma requisição não resultem em transferências duplicadas.
     */
    @Column(name = "correlacao_id", nullable = false, unique = true, length = 36)
    private String correlacaoId;

    @Column(name = "origem_id", nullable = false)
    private Long origemId;

    @Column(name = "destino_id", nullable = false)
    private Long destinoId;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransferenciaStatus status;

    /** Snapshot do saldo da origem ANTES do débito — imutável após registro. */
    @Column(name = "saldo_origem_antes", precision = 15, scale = 2)
    private BigDecimal saldoOrigemAntes;

    /** Snapshot do saldo da origem APÓS o débito. */
    @Column(name = "saldo_origem_depois", precision = 15, scale = 2)
    private BigDecimal saldoOrigemDepois;

    /** Snapshot do saldo do destino ANTES do crédito — imutável após registro. */
    @Column(name = "saldo_destino_antes", precision = 15, scale = 2)
    private BigDecimal saldoDestinoAntes;

    /** Snapshot do saldo do destino APÓS o crédito. */
    @Column(name = "saldo_destino_depois", precision = 15, scale = 2)
    private BigDecimal saldoDestinoDepois;

    /** Razão do erro em caso de FALHA. Nulo quando CONCLUIDA. */
    @Column(name = "motivo_falha", length = 500)
    private String motivoFalha;

    @CreationTimestamp
    @Column(name = "iniciada_em", updatable = false)
    private LocalDateTime iniciadaEm;

    @Column(name = "finalizada_em")
    private LocalDateTime finalizadaEm;
}
