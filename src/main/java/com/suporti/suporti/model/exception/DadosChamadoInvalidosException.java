package com.suporti.suporti.model.exception;

/**
 * Exceção lançada quando os dados essenciais para criação ou edição de um chamado violam as regras do sistema.
 */
public class DadosChamadoInvalidosException extends ChamadoException {

    private final String campo;

    public DadosChamadoInvalidosException(String campo, String mensagem) {
        super(String.format("Dado inválido para o campo '%s': %s", campo, mensagem));
        this.campo = campo;
    }

    public DadosChamadoInvalidosException(String mensagem) {
        super(mensagem);
        this.campo = null;
    }

    public String getCampo() {
        return campo;
    }
}
