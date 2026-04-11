package com.bip.beneficio.domain.service.impl;

import com.bip.beneficio.api.dto.*;
import com.bip.beneficio.api.mapper.BeneficioMapper;
import com.bip.beneficio.domain.entity.Beneficio;
import com.bip.beneficio.domain.exception.*;
import com.bip.beneficio.domain.repository.BeneficioRepository;
import com.bip.beneficio.domain.service.BeneficioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service de benefícios - corrige os problemas do EJB original
 *
 * Problemas identificados no código legado:
 * - Não validava saldo antes de debitar
 * - Sem controle de concorrência (possibilidade de lost updates)
 * - Não verificava se origem/destino existiam
 * - Permitia transferência para o próprio benefício
 * - Fazia merge sem validações adequadas
 *
 * Correções implementadas:
 * - Validação de saldo antes de realizar débito
 * - Pessimistic lock (findByIdWithLock) + optimistic (@Version) para evitar race conditions
 * - Lança exception se não encontrar o registro
 * - Impede transferência origem == destino
 * - Transação com rollback automático
 * - Isolamento SERIALIZABLE nas transferências (pode impactar performance mas garante consistência)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BeneficioServiceImpl implements BeneficioService {

    private final BeneficioRepository repository;
    private final BeneficioMapper mapper;

    private static final String BENEFICIO = "Benefício";
    private static final String ID = "id";

    @Override
    public Page<BeneficioDTO> listarTodos(Pageable pageable) {
        log.debug("Listando todos os benefícios - página: {}, tamanho: {}",
                pageable.getPageNumber(), pageable.getPageSize());
        return repository.findAll(pageable).map(mapper::toDTO);
    }

    @Override
    public Page<BeneficioDTO> listarAtivos(Pageable pageable) {
        log.debug("Listando benefícios ativos - página: {}, tamanho: {}",
                pageable.getPageNumber(), pageable.getPageSize());
        return repository.findByAtivoTrue(pageable).map(mapper::toDTO);
    }

    @Override
    public BeneficioDTO buscarPorId(Long id) {
        log.debug("Buscando benefício por ID: {}", id);
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(BENEFICIO, ID, id));
    }

    @Override
    public Page<BeneficioDTO> buscarPorNome(String nome, Pageable pageable) {
        log.debug("Buscando benefícios por nome: {}", nome);
        return repository.findByNomeContainingIgnoreCase(nome, pageable).map(mapper::toDTO);
    }

    @Override
    @Transactional
    public BeneficioDTO criar(BeneficioCreateDTO dto) {
        log.info("Criando novo benefício: {}", dto.getNome());

        if (repository.existsByNomeIgnoreCase(dto.getNome())) {
            throw new DuplicateResourceException(BENEFICIO, "nome", dto.getNome());
        }

        Beneficio beneficio = mapper.toEntity(dto);
        beneficio = repository.save(beneficio);

        log.info("Benefício criado com sucesso: ID={}", beneficio.getId());
        return mapper.toDTO(beneficio);
    }

    @Override
    @Transactional
    public BeneficioDTO atualizar(Long id, BeneficioUpdateDTO dto) {
        log.info("Atualizando benefício: ID={}", id);

        Beneficio beneficio = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(BENEFICIO, ID, id));

        //Verifica concorrência (Optimistic Locking)
        if (!beneficio.getVersion().equals(dto.getVersion())) {
            throw new ConcurrencyException(id);
        }

        //Validação de duplicidade (excluindo o próprio registro)
        if (repository.existsByNomeIgnoreCaseAndIdNot(dto.getNome(), id)) {
            throw new DuplicateResourceException(BENEFICIO, "nome", dto.getNome());
        }

        mapper.updateEntityFromDTO(dto, beneficio);

        try {
            beneficio = repository.save(beneficio);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConcurrencyException(id);
        }

        log.info("Benefício atualizado com sucesso: ID={}", id);
        return mapper.toDTO(beneficio);
    }

    @Override
    @Transactional
    public void remover(Long id) {
        log.info("Removendo benefício: ID={}", id);

        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException(BENEFICIO, ID, id);
        }

        repository.deleteById(id);
        log.info("Benefício removido com sucesso: ID={}", id);
    }

    @Override
    @Transactional
    public BeneficioDTO ativar(Long id) {
        log.info("Ativando benefício: ID={}", id);

        Beneficio beneficio = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(BENEFICIO, ID, id));

        beneficio.setAtivo(true);
        beneficio = repository.save(beneficio);

        log.info("Benefício ativado com sucesso: ID={}", id);
        return mapper.toDTO(beneficio);
    }

    @Override
    @Transactional
    public BeneficioDTO desativar(Long id) {
        log.info("Desativando benefício: ID={}", id);

        Beneficio beneficio = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(BENEFICIO, ID, id));

        beneficio.setAtivo(false);
        beneficio = repository.save(beneficio);

        log.info("Benefício desativado com sucesso: ID={}", id);
        return mapper.toDTO(beneficio);
    }

    /**
     * Transfere valor entre benefícios.
     *
     * Implementação completamente reescrita para corrigir problemas do EJB original:
     * - Valida origem != destino (implementação anterior permitia transferência para o próprio benefício)
     * - Verifica saldo antes de realizar o débito
     * - Utiliza pessimistic lock (findByIdWithLock) para evitar lost updates
     * - Ordena aquisição de locks por ID para evitar deadlocks (sempre menor ID primeiro)
     * - Isolamento SERIALIZABLE para garantir ausência de race conditions
     * - Rollback automático em caso de falha
     */
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransferenciaResultadoDTO transferir(TransferenciaDTO dto) {
        log.info("Iniciando transferência: origem={}, destino={}, valor={}",
                dto.getOrigemId(), dto.getDestinoId(), dto.getValor());

        //Impede transferência para o mesmo benefício
        if (dto.getOrigemId().equals(dto.getDestinoId())) {
            throw new BusinessException("Origem e destino não podem ser iguais");
        }

        //Adquire locks sempre na mesma ordem (menor ID primeiro) para evitar deadlocks
        Long primeiroId = Math.min(dto.getOrigemId(), dto.getDestinoId());
        Long segundoId = Math.max(dto.getOrigemId(), dto.getDestinoId());

        //Busca com lock pessimista (SELECT ... FOR UPDATE)
        Beneficio primeiro = repository.findByIdWithLock(primeiroId)
                .orElseThrow(() -> new ResourceNotFoundException(BENEFICIO, ID, primeiroId));

        Beneficio segundo = repository.findByIdWithLock(segundoId)
                .orElseThrow(() -> new ResourceNotFoundException(BENEFICIO, ID, segundoId));

        //Identifica qual registro é origem e qual é destino
        Beneficio origem = dto.getOrigemId().equals(primeiroId) ? primeiro : segundo;
        Beneficio destino = dto.getOrigemId().equals(primeiroId) ? segundo : primeiro;

        validarMontante(dto.getValor());

        if (!possuiSaldoSuficiente(origem, dto.getValor())) {
            throw new InsufficientBalanceException(
                    origem.getId(),
                    origem.getValor(),
                    dto.getValor()
            );
        }

        debitar(origem, dto.getValor());
        creditar(destino, dto.getValor());

        repository.save(origem);
        repository.save(destino);

        log.info("Transferência concluída com sucesso: origem={} (saldo={}), destino={} (saldo={})",
                origem.getId(), origem.getValor(), destino.getId(), destino.getValor());

        return TransferenciaResultadoDTO.builder()
                .sucesso(true)
                .mensagem("Transferência realizada com sucesso")
                .valorTransferido(dto.getValor())
                .saldoOrigem(origem.getValor())
                .saldoDestino(destino.getValor())
                .dataTransferencia(LocalDateTime.now())
                .build();
    }

    private void validarMontante(BigDecimal montante) {
        if (montante == null) {
            throw new IllegalArgumentException("Montante não pode ser nulo");
        }
        if (montante.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Montante deve ser maior que zero");
        }
    }

    private boolean possuiSaldoSuficiente(Beneficio beneficio, BigDecimal montante) {
        return beneficio.getValor() != null &&
               montante != null &&
               beneficio.getValor().compareTo(montante) >= 0;
    }

    private void debitar(Beneficio beneficio, BigDecimal montante) {
        beneficio.setValor(beneficio.getValor().subtract(montante));
    }

    private void creditar(Beneficio beneficio, BigDecimal montante) {
        beneficio.setValor(beneficio.getValor().add(montante));
    }
}
