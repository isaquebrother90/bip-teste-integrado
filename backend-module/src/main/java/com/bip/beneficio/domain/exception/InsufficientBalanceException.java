package com.bip.beneficio.domain.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends BusinessException {

    private final Long beneficioId;
    private final BigDecimal saldoAtual;
    private final BigDecimal valorSolicitado;

    public InsufficientBalanceException(Long beneficioId, BigDecimal saldoAtual, BigDecimal valorSolicitado) {
        super("INSUFFICIENT_BALANCE",
              String.format("Saldo insuficiente no benefício %d. Saldo atual: %s, Valor solicitado: %s",
                           beneficioId, saldoAtual, valorSolicitado));
        this.beneficioId = beneficioId;
        this.saldoAtual = saldoAtual;
        this.valorSolicitado = valorSolicitado;
    }

    public Long getBeneficioId() {
        return beneficioId;
    }

    public BigDecimal getSaldoAtual() {
        return saldoAtual;
    }

    public BigDecimal getValorSolicitado() {
        return valorSolicitado;
    }
}
