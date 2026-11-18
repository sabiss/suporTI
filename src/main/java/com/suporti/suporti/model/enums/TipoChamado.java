package com.suporti.suporti.model.enums;

/**
 * Define a classificação do chamado: Incidente (falha) ou Requisição (solicitação de serviço).
 */
public enum TipoChamado {
    INCIDENTE("Incidente"),
    REQUISICAO("Requisição");

    private final String descricao;

    TipoChamado(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
