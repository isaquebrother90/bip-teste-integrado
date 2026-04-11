package com.bip.beneficio.api.controller;

import com.bip.beneficio.api.dto.*;
import com.bip.beneficio.domain.service.BeneficioService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/v1/beneficios")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Benefícios", description = "API para gerenciamento de benefícios")
public class BeneficioController {

    private final BeneficioService service;

    @GetMapping
    @Operation(summary = "Listar benefícios", description = "Retorna lista paginada de benefícios")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listagem realizada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Page<BeneficioDTO>> listarTodos(
            @Parameter(description = "Filtrar apenas ativos")
            @RequestParam(required = false, defaultValue = "false") boolean apenasAtivos,
            @PageableDefault(size = 10, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {

        log.debug("GET /v1/beneficios - apenasAtivos={}, page={}", apenasAtivos, pageable);

        Page<BeneficioDTO> resultado = apenasAtivos
                ? service.listarAtivos(pageable)
                : service.listarTodos(pageable);

        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar benefício por ID", description = "Retorna os detalhes de um benefício específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Benefício encontrado"),
            @ApiResponse(responseCode = "404", description = "Benefício não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<BeneficioDTO> buscarPorId(
            @Parameter(description = "ID do benefício", required = true)
            @PathVariable Long id) {

        log.debug("GET /v1/beneficios/{}", id);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/buscar")
    @Operation(summary = "Buscar benefícios por nome", description = "Busca benefícios pelo nome (parcial, case-insensitive)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso")
    })
    public ResponseEntity<Page<BeneficioDTO>> buscarPorNome(
            @Parameter(description = "Nome ou parte do nome", required = true)
            @RequestParam String nome,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable) {

        log.debug("GET /v1/beneficios/buscar - nome={}", nome);
        return ResponseEntity.ok(service.buscarPorNome(nome, pageable));
    }

    @PostMapping
    @Operation(summary = "Criar benefício", description = "Cria um novo benefício")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Benefício criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Benefício com mesmo nome já existe",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<BeneficioDTO> criar(
            @Parameter(description = "Dados do benefício", required = true)
            @Valid @RequestBody BeneficioCreateDTO dto) {

        log.debug("POST /v1/beneficios - {}", dto);
        BeneficioDTO criado = service.criar(dto);
        URI location = URI.create("/v1/beneficios/" + criado.getId());
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar benefício", description = "Atualiza um benefício existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Benefício atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Benefício não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflito de concorrência ou nome duplicado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<BeneficioDTO> atualizar(
            @Parameter(description = "ID do benefício", required = true)
            @PathVariable Long id,
            @Parameter(description = "Dados atualizados", required = true)
            @Valid @RequestBody BeneficioUpdateDTO dto) {

        log.debug("PUT /v1/beneficios/{} - {}", id, dto);
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover benefício", description = "Remove um benefício pelo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Benefício removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Benefício não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> remover(
            @Parameter(description = "ID do benefício", required = true)
            @PathVariable Long id) {

        log.debug("DELETE /v1/beneficios/{}", id);
        service.remover(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ativar")
    @Operation(summary = "Ativar benefício", description = "Ativa um benefício inativo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Benefício ativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Benefício não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<BeneficioDTO> ativar(
            @Parameter(description = "ID do benefício", required = true)
            @PathVariable Long id) {

        log.debug("PATCH /v1/beneficios/{}/ativar", id);
        return ResponseEntity.ok(service.ativar(id));
    }

    @PatchMapping("/{id}/desativar")
    @Operation(summary = "Desativar benefício", description = "Desativa um benefício ativo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Benefício desativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Benefício não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<BeneficioDTO> desativar(
            @Parameter(description = "ID do benefício", required = true)
            @PathVariable Long id) {

        log.debug("PATCH /v1/beneficios/{}/desativar", id);
        return ResponseEntity.ok(service.desativar(id));
    }

    @PostMapping("/transferir")
    @Operation(summary = "Transferir valor entre benefícios",
            description = "Realiza transferência atômica de valor entre dois benefícios com validação de saldo e locking")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transferência realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Benefício de origem ou destino não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflito de concorrência",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente ou regra de negócio violada",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<TransferenciaResultadoDTO> transferir(
            @Parameter(description = "Dados da transferência", required = true)
            @Valid @RequestBody TransferenciaDTO dto) {

        log.debug("POST /v1/beneficios/transferir - origem={}, destino={}, valor={}",
                dto.getOrigemId(), dto.getDestinoId(), dto.getValor());
        return ResponseEntity.ok(service.transferir(dto));
    }
}
