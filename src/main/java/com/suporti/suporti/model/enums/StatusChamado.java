package com.suporti.suporti.model.enums;

/**
 * Representa os possíveis estados do ciclo de vida de um chamado.
 */
public enum StatusChamado {
    ABERTO("Aberto"),
    EM_ANDAMENTO("Em Andamento"),
    CONCLUIDO("Concluído"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusChamado(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isFinalizado() {
        return this == CONCLUIDO || this == CANCELADO;
    }
}
