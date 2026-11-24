package com.suporti.suporti.model.exception;

/**
 * Lançada quando se procura um chamado por um número que não existe no banco.
 */
public class ChamadoNaoEncontradoException extends ChamadoException {

    private final Long id;

    public ChamadoNaoEncontradoException(Long id) {
        super("Chamado nº " + id + " não encontrado.");
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}