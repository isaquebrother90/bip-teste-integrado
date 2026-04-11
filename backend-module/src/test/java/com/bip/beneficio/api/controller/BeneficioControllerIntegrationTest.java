package com.bip.beneficio.api.controller;

import com.bip.beneficio.api.dto.BeneficioCreateDTO;
import com.bip.beneficio.api.dto.BeneficioUpdateDTO;
import com.bip.beneficio.api.dto.TransferenciaDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para BeneficioController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("BeneficioController Integration Tests")
class BeneficioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/v1/beneficios";

    @Nested
    @DisplayName("GET /v1/beneficios")
    class ListarTests {

        @Test
        @DisplayName("Deve listar benefícios com paginação")
        void deveListarBeneficiosComPaginacao() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("page", "0")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", notNullValue()))
                    .andExpect(jsonPath("$.pageable", notNullValue()))
                    .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(0)));
        }

        @Test
        @DisplayName("Deve filtrar apenas benefícios ativos")
        void deveFiltrarApenasAtivos() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("apenasAtivos", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].ativo", everyItem(is(true))));
        }
    }

    @Nested
    @DisplayName("GET /v1/beneficios/{id}")
    class BuscarPorIdTests {

        @Test
        @DisplayName("Deve buscar benefício por ID com sucesso")
        void deveBuscarBeneficioPorIdComSucesso() throws Exception {
            // Primeiro criar um benefício
            BeneficioCreateDTO createDTO = BeneficioCreateDTO.builder()
                    .nome("Benefício para Busca")
                    .descricao("Descrição")
                    .valor(new BigDecimal("500.00"))
                    .ativo(true)
                    .build();

            String responseCreate = mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long id = objectMapper.readTree(responseCreate).get("id").asLong();

            // Agora buscar o benefício criado
            mockMvc.perform(get(BASE_URL + "/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(id.intValue())))
                    .andExpect(jsonPath("$.nome", is("Benefício para Busca")))
                    .andExpect(jsonPath("$.valor", is(500.00)));
        }

        @Test
        @DisplayName("Deve retornar 404 para ID inexistente")
        void deveRetornar404ParaIdInexistente() throws Exception {
            mockMvc.perform(get(BASE_URL + "/99999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
        }
    }

    @Nested
    @DisplayName("GET /v1/beneficios/buscar")
    class BuscarPorNomeTests {

        @Test
        @DisplayName("Deve buscar benefícios por nome com sucesso")
        void deveBuscarBeneficiosPorNomeComSucesso() throws Exception {
            // Criar benefícios para busca
            BeneficioCreateDTO dto1 = BeneficioCreateDTO.builder()
                    .nome("Vale Alimentação Premium")
                    .valor(new BigDecimal("800.00"))
                    .build();

            BeneficioCreateDTO dto2 = BeneficioCreateDTO.builder()
                    .nome("Vale Transporte")
                    .valor(new BigDecimal("200.00"))
                    .build();

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto1)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto2)))
                    .andExpect(status().isCreated());

            // Buscar por "Vale"
            mockMvc.perform(get(BASE_URL + "/buscar")
                            .param("nome", "Vale"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", notNullValue()))
                    .andExpect(jsonPath("$.content.length()", greaterThanOrEqualTo(2)));
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não encontrar")
        void deveRetornarListaVaziaQuandoNaoEncontrar() throws Exception {
            mockMvc.perform(get(BASE_URL + "/buscar")
                            .param("nome", "NomeInexistenteXYZ123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", notNullValue()))
                    .andExpect(jsonPath("$.content.length()", is(0)));
        }
    }

    @Nested
    @DisplayName("POST /v1/beneficios")
    class CriarTests {

        @Test
        @DisplayName("Deve criar benefício com sucesso")
        void deveCriarBeneficioComSucesso() throws Exception {
            BeneficioCreateDTO dto = BeneficioCreateDTO.builder()
                    .nome("Novo Benefício Teste")
                    .descricao("Descrição do teste")
                    .valor(new BigDecimal("750.00"))
                    .ativo(true)
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.nome", is("Novo Benefício Teste")))
                    .andExpect(jsonPath("$.valor", is(750.00)))
                    .andExpect(header().exists("Location"));
        }

        @Test
        @DisplayName("Deve retornar 400 para dados inválidos")
        void deveRetornar400ParaDadosInvalidos() throws Exception {
            BeneficioCreateDTO dto = BeneficioCreateDTO.builder()
                    .nome("")  // Nome vazio - inválido
                    .valor(new BigDecimal("-100.00"))  // Valor negativo - inválido
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")))
                    .andExpect(jsonPath("$.fieldErrors", notNullValue()));
        }
    }

    @Nested
    @DisplayName("PUT /v1/beneficios/{id}")
    class AtualizarTests {

        @Test
        @DisplayName("Deve atualizar benefício com sucesso")
        void deveAtualizarBeneficioComSucesso() throws Exception {
            // Criar benefício
            BeneficioCreateDTO createDTO = BeneficioCreateDTO.builder()
                    .nome("Benefício Original")
                    .descricao("Descrição original")
                    .valor(new BigDecimal("500.00"))
                    .ativo(true)
                    .build();

            String responseCreate = mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long id = objectMapper.readTree(responseCreate).get("id").asLong();
            Long version = objectMapper.readTree(responseCreate).get("version").asLong();

            // Atualizar benefício
            BeneficioUpdateDTO updateDTO = BeneficioUpdateDTO.builder()
                    .nome("Benefício Atualizado")
                    .descricao("Descrição atualizada")
                    .valor(new BigDecimal("750.00"))
                    .ativo(true)
                    .version(version)
                    .build();

            mockMvc.perform(put(BASE_URL + "/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(id.intValue())))
                    .andExpect(jsonPath("$.nome", is("Benefício Atualizado")))
                    .andExpect(jsonPath("$.descricao", is("Descrição atualizada")))
                    .andExpect(jsonPath("$.valor", is(750.00)));
        }

        @Test
        @DisplayName("Deve retornar 404 ao atualizar benefício inexistente")
        void deveRetornar404AoAtualizarBeneficioInexistente() throws Exception {
            BeneficioUpdateDTO updateDTO = BeneficioUpdateDTO.builder()
                    .nome("Benefício")
                    .valor(new BigDecimal("500.00"))
                    .ativo(true)
                    .version(0L)
                    .build();

            mockMvc.perform(put(BASE_URL + "/99999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
        }

        @Test
        @DisplayName("Deve retornar 400 para dados inválidos na atualização")
        void deveRetornar400ParaDadosInvalidosNaAtualizacao() throws Exception {
            BeneficioUpdateDTO updateDTO = BeneficioUpdateDTO.builder()
                    .nome("") // Nome vazio - inválido
                    .valor(new BigDecimal("-100.00")) // Valor negativo - inválido
                    .ativo(true)
                    .version(0L)
                    .build();

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
        }
    }

    @Nested
    @DisplayName("PATCH /v1/beneficios/{id}/ativar")
    class AtivarTests {

        @Test
        @DisplayName("Deve ativar benefício com sucesso")
        void deveAtivarBeneficioComSucesso() throws Exception {
            // Criar benefício inativo
            BeneficioCreateDTO createDTO = BeneficioCreateDTO.builder()
                    .nome("Benefício Inativo")
                    .valor(new BigDecimal("500.00"))
                    .ativo(false)
                    .build();

            String responseCreate = mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long id = objectMapper.readTree(responseCreate).get("id").asLong();

            // Ativar benefício
            mockMvc.perform(patch(BASE_URL + "/" + id + "/ativar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(id.intValue())))
                    .andExpect(jsonPath("$.ativo", is(true)));
        }

        @Test
        @DisplayName("Deve retornar 404 ao ativar benefício inexistente")
        void deveRetornar404AoAtivarBeneficioInexistente() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/99999/ativar"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
        }
    }

    @Nested
    @DisplayName("PATCH /v1/beneficios/{id}/desativar")
    class DesativarTests {

        @Test
        @DisplayName("Deve desativar benefício com sucesso")
        void deveDesativarBeneficioComSucesso() throws Exception {
            // Criar benefício ativo
            BeneficioCreateDTO createDTO = BeneficioCreateDTO.builder()
                    .nome("Benefício Ativo")
                    .valor(new BigDecimal("500.00"))
                    .ativo(true)
                    .build();

            String responseCreate = mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long id = objectMapper.readTree(responseCreate).get("id").asLong();

            // Desativar benefício
            mockMvc.perform(patch(BASE_URL + "/" + id + "/desativar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(id.intValue())))
                    .andExpect(jsonPath("$.ativo", is(false)));
        }

        @Test
        @DisplayName("Deve retornar 404 ao desativar benefício inexistente")
        void deveRetornar404AoDesativarBeneficioInexistente() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/99999/desativar"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
        }
    }

    @Nested
    @DisplayName("POST /v1/beneficios/transferir")
    class TransferirTests {

        @Test
        @DisplayName("Deve transferir valor entre benefícios com sucesso")
        void deveTransferirValorEntreBeneficiosComSucesso() throws Exception {
            // Criar benefício origem
            BeneficioCreateDTO origemDTO = BeneficioCreateDTO.builder()
                    .nome("Origem Transferência")
                    .valor(new BigDecimal("1000.00"))
                    .ativo(true)
                    .build();

            String origemResponse = mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(origemDTO)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long origemId = objectMapper.readTree(origemResponse).get("id").asLong();

            // Criar benefício destino
            BeneficioCreateDTO destinoDTO = BeneficioCreateDTO.builder()
                    .nome("Destino Transferência")
                    .valor(new BigDecimal("500.00"))
                    .ativo(true)
                    .build();

            String destinoResponse = mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(destinoDTO)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long destinoId = objectMapper.readTree(destinoResponse).get("id").asLong();

            // Realizar transferência
            TransferenciaDTO transferenciaDTO = TransferenciaDTO.builder()
                    .origemId(origemId)
                    .destinoId(destinoId)
                    .valor(new BigDecimal("300.00"))
                    .build();

            mockMvc.perform(post(BASE_URL + "/transferir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(transferenciaDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sucesso", is(true)))
                    .andExpect(jsonPath("$.saldoOrigem", is(700.00)))
                    .andExpect(jsonPath("$.saldoDestino", is(800.00)));
        }


        @Test
        @DisplayName("Deve retornar 422 para origem igual a destino")
        void deveRetornar422ParaOrigemIgualDestino() throws Exception {
            TransferenciaDTO dto = TransferenciaDTO.builder()
                    .origemId(1L)
                    .destinoId(1L)
                    .valor(new BigDecimal("100.00"))
                    .build();

            mockMvc.perform(post(BASE_URL + "/transferir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.errorCode", is("BUSINESS_ERROR")));
        }

        @Test
        @DisplayName("Deve retornar 400 para valor zero")
        void deveRetornar400ParaValorZero() throws Exception {
            TransferenciaDTO dto = TransferenciaDTO.builder()
                    .origemId(1L)
                    .destinoId(2L)
                    .valor(BigDecimal.ZERO)
                    .build();

            mockMvc.perform(post(BASE_URL + "/transferir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
        }

        @Test
        @DisplayName("Deve retornar 404 para benefício inexistente")
        void deveRetornar404ParaBeneficioInexistente() throws Exception {
            TransferenciaDTO dto = TransferenciaDTO.builder()
                    .origemId(99999L)
                    .destinoId(99998L)
                    .valor(new BigDecimal("100.00"))
                    .build();

            mockMvc.perform(post(BASE_URL + "/transferir")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /v1/beneficios/{id}")
    class RemoverTests {

        @Test
        @DisplayName("Deve remover benefício com sucesso")
        void deveRemoverBeneficioComSucesso() throws Exception {
            // Criar benefício
            BeneficioCreateDTO createDTO = BeneficioCreateDTO.builder()
                    .nome("Benefício para Remover")
                    .valor(new BigDecimal("500.00"))
                    .ativo(true)
                    .build();

            String responseCreate = mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDTO)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long id = objectMapper.readTree(responseCreate).get("id").asLong();

            // Remover benefício
            mockMvc.perform(delete(BASE_URL + "/" + id))
                    .andExpect(status().isNoContent());

            // Verificar se foi removido
            mockMvc.perform(get(BASE_URL + "/" + id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 404 ao tentar remover ID inexistente")
        void deveRetornar404AoRemoverIdInexistente() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/99999"))
                    .andExpect(status().isNotFound());
        }
    }
}
