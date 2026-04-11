package com.bip.beneficio.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados para transferência de valor entre benefícios")
public class TransferenciaDTO {

    @NotNull(message = "ID do benefício de origem é obrigatório")
    @Schema(description = "ID do benefício de origem (débito)", example = "1", required = true)
    private Long origemId;

    @NotNull(message = "ID do benefício de destino é obrigatório")
    @Schema(description = "ID do benefício de destino (crédito)", example = "2", required = true)
    private Long destinoId;

    @NotNull(message = "Valor da transferência é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor da transferência deve ser maior que zero")
    @Digits(integer = 13, fraction = 2, message = "Valor deve ter no máximo 13 dígitos inteiros e 2 decimais")
    @Schema(description = "Valor a ser transferido", example = "100.00", required = true)
    private BigDecimal valor;
}
