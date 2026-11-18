package com.suporti.suporti.model.enums;

/**
 * Categorias técnicas para os chamados de suporte.
 */
public enum Categoria {
    HARDWARE("Hardware"),
    SOFTWARE("Software"),
    REDE("Rede e Conectividade"),
    ACESSO_E_PERMISSAO("Acesso e Permissões"),
    OUTROS("Outros");

    private final String descricao;

    Categoria(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
