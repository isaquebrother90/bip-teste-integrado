package com.bip.beneficio.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados para atualização de benefício")
public class BeneficioUpdateDTO {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    @Schema(description = "Nome do benefício", example = "Vale Alimentação", required = true)
    private String nome;

    @Size(max = 255, message = "Descrição não pode exceder 255 caracteres")
    @Schema(description = "Descrição detalhada do benefício", example = "Benefício para alimentação diária")
    private String descricao;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.00", message = "Valor não pode ser negativo")
    @Digits(integer = 13, fraction = 2, message = "Valor deve ter no máximo 13 dígitos inteiros e 2 decimais")
    @Schema(description = "Valor do benefício", example = "800.00", required = true)
    private BigDecimal valor;

    @NotNull(message = "Status ativo é obrigatório")
    @Schema(description = "Indica se o benefício está ativo", example = "true", required = true)
    private Boolean ativo;

    @NotNull(message = "Versão é obrigatória para controle de concorrência")
    @Schema(description = "Versão atual do registro (Optimistic Locking)", example = "0", required = true)
    private Long version;
}
