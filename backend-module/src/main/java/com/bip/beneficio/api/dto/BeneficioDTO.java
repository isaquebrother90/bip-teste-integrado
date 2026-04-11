package com.bip.beneficio.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados do benefício")
public class BeneficioDTO {

    @Schema(description = "Identificador único do benefício", example = "1")
    private Long id;

    @Schema(description = "Nome do benefício", example = "Vale Alimentação")
    private String nome;

    @Schema(description = "Descrição detalhada do benefício", example = "Benefício para alimentação diária")
    private String descricao;

    @Schema(description = "Valor atual do benefício", example = "800.00")
    private BigDecimal valor;

    @Schema(description = "Indica se o benefício está ativo", example = "true")
    private Boolean ativo;

    @Schema(description = "Versão do registro (controle de concorrência)", example = "0")
    private Long version;

    @Schema(description = "Data de criação do registro")
    private LocalDateTime criadoEm;

    @Schema(description = "Data da última atualização")
    private LocalDateTime atualizadoEm;
}
