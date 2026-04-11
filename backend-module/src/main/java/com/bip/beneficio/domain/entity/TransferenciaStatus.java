package com.bip.beneficio.domain.entity;

/**
 * Máquina de estados de uma transferência financeira.
 *
 * <pre>
 * PENDENTE ──► PROCESSANDO ──► CONCLUIDA
 *                  │
 *                  └──► FALHA
 *                  └──► REVERTIDA
 * </pre>
 */
public enum TransferenciaStatus {
    PENDENTE,
    PROCESSANDO,
    CONCLUIDA,
    FALHA,
    REVERTIDA
}
