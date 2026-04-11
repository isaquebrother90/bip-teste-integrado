package com.bip.beneficio.domain.exception;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String correlacaoId) {
        super(String.format(
                "Transferência com correlacaoId '%s' já foi processada. " +
                "Consulte o endpoint GET /v1/transferencias/correlacao/{correlacaoId} para ver o resultado.",
                correlacaoId));
    }
}
