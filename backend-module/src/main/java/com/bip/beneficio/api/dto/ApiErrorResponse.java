package com.bip.beneficio.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resposta de erro padronizada")
public class ApiErrorResponse {

    @Schema(description = "Timestamp do erro")
    private LocalDateTime timestamp;

    @Schema(description = "Código HTTP do erro", example = "400")
    private int status;

    @Schema(description = "Nome do erro HTTP", example = "Bad Request")
    private String error;

    @Schema(description = "Código de erro interno", example = "VALIDATION_ERROR")
    private String errorCode;

    @Schema(description = "Mensagem descritiva do erro")
    private String message;

    @Schema(description = "Caminho da requisição", example = "/api/v1/beneficios")
    private String path;

    @Schema(description = "Detalhes adicionais de validação")
    private List<FieldErrorDetail> fieldErrors;

    @Schema(description = "Detalhes adicionais do erro")
    private Map<String, Object> details;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Detalhe de erro de validação de campo")
    public static class FieldErrorDetail {

        @Schema(description = "Nome do campo com erro", example = "nome")
        private String field;

        @Schema(description = "Valor rejeitado", example = "")
        private Object rejectedValue;

        @Schema(description = "Mensagem de erro", example = "Nome é obrigatório")
        private String message;
    }
}
