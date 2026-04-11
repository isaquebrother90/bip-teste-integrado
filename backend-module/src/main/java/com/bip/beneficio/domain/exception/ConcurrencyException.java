package com.bip.beneficio.domain.exception;

public class ConcurrencyException extends BusinessException {

    public ConcurrencyException(String message) {
        super("CONCURRENCY_CONFLICT", message);
    }

    public ConcurrencyException(Long resourceId) {
        super("CONCURRENCY_CONFLICT",
              String.format("O recurso %d foi modificado por outro usuário. Por favor, recarregue os dados e tente novamente.",
                           resourceId));
    }
}
