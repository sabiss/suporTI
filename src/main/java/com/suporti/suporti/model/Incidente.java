package com.suporti.suporti.model;

import com.suporti.suporti.model.enums.Categoria;
import com.suporti.suporti.model.enums.Prioridade;
import com.suporti.suporti.model.enums.Setor;
import com.suporti.suporti.model.enums.StatusChamado;
import com.suporti.suporti.model.enums.TipoChamado;

import java.time.LocalDateTime;

/**
 * Representa um chamado do tipo Incidente: uma interrupção não planejada
 * ou redução da qualidade de um serviço de TI.
 * Subclasse concreta de Chamado.
 */
public class Incidente extends Chamado {

    private boolean sistemaIndisponivel;
    private String ambienteAfetado;

    /**
     * Construtor completo para Incidente sem prioridade.
     * A prioridade deve ser definida posteriormente pelo método classificarPrioridade.
     */
    public Incidente(Long id, String titulo, String descricao, String solicitante,
                     Setor setor, Categoria categoria,
                     StatusChamado status, LocalDateTime dataAbertura,
                     boolean sistemaIndisponivel, String ambienteAfetado) {
        super(id, titulo, descricao, solicitante, setor, categoria, status, dataAbertura);
        this.sistemaIndisponivel = sistemaIndisponivel;
        this.ambienteAfetado = ambienteAfetado != null && !ambienteAfetado.trim().isEmpty()
                ? ambienteAfetado.trim()
                : "Geral";
    }

    /**
     * Construtor simplificado para novos incidentes sem prioridade.
     */
    public Incidente(String titulo, String descricao, String solicitante,
                     Setor setor, Categoria categoria,
                     boolean sistemaIndisponivel, String ambienteAfetado) {
        this(null, titulo, descricao, solicitante, setor, categoria,
                StatusChamado.ABERTO, LocalDateTime.now(), sistemaIndisponivel, ambienteAfetado);
    }

    @Override
    public TipoChamado getTipo() {
        return TipoChamado.INCIDENTE;
    }

    // ==========================================
    // IMPLEMENTAÇÃO POLIMÓRFICA DE SLACALCULAVEL
    // ==========================================

    /**
     * Retorna o limite de resolução em horas conforme a prioridade e a indisponibilidade do sistema.
     * Incidentes de prioridade alta têm limite de 2 horas se o sistema estiver indisponível e
     * de 4 horas caso contrário; prioridades média e baixa têm limites de 8 e 24 horas.
     *
     * @return quantidade máxima de horas para resolução, ou {@code 0} se ainda não houver prioridade
     */
    @Override
    public int getTempoMaximoResolucaoHoras() {
        if (getPrioridade() == null) {
            return 0;
        }
        if (getPrioridade() == Prioridade.ALTA) {
            return sistemaIndisponivel ? 2 : 4;
        } else if (getPrioridade() == Prioridade.MEDIA) {
            return 8;
        } else {
            return 24;
        }
    }

    /**
     * Calcula o prazo limite somando o SLA à data de referência.
     *
     * @param dataReferencia data a partir da qual contar o prazo; se nula, usa a data e hora atuais
     * @return data e hora limite, ou {@code null} se o incidente ainda não tiver prioridade
     */
    @Override
    public LocalDateTime calcularPrazoLimite(LocalDateTime dataReferencia) {
        if (getPrioridade() == null) {
            return null;
        }
        LocalDateTime base = dataReferencia != null ? dataReferencia : LocalDateTime.now();
        return base.plusHours(getTempoMaximoResolucaoHoras());
    }

    /**
     * Indica se o incidente deve receber atendimento imediato por ter prioridade alta ou por
     * representar indisponibilidade do sistema. Este método apenas informa a urgência; não altera
     * o status nem inicia o atendimento.
     *
     * @return {@code true} se a prioridade for alta ou o sistema estiver indisponível;
     *         {@code false} caso contrário
     */
    @Override
    public boolean requerAtendimentoImediato() {
        return getPrioridade() == Prioridade.ALTA || sistemaIndisponivel;
    }

    /**
     * Monta uma descrição legível do SLA e da indisponibilidade para exibição.
     *
     * @return descrição do SLA, ou indicação de que o incidente aguarda triagem se não tiver prioridade
     */
    @Override
    public String obterDescricaoSla() {
        if (getPrioridade() == null) {
            return "Incidente [SEM PRIORIDADE] - Aguardando triagem";
        }
        return String.format("Incidente [%s] - Prazo máximo: %d horas (Indisponibilidade Crítica: %s)",
                getPrioridade().getDescricao(),
                getTempoMaximoResolucaoHoras(),
                sistemaIndisponivel ? "Sim" : "Não");
    }

    // ==========================================
    // GETTERS E SETTERS
    // ==========================================

    public boolean isSistemaIndisponivel() {
        return sistemaIndisponivel;
    }

    /**
     * Atualiza se o sistema está indisponível e recalcula o prazo limite do incidente com base
     * na data de abertura, quando disponível.
     *
     * @param sistemaIndisponivel {@code true} se o sistema estiver indisponível; {@code false} caso contrário
     */
    public void setSistemaIndisponivel(boolean sistemaIndisponivel) {
        this.sistemaIndisponivel = sistemaIndisponivel;
        // Recalcula prazo limite se houver alteração de severidade
        if (getDataAbertura() != null) {
            setPrazoLimite(calcularPrazoLimite(getDataAbertura()));
        }
    }

    public String getAmbienteAfetado() {
        return ambienteAfetado;
    }

    public void setAmbienteAfetado(String ambienteAfetado) {
        this.ambienteAfetado = ambienteAfetado != null ? ambienteAfetado.trim() : "Geral";
    }
}
