package com.bip.beneficio.api.controller;

import com.bip.beneficio.api.dto.BeneficioCreateDTO;
import com.bip.beneficio.api.dto.TransferenciaDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("TransferenciaController Integration Tests")
class TransferenciaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/v1/transferencias";
    private static final String BENEFICIOS_URL = "/v1/beneficios";

    private Long origemId;
    private Long destinoId;

    @BeforeEach
    void criarBeneficios() throws Exception {
        origemId = criarBeneficio("Origem Teste", new BigDecimal("1000.00"));
        destinoId = criarBeneficio("Destino Teste", new BigDecimal("500.00"));
    }

    private Long criarBeneficio(String nome, BigDecimal valor) throws Exception {
        BeneficioCreateDTO dto = BeneficioCreateDTO.builder()
                .nome(nome)
                .valor(valor)
                .ativo(true)
                .build();

        String response = mockMvc.perform(post(BENEFICIOS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    @Nested
    @DisplayName("POST /v1/transferencias")
    class TransferirTests {

        @Test
        @DisplayName("Deve realizar transferência com sucesso e retornar snapshots de saldo")
        void deveRealizarTransferenciaComSucesso() throws Exception {
            TransferenciaDTO dto = TransferenciaDTO.builder()
                    .origemId(origemId)
                    .destinoId(destinoId)
                    .valor(new BigDecimal("300.00"))
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sucesso").value(true))
                    .andExpect(jsonPath("$.saldoOrigem").value(700.00))
                    .andExpect(jsonPath("$.saldoDestino").value(800.00))
                    .andExpect(jsonPath("$.transferenciaId").isNumber())
                    .andExpect(jsonPath("$.correlacaoId").isNotEmpty());
        }

        @Test
        @DisplayName("Deve retornar resultado original sem reprocessar quando correlacaoId já foi concluído (idempotência)")
        void deveRetornarResultadoIdempotente() throws Exception {
            String correlacaoId = UUID.randomUUID().toString();

            TransferenciaDTO dto = TransferenciaDTO.builder()
                    .origemId(origemId)
                    .destinoId(destinoId)
                    .valor(new BigDecimal("200.00"))
                    .correlacaoId(correlacaoId)
                    .build();

            // Primeira chamada
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sucesso").value(true));

            // Segunda chamada com mesmo correlacaoId — deve retornar o mesmo resultado
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sucesso").value(true))
                    .andExpect(jsonPath("$.correlacaoId").value(correlacaoId));
        }

        @Test
        @DisplayName("Deve retornar 422 quando saldo insuficiente")
        void deveRetornar422QuandoSaldoInsuficiente() throws Exception {
            TransferenciaDTO dto = TransferenciaDTO.builder()
                    .origemId(origemId)
                    .destinoId(destinoId)
                    .valor(new BigDecimal("9999.00"))
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("Deve retornar 422 quando origem igual a destino")
        void deveRetornar422QuandoOrigemIgualDestino() throws Exception {
            TransferenciaDTO dto = TransferenciaDTO.builder()
                    .origemId(origemId)
                    .destinoId(origemId)
                    .valor(new BigDecimal("100.00"))
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("Deve retornar 404 quando benefício de origem não encontrado")
        void deveRetornar404QuandoOrigemNaoEncontrada() throws Exception {
            TransferenciaDTO dto = TransferenciaDTO.builder()
                    .origemId(99999L)
                    .destinoId(destinoId)
                    .valor(new BigDecimal("100.00"))
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /v1/transferencias")
    class ListarTests {

        @Test
        @DisplayName("Deve listar transferências paginadas")
        void deveListarTransferenciasPaginadas() throws Exception {
            // Realiza uma transferência para garantir que há dados
            TransferenciaDTO dto = TransferenciaDTO.builder()
                    .origemId(origemId)
                    .destinoId(destinoId)
                    .valor(new BigDecimal("100.00"))
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
        }
    }

    @Nested
    @DisplayName("GET /v1/transferencias/{id}")
    class BuscarPorIdTests {

        @Test
        @DisplayName("Deve buscar transferência por ID com sucesso")
        void deveBuscarPorIdComSucesso() throws Exception {
            TransferenciaDTO dto = TransferenciaDTO.builder()
                    .origemId(origemId)
                    .destinoId(destinoId)
                    .valor(new BigDecimal("150.00"))
                    .build();

            String response = mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            Long transferenciaId = objectMapper.readTree(response).get("transferenciaId").asLong();

            mockMvc.perform(get(BASE_URL + "/" + transferenciaId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(transferenciaId))
                    .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                    .andExpect(jsonPath("$.saldoOrigemAntes").value(1000.00))
                    .andExpect(jsonPath("$.saldoOrigemDepois").value(850.00));
        }

        @Test
        @DisplayName("Deve retornar 404 para ID inexistente")
        void deveRetornar404ParaIdInexistente() throws Exception {
            mockMvc.perform(get(BASE_URL + "/99999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /v1/transferencias/correlacao/{correlacaoId}")
    class BuscarPorCorrelacaoTests {

        @Test
        @DisplayName("Deve buscar transferência por correlacaoId com sucesso")
        void deveBuscarPorCorrelacaoIdComSucesso() throws Exception {
            String correlacaoId = UUID.randomUUID().toString();

            TransferenciaDTO dto = TransferenciaDTO.builder()
                    .origemId(origemId)
                    .destinoId(destinoId)
                    .valor(new BigDecimal("100.00"))
                    .correlacaoId(correlacaoId)
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());

            mockMvc.perform(get(BASE_URL + "/correlacao/" + correlacaoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.correlacaoId").value(correlacaoId))
                    .andExpect(jsonPath("$.status").value("CONCLUIDA"));
        }

        @Test
        @DisplayName("Deve retornar 404 para correlacaoId inexistente")
        void deveRetornar404ParaCorrelacaoIdInexistente() throws Exception {
            mockMvc.perform(get(BASE_URL + "/correlacao/" + UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }
}
