package com.bip.beneficio.domain.service.impl;

import com.bip.beneficio.api.dto.*;
import com.bip.beneficio.api.mapper.BeneficioMapper;
import com.bip.beneficio.domain.entity.Beneficio;
import com.bip.beneficio.domain.exception.*;
import com.bip.beneficio.domain.repository.BeneficioRepository;
import com.bip.beneficio.domain.service.BeneficioService;
import com.bip.beneficio.domain.service.TransferenciaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final TransferenciaService transferenciaService;

    private static final String BENEFICIO = "Benefício";
    private static final String ID = "id";

    @Override
    @Cacheable(value = "beneficios-metadata", key = "#nome + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<BeneficioMetadataDTO> buscarMetadados(String nome, Pageable pageable) {
        log.debug("Buscando metadados (cached): nome={}", nome);
        if (nome != null && !nome.isBlank()) {
            return repository.findByNomeContainingIgnoreCase(nome, pageable).map(mapper::toMetadataDTO);
        }
        return repository.findAll(pageable).map(mapper::toMetadataDTO);
    }

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
    @CacheEvict(value = "beneficios-metadata", allEntries = true)
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
    @CacheEvict(value = "beneficios-metadata", allEntries = true)
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
    @CacheEvict(value = "beneficios-metadata", allEntries = true)
    public void remover(Long id) {
        removerComMotivo(id, null);
    }

    @Override
    @Transactional
    public void removerComMotivo(Long id, String motivo) {
        log.info("Removendo benefício (soft delete): ID={}, motivo={}", id, motivo);

        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException(BENEFICIO, ID, id);
        }

        // @Modifying(clearAutomatically=true) invalida o L1 cache após o UPDATE,
        // garantindo que findById subsequente vá ao banco e respeite o @SQLRestriction
        repository.softDeleteById(id, LocalDateTime.now(), motivo);

        log.info("Benefício removido com sucesso (soft delete): ID={}", id);
    }

    @Override
    @Transactional
    public BeneficioDTO restaurar(Long id) {
        log.info("Restaurando benefício: ID={}", id);

        // @SQLRestriction filtra deletados, então precisamos de query nativa
        Beneficio beneficio = repository.findByIdIncluindoDeletados(id)
                .orElseThrow(() -> new ResourceNotFoundException(BENEFICIO, ID, id));

        if (beneficio.getDeletadoEm() == null) {
            throw new BusinessException("Benefício ID=" + id + " não está removido.");
        }

        beneficio.setDeletadoEm(null);
        beneficio.setMotivoDesativacao(null);
        beneficio = repository.save(beneficio);

        log.info("Benefício restaurado com sucesso: ID={}", id);
        return mapper.toDTO(beneficio);
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
     * @deprecated Delegado para {@link TransferenciaService}.
     * Use POST /v1/transferencias diretamente para obter rastreamento completo de auditoria.
     */
    @Override
    @Deprecated(since = "2.0", forRemoval = true)
    public TransferenciaResultadoDTO transferir(TransferenciaDTO dto) {
        return transferenciaService.transferir(dto);
    }
}
