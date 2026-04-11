package com.bip.beneficio.api.controller;

import com.bip.beneficio.api.dto.ApiErrorResponse;
import com.bip.beneficio.api.dto.TransferenciaAuditoriaDTO;
import com.bip.beneficio.api.dto.TransferenciaDTO;
import com.bip.beneficio.api.dto.TransferenciaResultadoDTO;
import com.bip.beneficio.domain.service.TransferenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/transferencias")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transferências", description = "Auditoria e histórico de transferências entre benefícios")
public class TransferenciaController {

    private final TransferenciaService transferenciaService;

    @PostMapping
    @Operation(summary = "Realizar transferência",
            description = "Transfere valor entre dois benefícios com rastreamento completo de auditoria e suporte a idempotência via correlacaoId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transferência realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Benefício não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "correlacaoId já utilizado (use GET para consultar resultado)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente ou regra violada",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TransferenciaResultadoDTO> transferir(
            @Valid @RequestBody TransferenciaDTO dto) {
        return ResponseEntity.ok(transferenciaService.transferir(dto));
    }

    @GetMapping
    @Operation(summary = "Listar todas as transferências",
            description = "Retorna histórico paginado de todas as transferências (auditoria)")
    @ApiResponse(responseCode = "200", description = "Listagem realizada com sucesso")
    public ResponseEntity<Page<TransferenciaAuditoriaDTO>> listarTodas(
            @PageableDefault(size = 20, sort = "iniciadaEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(transferenciaService.listarTodas(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar transferência por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transferência encontrada"),
            @ApiResponse(responseCode = "404", description = "Transferência não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TransferenciaAuditoriaDTO> buscarPorId(
            @Parameter(description = "ID da transferência") @PathVariable Long id) {
        return ResponseEntity.ok(transferenciaService.buscarPorId(id));
    }

    @GetMapping("/correlacao/{correlacaoId}")
    @Operation(summary = "Buscar transferência por chave de correlação (idempotência)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transferência encontrada"),
            @ApiResponse(responseCode = "404", description = "Transferência não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TransferenciaAuditoriaDTO> buscarPorCorrelacaoId(
            @Parameter(description = "Chave de correlação UUID") @PathVariable String correlacaoId) {
        return ResponseEntity.ok(transferenciaService.buscarPorCorrelacaoId(correlacaoId));
    }
}
