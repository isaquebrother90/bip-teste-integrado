package com.bip.beneficio.domain.service;

import com.bip.beneficio.api.dto.TransferenciaAuditoriaDTO;
import com.bip.beneficio.api.dto.TransferenciaDTO;
import com.bip.beneficio.api.dto.TransferenciaResultadoDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransferenciaService {

    TransferenciaResultadoDTO transferir(TransferenciaDTO dto);

    Page<TransferenciaAuditoriaDTO> listarTodas(Pageable pageable);

    Page<TransferenciaAuditoriaDTO> listarPorBeneficio(Long beneficioId, Pageable pageable);

    TransferenciaAuditoriaDTO buscarPorId(Long id);

    TransferenciaAuditoriaDTO buscarPorCorrelacaoId(String correlacaoId);
}
