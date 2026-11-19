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
        return getPrioridade() == Prioridade.ALTA || sistemaIndisponivel;
    }

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
