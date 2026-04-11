package com.bip.beneficio.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Resultado da transferência entre benefícios")
public class TransferenciaResultadoDTO {

    @Schema(description = "Indica se a transferência foi bem sucedida")
    private boolean sucesso;

    @Schema(description = "Mensagem descritiva do resultado")
    private String mensagem;

    @Schema(description = "Valor transferido")
    private BigDecimal valorTransferido;

    @Schema(description = "Saldo atualizado do benefício de origem")
    private BigDecimal saldoOrigem;

    @Schema(description = "Saldo atualizado do benefício de destino")
    private BigDecimal saldoDestino;

    @Schema(description = "Data/hora da transferência")
    private LocalDateTime dataTransferencia;

    @Schema(description = "ID da transferência registrada (para consulta de auditoria)")
    private Long transferenciaId;

    @Schema(description = "Chave de correlação/idempotência")
    private String correlacaoId;
}
