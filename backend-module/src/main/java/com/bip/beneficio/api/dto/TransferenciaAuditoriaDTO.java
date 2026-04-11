package com.bip.beneficio.api.dto;

import com.bip.beneficio.domain.entity.TransferenciaStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Registro de auditoria de uma transferência entre benefícios")
public class TransferenciaAuditoriaDTO {

    @Schema(description = "ID único da transferência")
    private Long id;

    @Schema(description = "Chave de idempotência (correlação)")
    private String correlacaoId;

    @Schema(description = "ID do benefício de origem")
    private Long origemId;

    @Schema(description = "ID do benefício de destino")
    private Long destinoId;

    @Schema(description = "Valor transferido")
    private BigDecimal valor;

    @Schema(description = "Status atual da transferência")
    private TransferenciaStatus status;

    @Schema(description = "Saldo da origem antes da transferência")
    private BigDecimal saldoOrigemAntes;

    @Schema(description = "Saldo da origem após a transferência")
    private BigDecimal saldoOrigemDepois;

    @Schema(description = "Saldo do destino antes da transferência")
    private BigDecimal saldoDestinoAntes;

    @Schema(description = "Saldo do destino após a transferência")
    private BigDecimal saldoDestinoDepois;

    @Schema(description = "Motivo da falha (preenchido apenas quando status=FALHA)")
    private String motivoFalha;

    @Schema(description = "Data/hora de início da transferência")
    private LocalDateTime iniciadaEm;

    @Schema(description = "Data/hora de conclusão da transferência")
    private LocalDateTime finalizadaEm;
}
