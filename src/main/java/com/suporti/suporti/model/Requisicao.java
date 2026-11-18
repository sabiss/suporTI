package com.suporti.suporti.model;

import com.suporti.suporti.model.enums.Categoria;
import com.suporti.suporti.model.enums.Prioridade;
import com.suporti.suporti.model.enums.Setor;
import com.suporti.suporti.model.enums.StatusChamado;
import com.suporti.suporti.model.enums.TipoChamado;
import com.suporti.suporti.model.exception.TransicaoStatusInvalidaException;

import java.time.LocalDateTime;

/**
 * Representa um chamado do tipo Requisição: um pedido formal de um usuário
 * solicitando algo novo (novo acesso, equipamento, instalação de software, permissão).
 * Subclasse concreta de Chamado.
 */
public class Requisicao extends Chamado {

    private String justificativaNegocio;
    private boolean precisaAprovacao;
    private Boolean aprovado; // null = pendente, true = aprovado, false = rejeitado

    /**
     * Construtor completo para Requisição.
     */
    public Requisicao(Long id, String titulo, String descricao, String solicitante,
                      Setor setor, Categoria categoria, Prioridade prioridade,
                      StatusChamado status, LocalDateTime dataAbertura,
                      String justificativaNegocio, boolean precisaAprovacao, Boolean aprovado) {
        super(id, titulo, descricao, solicitante, setor, categoria, prioridade, status, dataAbertura);
        this.justificativaNegocio = justificativaNegocio != null ? justificativaNegocio.trim() : "";
        this.precisaAprovacao = precisaAprovacao;
        this.aprovado = aprovado;
        // Ajusta prazo limite considerando a necessidade de aprovação
        this.setPrazoLimite(calcularPrazoLimite(this.getDataAbertura()));
    }

    /**
     * Construtor simplificado para novas requisições.
     */
    public Requisicao(String titulo, String descricao, String solicitante,
                      Setor setor, Categoria categoria, Prioridade prioridade,
                      String justificativaNegocio, boolean precisaAprovacao) {
        this(null, titulo, descricao, solicitante, setor, categoria, prioridade,
                StatusChamado.ABERTO, LocalDateTime.now(),
                justificativaNegocio, precisaAprovacao, precisaAprovacao ? null : Boolean.TRUE);
    }

    @Override
    public TipoChamado getTipo() {
        return TipoChamado.REQUISICAO;
    }

    // ==========================================
    // IMPLEMENTAÇÃO POLIMÓRFICA DE SLACALCULAVEL
    // ==========================================

    @Override
    public int getTempoMaximoResolucaoHoras() {
        int horasBase;
        if (getPrioridade() == Prioridade.ALTA) {
            horasBase = 24; // 1 dia
        } else if (getPrioridade() == Prioridade.MEDIA) {
            horasBase = 48; // 2 dias
        } else {
            horasBase = 72; // 3 dias
        }

        // Se requer aprovação e ainda não foi aprovado, adiciona 24h para o processo de validação
        if (precisaAprovacao && !Boolean.TRUE.equals(aprovado)) {
            horasBase += 24;
        }

        return horasBase;
    }

    @Override
    public LocalDateTime calcularPrazoLimite(LocalDateTime dataReferencia) {
        LocalDateTime base = dataReferencia != null ? dataReferencia : LocalDateTime.now();
        return base.plusHours(getTempoMaximoResolucaoHoras());
    }

    @Override
    public boolean requerAtendimentoImediato() {
        // Requisições não possuem natureza emergencial de incidentes críticos
        return false;
    }

    @Override
    public String obterDescricaoSla() {
        return String.format("Requisição [%s] - Prazo máximo: %d horas (Requer Aprovação: %s)",
                getPrioridade().getDescricao(),
                getTempoMaximoResolucaoHoras(),
                precisaAprovacao ? "Sim" : "Não");
    }

    // ==========================================
    // REGRAS DE NEGÓCIO ESPECÍFICAS DE REQUISIÇÃO
    // ==========================================

    public void aprovar() {
        if (!precisaAprovacao) {
            return;
        }
        if (getStatus().isFinalizado()) {
            throw new TransicaoStatusInvalidaException("Não é possível aprovar uma requisição finalizada.");
        }
        this.aprovado = true;
        // Recalcula o prazo limite após a aprovação
        if (getDataAbertura() != null) {
            setPrazoLimite(calcularPrazoLimite(getDataAbertura()));
        }
    }

    public void rejeitar(String motivoRejeicao) {
        if (!precisaAprovacao) {
            throw new TransicaoStatusInvalidaException("Esta requisição não requer aprovação para ser rejeitada.");
        }
        this.aprovado = false;
        cancelar("Requisição rejeitada pela gestão: " + (motivoRejeicao != null ? motivoRejeicao : "Sem justificativa"));
    }

    // Sobrescreve iniciarAtendimento garantindo que requisição que precisa de aprovação foi aprovada
    @Override
    public void iniciarAtendimento() {
        if (precisaAprovacao && !Boolean.TRUE.equals(aprovado)) {
            throw new TransicaoStatusInvalidaException(
                    "Não é permitido iniciar atendimento de requisição pendente de aprovação.");
        }
        super.iniciarAtendimento();
    }

    // ==========================================
    // GETTERS E SETTERS
    // ==========================================

    public String getJustificativaNegocio() {
        return justificativaNegocio;
    }

    public void setJustificativaNegocio(String justificativaNegocio) {
        this.justificativaNegocio = justificativaNegocio != null ? justificativaNegocio.trim() : "";
    }

    public boolean isPrecisaAprovacao() {
        return precisaAprovacao;
    }

    public void setPrecisaAprovacao(boolean precisaAprovacao) {
        this.precisaAprovacao = precisaAprovacao;
    }

    public Boolean getAprovado() {
        return aprovado;
    }

    public void setAprovado(Boolean aprovado) {
        this.aprovado = aprovado;
    }
}
