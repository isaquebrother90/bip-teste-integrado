package com.bip.beneficio.domain.service;

import com.bip.beneficio.api.dto.*;
import com.bip.beneficio.api.mapper.BeneficioMapper;
import com.bip.beneficio.domain.entity.Beneficio;
import com.bip.beneficio.domain.exception.*;
import com.bip.beneficio.domain.repository.BeneficioRepository;
import com.bip.beneficio.domain.service.impl.BeneficioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para BeneficioService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BeneficioService Tests")
class BeneficioServiceTest {

    @Mock
    private BeneficioRepository repository;

    @Mock
    private BeneficioMapper mapper;

    @InjectMocks
    private BeneficioServiceImpl service;

    private Beneficio beneficio;
    private BeneficioDTO beneficioDTO;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        beneficio = Beneficio.builder()
                .id(1L)
                .nome("Vale Alimentação")
                .descricao("Benefício para alimentação")
                .valor(new BigDecimal("1000.00"))
                .ativo(true)
                .version(0L)
                .build();

        beneficioDTO = BeneficioDTO.builder()
                .id(1L)
                .nome("Vale Alimentação")
                .descricao("Benefício para alimentação")
                .valor(new BigDecimal("1000.00"))
                .ativo(true)
                .version(0L)
                .build();

        pageable = PageRequest.of(0, 10);
    }

    @Nested
    @DisplayName("Testes de busca")
    class BuscaTests {

        @Test
        @DisplayName("Deve listar todos os benefícios")
        void deveListarTodosBeneficios() {
            Page<Beneficio> page = new PageImpl<>(List.of(beneficio));
            when(repository.findAll(pageable)).thenReturn(page);
            when(mapper.toDTO(any(Beneficio.class))).thenReturn(beneficioDTO);

            Page<BeneficioDTO> resultado = service.listarTodos(pageable);

            assertNotNull(resultado);
            assertEquals(1, resultado.getTotalElements());
            verify(repository).findAll(pageable);
        }

        @Test
        @DisplayName("Deve listar apenas benefícios ativos")
        void deveListarApenasBeneficiosAtivos() {
            Page<Beneficio> page = new PageImpl<>(List.of(beneficio));
            when(repository.findByAtivoTrue(pageable)).thenReturn(page);
            when(mapper.toDTO(any(Beneficio.class))).thenReturn(beneficioDTO);

            Page<BeneficioDTO> resultado = service.listarAtivos(pageable);

            assertNotNull(resultado);
            assertEquals(1, resultado.getTotalElements());
            verify(repository).findByAtivoTrue(pageable);
        }

        @Test
        @DisplayName("Deve buscar benefício por ID")
        void deveBuscarPorId() {
            when(repository.findById(1L)).thenReturn(Optional.of(beneficio));
            when(mapper.toDTO(beneficio)).thenReturn(beneficioDTO);

            BeneficioDTO resultado = service.buscarPorId(1L);

            assertNotNull(resultado);
            assertEquals(1L, resultado.getId());
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção quando benefício não encontrado")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.buscarPorId(99L));
        }

        @Test
        @DisplayName("Deve buscar benefícios por nome")
        void deveBuscarBeneficiosPorNome() {
            Page<Beneficio> page = new PageImpl<>(List.of(beneficio));
            when(repository.findByNomeContainingIgnoreCase("Vale", pageable)).thenReturn(page);
            when(mapper.toDTO(any(Beneficio.class))).thenReturn(beneficioDTO);

            Page<BeneficioDTO> resultado = service.buscarPorNome("Vale", pageable);

            assertNotNull(resultado);
            assertEquals(1, resultado.getTotalElements());
            verify(repository).findByNomeContainingIgnoreCase("Vale", pageable);
        }
    }

    @Nested
    @DisplayName("Testes de criação")
    class CriacaoTests {

        @Test
        @DisplayName("Deve criar benefício com sucesso")
        void deveCriarBeneficioComSucesso() {
            BeneficioCreateDTO createDTO = BeneficioCreateDTO.builder()
                    .nome("Novo Benefício")
                    .descricao("Descrição")
                    .valor(new BigDecimal("500.00"))
                    .ativo(true)
                    .build();

            when(repository.existsByNomeIgnoreCase(anyString())).thenReturn(false);
            when(mapper.toEntity(createDTO)).thenReturn(beneficio);
            when(repository.save(any(Beneficio.class))).thenReturn(beneficio);
            when(mapper.toDTO(beneficio)).thenReturn(beneficioDTO);

            BeneficioDTO resultado = service.criar(createDTO);

            assertNotNull(resultado);
            verify(repository).save(any(Beneficio.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando nome duplicado")
        void deveLancarExcecaoQuandoNomeDuplicado() {
            BeneficioCreateDTO createDTO = BeneficioCreateDTO.builder()
                    .nome("Nome Existente")
                    .valor(new BigDecimal("500.00"))
                    .build();

            when(repository.existsByNomeIgnoreCase(anyString())).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () -> service.criar(createDTO));
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Testes de atualização")
    class AtualizacaoTests {

        @Test
        @DisplayName("Deve atualizar benefício com sucesso")
        void deveAtualizarBeneficioComSucesso() {
            BeneficioUpdateDTO updateDTO = BeneficioUpdateDTO.builder()
                    .nome("Vale Alimentação Atualizado")
                    .descricao("Nova descrição")
                    .valor(new BigDecimal("1200.00"))
                    .ativo(true)
                    .version(0L)
                    .build();

            when(repository.findById(1L)).thenReturn(Optional.of(beneficio));
            when(repository.existsByNomeIgnoreCaseAndIdNot(anyString(), anyLong())).thenReturn(false);
            when(repository.save(any(Beneficio.class))).thenAnswer(i -> i.getArgument(0));
            when(mapper.toDTO(any(Beneficio.class))).thenReturn(beneficioDTO);

            BeneficioDTO resultado = service.atualizar(1L, updateDTO);

            assertNotNull(resultado);
            verify(repository).save(any(Beneficio.class));
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar benefício inexistente")
        void deveLancarExcecaoAoAtualizarBeneficioInexistente() {
            BeneficioUpdateDTO updateDTO = BeneficioUpdateDTO.builder()
                    .nome("Nome")
                    .valor(new BigDecimal("500.00"))
                    .ativo(true)
                    .version(0L)
                    .build();

            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.atualizar(99L, updateDTO));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar com nome duplicado")
        void deveLancarExcecaoAoAtualizarComNomeDuplicado() {
            BeneficioUpdateDTO updateDTO = BeneficioUpdateDTO.builder()
                    .nome("Nome Existente")
                    .valor(new BigDecimal("500.00"))
                    .ativo(true)
                    .version(0L)
                    .build();

            when(repository.findById(1L)).thenReturn(Optional.of(beneficio));
            when(repository.existsByNomeIgnoreCaseAndIdNot("Nome Existente", 1L)).thenReturn(true);

            assertThrows(DuplicateResourceException.class, () -> service.atualizar(1L, updateDTO));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção em conflito de versão")
        void deveLancarExcecaoEmConflitoDeVersao() {
            BeneficioUpdateDTO updateDTO = BeneficioUpdateDTO.builder()
                    .nome("Vale Alimentação")
                    .valor(new BigDecimal("1200.00"))
                    .ativo(true)
                    .version(5L)  // Versão diferente da entidade (0L)
                    .build();

            when(repository.findById(1L)).thenReturn(Optional.of(beneficio));

            assertThrows(ConcurrencyException.class, () -> service.atualizar(1L, updateDTO));
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Testes de ativação e desativação")
    class AtivacaoTests {

        @Test
        @DisplayName("Deve ativar benefício com sucesso")
        void deveAtivarBeneficioComSucesso() {
            beneficio.setAtivo(false);

            when(repository.findById(1L)).thenReturn(Optional.of(beneficio));
            when(repository.save(any(Beneficio.class))).thenAnswer(i -> i.getArgument(0));
            when(mapper.toDTO(any(Beneficio.class))).thenReturn(beneficioDTO);

            BeneficioDTO resultado = service.ativar(1L);

            assertNotNull(resultado);
            verify(repository).save(any(Beneficio.class));
        }

        @Test
        @DisplayName("Deve lançar exceção ao ativar benefício inexistente")
        void deveLancarExcecaoAoAtivarBeneficioInexistente() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.ativar(99L));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve desativar benefício com sucesso")
        void deveDesativarBeneficioComSucesso() {
            when(repository.findById(1L)).thenReturn(Optional.of(beneficio));
            when(repository.save(any(Beneficio.class))).thenAnswer(i -> i.getArgument(0));
            when(mapper.toDTO(any(Beneficio.class))).thenReturn(beneficioDTO);

            BeneficioDTO resultado = service.desativar(1L);

            assertNotNull(resultado);
            verify(repository).save(any(Beneficio.class));
        }

        @Test
        @DisplayName("Deve lançar exceção ao desativar benefício inexistente")
        void deveLancarExcecaoAoDesativarBeneficioInexistente() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.desativar(99L));
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Testes de remoção")
    class RemocaoTests {

        @Test
        @DisplayName("Deve remover benefício com sucesso")
        void deveRemoverBeneficioComSucesso() {
            when(repository.existsById(1L)).thenReturn(true);
            doNothing().when(repository).deleteById(1L);

            service.remover(1L);

            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção ao remover benefício inexistente")
        void deveLancarExcecaoAoRemoverBeneficioInexistente() {
            when(repository.existsById(99L)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class, () -> service.remover(99L));
            verify(repository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Testes de transferência")
    class TransferenciaTests {

        private Beneficio origem;
        private Beneficio destino;

        @BeforeEach
        void setUp() {
            origem = Beneficio.builder()
                    .id(1L)
                    .nome("Origem")
                    .valor(new BigDecimal("1000.00"))
                    .version(0L)
                    .build();

            destino = Beneficio.builder()
                    .id(2L)
                    .nome("Destino")
                    .valor(new BigDecimal("500.00"))
                    .version(0L)
                    .build();
        }

        @Test
        @DisplayName("Deve realizar transferência com sucesso")
        void deveRealizarTransferenciaComSucesso() {
            TransferenciaDTO dto = TransferenciaDTO.builder()
                    .origemId(1L)
                    .destinoId(2L)
                    .valor(new BigDecimal("300.00"))
                    .build();

            when(repository.findByIdWithLock(1L)).thenReturn(Optional.of(origem));
            when(repository.findByIdWithLock(2L)).thenReturn(Optional.of(destino));
            when(repository.save(any(Beneficio.class))).thenAnswer(i -> i.getArgument(0));

            TransferenciaResultadoDTO resultado = service.transferir(dto);

            assertTrue(resultado.isSucesso());
            assertEquals(new BigDecimal("700.00"), resultado.getSaldoOrigem());
            assertEquals(new BigDecimal("800.00"), resultado.getSaldoDestino());
            verify(repository, times(2)).save(any(Beneficio.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando saldo insuficiente")
        void deveLancarExcecaoQuandoSaldoInsuficiente() {
            TransferenciaDTO dto = TransferenciaDTO.builder()
                    .origemId(1L)
                    .destinoId(2L)
                    .valor(new BigDecimal("2000.00"))
                    .build();

            when(repository.findByIdWithLock(1L)).thenReturn(Optional.of(origem));
            when(repository.findByIdWithLock(2L)).thenReturn(Optional.of(destino));

            assertThrows(InsufficientBalanceException.class, () -> service.transferir(dto));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando origem igual a destino")
        void deveLancarExcecaoQuandoOrigemIgualDestino() {
            TransferenciaDTO dto = TransferenciaDTO.builder()
                    .origemId(1L)
                    .destinoId(1L)
                    .valor(new BigDecimal("100.00"))
                    .build();

            assertThrows(BusinessException.class, () -> service.transferir(dto));
            verify(repository, never()).findByIdWithLock(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando origem não encontrada")
        void deveLancarExcecaoQuandoOrigemNaoEncontrada() {
            TransferenciaDTO dto = TransferenciaDTO.builder()
                    .origemId(99L)
                    .destinoId(2L)
                    .valor(new BigDecimal("100.00"))
                    .build();

            when(repository.findByIdWithLock(2L)).thenReturn(Optional.of(destino));
            when(repository.findByIdWithLock(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.transferir(dto));
        }
    }
}
