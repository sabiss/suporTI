package com.suporti.suporti.model.exception;

import com.suporti.suporti.model.enums.StatusChamado;

/**
 * Exceção lançada quando ocorre uma tentativa de transição ilegal no ciclo de vida do chamado.
 */
public class TransicaoStatusInvalidaException extends ChamadoException {

    private final StatusChamado statusOrigem;
    private final StatusChamado statusDestino;

    public TransicaoStatusInvalidaException(StatusChamado statusOrigem, StatusChamado statusDestino) {
        super(String.format("Transição de status inválida: não é permitido mudar de [%s] para [%s].",
                statusOrigem != null ? statusOrigem.getDescricao() : "Nulo",
                statusDestino != null ? statusDestino.getDescricao() : "Nulo"));
        this.statusOrigem = statusOrigem;
        this.statusDestino = statusDestino;
    }

    public TransicaoStatusInvalidaException(String mensagem) {
        super(mensagem);
        this.statusOrigem = null;
        this.statusDestino = null;
    }

    public StatusChamado getStatusOrigem() {
        return statusOrigem;
    }

    public StatusChamado getStatusDestino() {
        return statusDestino;
    }
}
