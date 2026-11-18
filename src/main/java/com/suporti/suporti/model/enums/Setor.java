package com.suporti.suporti.model.enums;

/**
 * Setores da organização que podem solicitar suporte técnico.
 */
public enum Setor {
    TI("Tecnologia da Informação"),
    RH("Recursos Humanos"),
    FINANCEIRO("Financeiro"),
    OPERACIONAL("Operacional"),
    VENDAS("Vendas"),
    MARKETING("Marketing"),
    DIRETORIA("Diretoria");

    private final String nomeExibicao;

    Setor(String nomeExibicao) {
        this.nomeExibicao = nomeExibicao;
    }

    public String getNomeExibicao() {
        return nomeExibicao;
    }
}
