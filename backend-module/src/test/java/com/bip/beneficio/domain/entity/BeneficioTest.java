package com.bip.beneficio.domain.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para a entidade Beneficio.
 */
@DisplayName("Beneficio Entity Tests")
class BeneficioTest {

    private Beneficio beneficio;

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
    }

    @Nested
    @DisplayName("Testes de validação de saldo")
    class ValidacaoSaldoTests {

        @Test
        @DisplayName("Deve retornar true quando saldo é suficiente")
        void deveRetornarTrueQuandoSaldoSuficiente() {
            assertTrue(beneficio.possuiSaldoSuficiente(new BigDecimal("500.00")));
        }

        @Test
        @DisplayName("Deve retornar true quando saldo é igual ao montante")
        void deveRetornarTrueQuandoSaldoIgualMontante() {
            assertTrue(beneficio.possuiSaldoSuficiente(new BigDecimal("1000.00")));
        }

        @Test
        @DisplayName("Deve retornar false quando saldo é insuficiente")
        void deveRetornarFalseQuandoSaldoInsuficiente() {
            assertFalse(beneficio.possuiSaldoSuficiente(new BigDecimal("1500.00")));
        }

        @Test
        @DisplayName("Deve retornar false quando montante é nulo")
        void deveRetornarFalseQuandoMontanteNulo() {
            assertFalse(beneficio.possuiSaldoSuficiente(null));
        }
    }

    @Nested
    @DisplayName("Testes de débito")
    class DebitoTests {

        @Test
        @DisplayName("Deve debitar valor corretamente")
        void deveDebitarValorCorretamente() {
            beneficio.debitar(new BigDecimal("300.00"));
            assertEquals(new BigDecimal("700.00"), beneficio.getValor());
        }

        @Test
        @DisplayName("Deve debitar todo o saldo")
        void deveDebitarTodoSaldo() {
            beneficio.debitar(new BigDecimal("1000.00"));
            assertEquals(BigDecimal.ZERO.setScale(2), beneficio.getValor());
        }

        @Test
        @DisplayName("Deve lançar exceção quando saldo insuficiente")
        void deveLancarExcecaoQuandoSaldoInsuficiente() {
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> beneficio.debitar(new BigDecimal("1500.00"))
            );
            assertTrue(exception.getMessage().contains("Saldo insuficiente"));
        }

        @Test
        @DisplayName("Deve lançar exceção quando montante é nulo")
        void deveLancarExcecaoQuandoMontanteNulo() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> beneficio.debitar(null)
            );
        }

        @Test
        @DisplayName("Deve lançar exceção quando montante é zero")
        void deveLancarExcecaoQuandoMontanteZero() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> beneficio.debitar(BigDecimal.ZERO)
            );
        }

        @Test
        @DisplayName("Deve lançar exceção quando montante é negativo")
        void deveLancarExcecaoQuandoMontanteNegativo() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> beneficio.debitar(new BigDecimal("-100.00"))
            );
        }
    }

    @Nested
    @DisplayName("Testes de crédito")
    class CreditoTests {

        @Test
        @DisplayName("Deve creditar valor corretamente")
        void deveCreditarValorCorretamente() {
            beneficio.creditar(new BigDecimal("500.00"));
            assertEquals(new BigDecimal("1500.00"), beneficio.getValor());
        }

        @Test
        @DisplayName("Deve lançar exceção quando montante é nulo")
        void deveLancarExcecaoQuandoMontanteNulo() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> beneficio.creditar(null)
            );
        }

        @Test
        @DisplayName("Deve lançar exceção quando montante é zero")
        void deveLancarExcecaoQuandoMontanteZero() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> beneficio.creditar(BigDecimal.ZERO)
            );
        }
    }

    @Nested
    @DisplayName("Testes de ativação/desativação")
    class AtivacaoTests {

        @Test
        @DisplayName("Deve ativar benefício")
        void deveAtivarBeneficio() {
            beneficio.setAtivo(false);
            beneficio.ativar();
            assertTrue(beneficio.getAtivo());
        }

        @Test
        @DisplayName("Deve desativar benefício")
        void deveDesativarBeneficio() {
            beneficio.desativar();
            assertFalse(beneficio.getAtivo());
        }
    }
}
