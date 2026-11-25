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
     * Construtor completo para Requisição sem prioridade.
     * A prioridade deve ser definida posteriormente pelo método classificarPrioridade.
     */
    public Requisicao(Long id, String titulo, String descricao, String solicitante,
                      Setor setor, Categoria categoria,
                      StatusChamado status, LocalDateTime dataAbertura,
                      String justificativaNegocio, boolean precisaAprovacao, Boolean aprovado) {
        super(id, titulo, descricao, solicitante, setor, categoria, status, dataAbertura);
        this.justificativaNegocio = justificativaNegocio != null ? justificativaNegocio.trim() : "";
        this.precisaAprovacao = precisaAprovacao;
        this.aprovado = aprovado;
    }

    /**
     * Construtor simplificado para novas requisições sem prioridade.
     */
    public Requisicao(String titulo, String descricao, String solicitante,
                      Setor setor, Categoria categoria,
                      String justificativaNegocio, boolean precisaAprovacao) {
        this(null, titulo, descricao, solicitante, setor, categoria,
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

    /**
     * Retorna o limite de resolução em horas conforme a prioridade e a situação da aprovação.
     * As prioridades alta, média e baixa correspondem a 24, 48 e 72 horas. Se a aprovação
     * obrigatória ainda não foi concedida, são acrescentadas 24 horas.
     *
     * @return quantidade máxima de horas para resolução, ou {@code 0} se ainda não houver prioridade
     */
    @Override
    public int getTempoMaximoResolucaoHoras() {
        if (getPrioridade() == null) {
            return 0;
        }
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

    /**
     * Calcula o prazo limite somando o SLA à data de referência.
     *
     * @param dataReferencia data a partir da qual contar o prazo; se nula, usa a data e hora atuais
     * @return data e hora limite, ou {@code null} se a requisição ainda não tiver prioridade
     */
    @Override
    public LocalDateTime calcularPrazoLimite(LocalDateTime dataReferencia) {
        if (getPrioridade() == null) {
            return null;
        }
        LocalDateTime base = dataReferencia != null ? dataReferencia : LocalDateTime.now();
        return base.plusHours(getTempoMaximoResolucaoHoras());
    }

    @Override
    public boolean requerAtendimentoImediato() {
        // Requisições não possuem natureza emergencial de incidentes críticos
        return false;
    }

    /**
     * Monta uma descrição legível do SLA, da prioridade e da necessidade de aprovação.
     *
     * @return descrição do SLA, ou indicação de que a requisição aguarda triagem se não tiver prioridade
     */
    @Override
    public String obterDescricaoSla() {
        if (getPrioridade() == null) {
            return "Requisição [SEM PRIORIDADE] - Aguardando triagem";
        }
        return String.format("Requisição [%s] - Prazo máximo: %d horas (Requer Aprovação: %s)",
                getPrioridade().getDescricao(),
                getTempoMaximoResolucaoHoras(),
                precisaAprovacao ? "Sim" : "Não");
    }

    // ==========================================
    // REGRAS DE NEGÓCIO ESPECÍFICAS DE REQUISIÇÃO
    // ==========================================

    /**
     * Aprova a requisição quando ela exige aprovação e ainda não está finalizada.
     * Após aprovar, recalcula o prazo para remover o acréscimo referente à aprovação pendente.
     * Não altera o status nem inicia o atendimento.
     *
     * @throws TransicaoStatusInvalidaException se a requisição que exige aprovação já estiver finalizada
     */
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

    /**
     * Rejeita uma requisição que exige aprovação, marcando-a como reprovada e cancelando-a.
     *
     * @param motivoRejeicao justificativa da rejeição; se nula, é registrada uma justificativa padrão
     * @throws TransicaoStatusInvalidaException se a requisição não exigir aprovação ou não puder ser cancelada
     */
    public void rejeitar(String motivoRejeicao) {
        if (!precisaAprovacao) {
            throw new TransicaoStatusInvalidaException("Esta requisição não requer aprovação para ser rejeitada.");
        }
        this.aprovado = false;
        cancelar("Requisição rejeitada pela gestão: " + (motivoRejeicao != null ? motivoRejeicao : "Sem justificativa"));
    }

    /**
     * Inicia o atendimento se a aprovação obrigatória já tiver sido concedida.
     *
     * @throws TransicaoStatusInvalidaException se a aprovação estiver pendente ou se a transição
     *         para atendimento não for válida para o status atual
     */
    @Override
    public void iniciarAtendimento() {
        if (precisaAprovacao && !Boolean.TRUE.equals(aprovado)) {
            throw new TransicaoStatusInvalidaException(
                    "Não é permitido iniciar atendimento de requisição pendente de aprovação.");
        }
        super.iniciarAtendimento();
    }

    /**
     * Verifica se a requisição está apta para iniciar atendimento, considerando também a aprovação.
     *
     * @return {@code true} se atende às regras de aprovação, status e prioridade; {@code false} caso contrário
     */
    @Override
    public boolean podeIniciar() {
        if (precisaAprovacao && !Boolean.TRUE.equals(aprovado)) {
            return false;
        }
        return super.podeIniciar();
    }

    /**
     * Indica se a requisição pode ser aprovada: precisa exigir aprovação, ainda não estar aprovada
     * e não estar finalizada.
     *
     * @return {@code true} se a ação de aprovar estiver disponível; {@code false} caso contrário
     */
    public boolean podeAprovar() {
        return precisaAprovacao && !Boolean.TRUE.equals(aprovado) && getStatus() != null && !getStatus().isFinalizado();
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
