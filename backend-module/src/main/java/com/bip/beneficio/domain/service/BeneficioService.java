package com.bip.beneficio.domain.service;

import com.bip.beneficio.api.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BeneficioService {

    /**
     * Retorna metadados cacheados (nome, descrição, ativo) — sem o campo valor.
     * Cache TTL: 5 minutos. Use buscarPorId() para dados financeiros atualizados.
     */
    Page<BeneficioMetadataDTO> buscarMetadados(String nome, Pageable pageable);

    Page<BeneficioDTO> listarTodos(Pageable pageable);

    Page<BeneficioDTO> listarAtivos(Pageable pageable);

    BeneficioDTO buscarPorId(Long id);

    Page<BeneficioDTO> buscarPorNome(String nome, Pageable pageable);

    BeneficioDTO criar(BeneficioCreateDTO dto);

    BeneficioDTO atualizar(Long id, BeneficioUpdateDTO dto);

    /** Soft delete — registra data de remoção sem apagar o registro. */
    void remover(Long id);

    /** Soft delete com motivo explícito. */
    void removerComMotivo(Long id, String motivo);

    /** Restaura um benefício removido via soft delete. */
    BeneficioDTO restaurar(Long id);

    BeneficioDTO ativar(Long id);

    BeneficioDTO desativar(Long id);

    /** @deprecated Use {@link TransferenciaService#transferir} diretamente via POST /v1/transferencias */
    @Deprecated(since = "2.0", forRemoval = true)
    TransferenciaResultadoDTO transferir(TransferenciaDTO dto);
}
