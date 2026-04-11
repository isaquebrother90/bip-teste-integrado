package com.bip.beneficio.domain.service.impl;

import com.bip.beneficio.api.dto.TransferenciaAuditoriaDTO;
import com.bip.beneficio.api.dto.TransferenciaDTO;
import com.bip.beneficio.api.dto.TransferenciaResultadoDTO;
import com.bip.beneficio.domain.entity.Beneficio;
import com.bip.beneficio.domain.entity.Transferencia;
import com.bip.beneficio.domain.entity.TransferenciaStatus;
import com.bip.beneficio.domain.exception.*;
import com.bip.beneficio.domain.repository.BeneficioRepository;
import com.bip.beneficio.domain.repository.TransferenciaRepository;
import com.bip.beneficio.domain.service.TransferenciaService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TransferenciaServiceImpl implements TransferenciaService {

    private final BeneficioRepository beneficioRepository;
    private final TransferenciaRepository transferenciaRepository;
    private final MeterRegistry meterRegistry;

    private static final String BENEFICIO = "Benefício";
    private static final String ID = "id";

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransferenciaResultadoDTO transferir(TransferenciaDTO dto) {
        String correlacaoId = resolverCorrelacaoId(dto);

        log.info("Iniciando transferência: correlacao={}, origem={}, destino={}, valor={}",
                correlacaoId, dto.getOrigemId(), dto.getDestinoId(), dto.getValor());

        // Idempotência: retorna resultado anterior se já processado
        if (transferenciaRepository.existsByCorrelacaoId(correlacaoId)) {
            Transferencia existente = transferenciaRepository.findByCorrelacaoId(correlacaoId)
                    .orElseThrow();
            if (existente.getStatus() == TransferenciaStatus.CONCLUIDA) {
                log.info("Transferência idempotente retornada: correlacao={}", correlacaoId);
                return toResultadoDTO(existente);
            }
            throw new IdempotencyConflictException(correlacaoId);
        }

        // Regras de negócio básicas
        if (dto.getOrigemId().equals(dto.getDestinoId())) {
            throw new BusinessException("Origem e destino não podem ser iguais");
        }

        // Registra a transferência como PENDENTE antes de adquirir locks (rastreabilidade de falhas)
        Transferencia transferencia = Transferencia.builder()
                .correlacaoId(correlacaoId)
                .origemId(dto.getOrigemId())
                .destinoId(dto.getDestinoId())
                .valor(dto.getValor())
                .status(TransferenciaStatus.PENDENTE)
                .build();
        transferencia = transferenciaRepository.save(transferencia);

        try {
            transferencia.setStatus(TransferenciaStatus.PROCESSANDO);
            transferenciaRepository.save(transferencia);

            // Locks sempre na ordem crescente de ID para evitar deadlocks
            Long primeiroId = Math.min(dto.getOrigemId(), dto.getDestinoId());
            Long segundoId = Math.max(dto.getOrigemId(), dto.getDestinoId());

            Beneficio primeiro = beneficioRepository.findByIdWithLock(primeiroId)
                    .orElseThrow(() -> new ResourceNotFoundException(BENEFICIO, ID, primeiroId));
            Beneficio segundo = beneficioRepository.findByIdWithLock(segundoId)
                    .orElseThrow(() -> new ResourceNotFoundException(BENEFICIO, ID, segundoId));

            Beneficio origem = dto.getOrigemId().equals(primeiroId) ? primeiro : segundo;
            Beneficio destino = dto.getOrigemId().equals(primeiroId) ? segundo : primeiro;

            if (origem.getValor() == null || origem.getValor().compareTo(dto.getValor()) < 0) {
                throw new InsufficientBalanceException(
                        origem.getId(), origem.getValor(), dto.getValor());
            }

            // Snapshot ANTES
            BigDecimal saldoOrigemAntes = origem.getValor();
            BigDecimal saldoDestinoAntes = destino.getValor();

            // Movimentação
            origem.setValor(saldoOrigemAntes.subtract(dto.getValor()));
            destino.setValor(saldoDestinoAntes.add(dto.getValor()));

            beneficioRepository.save(origem);
            beneficioRepository.save(destino);

            // Snapshot DEPOIS + conclusão
            transferencia.setSaldoOrigemAntes(saldoOrigemAntes);
            transferencia.setSaldoOrigemDepois(origem.getValor());
            transferencia.setSaldoDestinoAntes(saldoDestinoAntes);
            transferencia.setSaldoDestinoDepois(destino.getValor());
            transferencia.setStatus(TransferenciaStatus.CONCLUIDA);
            transferencia.setFinalizadaEm(LocalDateTime.now());
            transferencia = transferenciaRepository.save(transferencia);

            registrarMetrica(true);
            log.info("Transferência concluída: id={}, origem={} ({} → {}), destino={} ({} → {})",
                    transferencia.getId(),
                    origem.getId(), saldoOrigemAntes, origem.getValor(),
                    destino.getId(), saldoDestinoAntes, destino.getValor());

            return toResultadoDTO(transferencia);

        } catch (Exception ex) {
            transferencia.setStatus(TransferenciaStatus.FALHA);
            transferencia.setMotivoFalha(ex.getMessage());
            transferencia.setFinalizadaEm(LocalDateTime.now());
            transferenciaRepository.save(transferencia);
            registrarMetrica(false);
            log.warn("Transferência falhou: correlacao={}, motivo={}", correlacaoId, ex.getMessage());
            throw ex;
        }
    }

    @Override
    public Page<TransferenciaAuditoriaDTO> listarTodas(Pageable pageable) {
        return transferenciaRepository.findAll(pageable).map(this::toAuditoriaDTO);
    }

    @Override
    public Page<TransferenciaAuditoriaDTO> listarPorBeneficio(Long beneficioId, Pageable pageable) {
        return transferenciaRepository.findByBeneficioId(beneficioId, pageable).map(this::toAuditoriaDTO);
    }

    @Override
    public TransferenciaAuditoriaDTO buscarPorId(Long id) {
        return transferenciaRepository.findById(id)
                .map(this::toAuditoriaDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Transferência", ID, id));
    }

    @Override
    public TransferenciaAuditoriaDTO buscarPorCorrelacaoId(String correlacaoId) {
        return transferenciaRepository.findByCorrelacaoId(correlacaoId)
                .map(this::toAuditoriaDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Transferência", "correlacaoId", correlacaoId));
    }

    private String resolverCorrelacaoId(TransferenciaDTO dto) {
        return (dto.getCorrelacaoId() != null && !dto.getCorrelacaoId().isBlank())
                ? dto.getCorrelacaoId()
                : UUID.randomUUID().toString();
    }

    private void registrarMetrica(boolean sucesso) {
        Counter.builder("transferencias.total")
                .tag("status", sucesso ? "sucesso" : "falha")
                .description("Total de transferências processadas")
                .register(meterRegistry)
                .increment();
    }

    private TransferenciaResultadoDTO toResultadoDTO(Transferencia t) {
        return TransferenciaResultadoDTO.builder()
                .sucesso(t.getStatus() == TransferenciaStatus.CONCLUIDA)
                .mensagem("Transferência " + t.getStatus().name().toLowerCase())
                .valorTransferido(t.getValor())
                .saldoOrigem(t.getSaldoOrigemDepois())
                .saldoDestino(t.getSaldoDestinoDepois())
                .dataTransferencia(t.getFinalizadaEm() != null ? t.getFinalizadaEm() : t.getIniciadaEm())
                .transferenciaId(t.getId())
                .correlacaoId(t.getCorrelacaoId())
                .build();
    }

    private TransferenciaAuditoriaDTO toAuditoriaDTO(Transferencia t) {
        return TransferenciaAuditoriaDTO.builder()
                .id(t.getId())
                .correlacaoId(t.getCorrelacaoId())
                .origemId(t.getOrigemId())
                .destinoId(t.getDestinoId())
                .valor(t.getValor())
                .status(t.getStatus())
                .saldoOrigemAntes(t.getSaldoOrigemAntes())
                .saldoOrigemDepois(t.getSaldoOrigemDepois())
                .saldoDestinoAntes(t.getSaldoDestinoAntes())
                .saldoDestinoDepois(t.getSaldoDestinoDepois())
                .motivoFalha(t.getMotivoFalha())
                .iniciadaEm(t.getIniciadaEm())
                .finalizadaEm(t.getFinalizadaEm())
                .build();
    }
}
