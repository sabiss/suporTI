package com.suporti.suporti.model.exception;

/**
 * Exceção base de domínio para operações e regras de negócio com chamados.
 */
public class ChamadoException extends RuntimeException {

    public ChamadoException(String message) {
        super(message);
    }

    public ChamadoException(String message, Throwable cause) {
        super(message, cause);
    }
}
