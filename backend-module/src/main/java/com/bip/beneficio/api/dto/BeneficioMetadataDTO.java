package com.bip.beneficio.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * DTO de metadados de benefício — sem o campo {@code valor}.
 *
 * <p>Usado exclusivamente em endpoints cacheados. O campo {@code valor} (saldo)
 * é intencionalmente omitido para evitar exposição de dados financeiros desatualizados
 * via cache stale.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Metadados de benefício (sem saldo — use GET /{id} para dados financeiros atualizados)")
public class BeneficioMetadataDTO {

    @Schema(description = "ID do benefício")
    private Long id;

    @Schema(description = "Nome do benefício")
    private String nome;

    @Schema(description = "Descrição do benefício")
    private String descricao;

    @Schema(description = "Indica se o benefício está ativo")
    private Boolean ativo;
}
