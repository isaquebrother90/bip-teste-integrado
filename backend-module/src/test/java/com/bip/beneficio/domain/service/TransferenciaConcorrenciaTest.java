package com.bip.beneficio.domain.service;

import com.bip.beneficio.api.dto.TransferenciaDTO;
import com.bip.beneficio.api.dto.TransferenciaResultadoDTO;
import com.bip.beneficio.domain.entity.Beneficio;
import com.bip.beneficio.domain.repository.BeneficioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de concorrência para transferências entre benefícios.
 *
 * <p>Valida os invariantes de negócio sob carga concorrente:</p>
 * <ol>
 *   <li><b>Conservação de valor</b>: a soma total dos saldos deve ser igual antes e depois</li>
 *   <li><b>Saldo não-negativo</b>: nenhum benefício pode terminar com saldo negativo</li>
 *   <li><b>Ausência de deadlock</b>: transferências cruzadas (A→B e B→A) simultâneas não travam</li>
 * </ol>
 *
 * <p>Estes testes provam que o mecanismo de locking (pessimistic + ordenação por ID)
 * implementado no {@link TransferenciaService} é correto sob condições reais de concorrência.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class TransferenciaConcorrenciaTest {

    @Autowired
    private TransferenciaService transferenciaService;

    @Autowired
    private BeneficioRepository beneficioRepository;

    private Long idA;
    private Long idB;

    @BeforeEach
    void setUp() {
        beneficioRepository.deleteAll();

        Beneficio a = beneficioRepository.save(Beneficio.builder()
                .nome("Beneficio A")
                .valor(new BigDecimal("1000.00"))
                .ativo(true)
                .build());

        Beneficio b = beneficioRepository.save(Beneficio.builder()
                .nome("Beneficio B")
                .valor(new BigDecimal("1000.00"))
                .ativo(true)
                .build());

        idA = a.getId();
        idB = b.getId();
    }

    @Test
    @DisplayName("Invariante: soma dos saldos permanece constante após transferências concorrentes")
    void transferenciaConcorrente_deveManteroSaldoTotal() throws Exception {
        BigDecimal somaInicial = somarSaldos();
        int threads = 10;
        int transferenciasPerThread = 5;
        BigDecimal valorPorTransferencia = new BigDecimal("50.00");

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<Void>> futures = new ArrayList<>();
        AtomicInteger falhas = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                for (int j = 0; j < transferenciasPerThread; j++) {
                    // Metade das threads transfere A→B, metade B→A
                    Long origem = (idx % 2 == 0) ? idA : idB;
                    Long destino = (idx % 2 == 0) ? idB : idA;

                    try {
                        transferenciaService.transferir(TransferenciaDTO.builder()
                                .origemId(origem)
                                .destinoId(destino)
                                .valor(valorPorTransferencia)
                                .correlacaoId(UUID.randomUUID().toString())
                                .build());
                    } catch (Exception ex) {
                        falhas.incrementAndGet();
                    }
                }
                return null;
            }));
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        for (Future<Void> f : futures) {
            f.get(); // propaga exceptions não capturadas
        }

        BigDecimal somaFinal = somarSaldos();

        assertThat(somaFinal)
                .as("A soma total dos saldos deve ser conservada (falhas de saldo esperadas não alteram o total)")
                .isEqualByComparingTo(somaInicial);

        assertThat(beneficioRepository.findById(idA).orElseThrow().getValor())
                .as("Saldo do benefício A não pode ser negativo")
                .isGreaterThanOrEqualTo(BigDecimal.ZERO);

        assertThat(beneficioRepository.findById(idB).orElseThrow().getValor())
                .as("Saldo do benefício B não pode ser negativo")
                .isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Anti-deadlock: transferências cruzadas A→B e B→A simultâneas não causam deadlock")
    void transferenciaCruzada_naoDeveCausarDeadlock() throws Exception {
        int pares = 20;
        ExecutorService executor = Executors.newFixedThreadPool(pares * 2);
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < pares; i++) {
            // Thread 1: A → B
            futures.add(executor.submit(() -> {
                try {
                    transferenciaService.transferir(TransferenciaDTO.builder()
                            .origemId(idA).destinoId(idB)
                            .valor(new BigDecimal("10.00"))
                            .correlacaoId(UUID.randomUUID().toString())
                            .build());
                } catch (Exception ignored) { /* saldo insuficiente é esperado */ }
                return null;
            }));

            // Thread 2: B → A (cruzada simultânea — cenário clássico de deadlock)
            futures.add(executor.submit(() -> {
                try {
                    transferenciaService.transferir(TransferenciaDTO.builder()
                            .origemId(idB).destinoId(idA)
                            .valor(new BigDecimal("10.00"))
                            .correlacaoId(UUID.randomUUID().toString())
                            .build());
                } catch (Exception ignored) { /* saldo insuficiente é esperado */ }
                return null;
            }));
        }

        executor.shutdown();
        boolean terminou = executor.awaitTermination(15, TimeUnit.SECONDS);

        assertThat(terminou)
                .as("Nenhum deadlock detectado: todas as threads terminaram dentro do timeout")
                .isTrue();

        assertThat(beneficioRepository.findById(idA).orElseThrow().getValor())
                .isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(beneficioRepository.findById(idB).orElseThrow().getValor())
                .isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Idempotência: reenviar a mesma correlacaoId não duplica a transferência")
    void transferencia_comMesmaCorrelacaoId_deveSerIdempotente() {
        String correlacaoId = UUID.randomUUID().toString();
        BigDecimal saldoAAntes = beneficioRepository.findById(idA).orElseThrow().getValor();

        TransferenciaDTO dto = TransferenciaDTO.builder()
                .origemId(idA).destinoId(idB)
                .valor(new BigDecimal("100.00"))
                .correlacaoId(correlacaoId)
                .build();

        TransferenciaResultadoDTO r1 = transferenciaService.transferir(dto);
        TransferenciaResultadoDTO r2 = transferenciaService.transferir(dto); // reenvio

        BigDecimal saldoADepois = beneficioRepository.findById(idA).orElseThrow().getValor();

        assertThat(r1.getTransferenciaId()).isEqualTo(r2.getTransferenciaId());
        assertThat(saldoADepois)
                .as("Saldo deve ter sido debitado apenas uma vez (idempotência)")
                .isEqualByComparingTo(saldoAAntes.subtract(new BigDecimal("100.00")));
    }

    private BigDecimal somarSaldos() {
        return beneficioRepository.findById(idA).orElseThrow().getValor()
                .add(beneficioRepository.findById(idB).orElseThrow().getValor());
    }
}
