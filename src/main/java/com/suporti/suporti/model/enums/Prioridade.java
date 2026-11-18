package com.suporti.suporti.model.enums;

/**
 * Representa o nível de urgência/prioridade atribuído a um chamado.
 */
public enum Prioridade {
    BAIXA(1, "Baixa"),
    MEDIA(2, "Média"),
    ALTA(3, "Alta");

    private final int nivel;
    private final String descricao;

    Prioridade(int nivel, String descricao) {
        this.nivel = nivel;
        this.descricao = descricao;
    }

    public int getNivel() {
        return nivel;
    }

    public String getDescricao() {
        return descricao;
    }
}
