package com.bip.beneficio.config;

import com.bip.beneficio.domain.repository.TransferenciaRepository;
import com.bip.beneficio.domain.entity.TransferenciaStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração de métricas de negócio com Micrometer.
 *
 * <p>Métricas registradas:</p>
 * <ul>
 *   <li>{@code transferencias.total} (counter) — registrado em cada chamada ao TransferenciaService,
 *       com tag {@code status=sucesso|falha}</li>
 *   <li>{@code transferencias.concluidas.total} (gauge) — total acumulado de transferências concluídas</li>
 * </ul>
 *
 * <p>Acesse em: {@code GET /actuator/metrics/transferencias.total}</p>
 * <p>Prometheus: {@code GET /actuator/prometheus}</p>
 */
@Configuration
@RequiredArgsConstructor
public class MetricasConfig {

    private final MeterRegistry meterRegistry;
    private final TransferenciaRepository transferenciaRepository;

    @PostConstruct
    public void registrarMetricas() {
        Gauge.builder("transferencias.concluidas.total",
                        transferenciaRepository,
                        repo -> repo.findByStatus(TransferenciaStatus.CONCLUIDA,
                                org.springframework.data.domain.Pageable.unpaged()).getTotalElements())
                .description("Total acumulado de transferências concluídas com sucesso")
                .register(meterRegistry);

        Gauge.builder("transferencias.falhas.total",
                        transferenciaRepository,
                        repo -> repo.findByStatus(TransferenciaStatus.FALHA,
                                org.springframework.data.domain.Pageable.unpaged()).getTotalElements())
                .description("Total acumulado de transferências com falha")
                .register(meterRegistry);
    }
}
