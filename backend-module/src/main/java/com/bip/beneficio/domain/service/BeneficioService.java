package com.bip.beneficio.domain.service;

import com.bip.beneficio.api.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BeneficioService {

    Page<BeneficioDTO> listarTodos(Pageable pageable);

    Page<BeneficioDTO> listarAtivos(Pageable pageable);

    BeneficioDTO buscarPorId(Long id);

    Page<BeneficioDTO> buscarPorNome(String nome, Pageable pageable);

    BeneficioDTO criar(BeneficioCreateDTO dto);

    BeneficioDTO atualizar(Long id, BeneficioUpdateDTO dto);

    void remover(Long id);

    BeneficioDTO ativar(Long id);

    BeneficioDTO desativar(Long id);

    //Transfere valor entre benefícios (com validação de saldo, pessimistic lock, e transação para garantir atomicidade)
    TransferenciaResultadoDTO transferir(TransferenciaDTO dto);
}
