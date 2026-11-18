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
     * Construtor completo para Incidente.
     */
    public Incidente(Long id, String titulo, String descricao, String solicitante,
                     Setor setor, Categoria categoria, Prioridade prioridade,
                     StatusChamado status, LocalDateTime dataAbertura,
                     boolean sistemaIndisponivel, String ambienteAfetado) {
        super(id, titulo, descricao, solicitante, setor, categoria, prioridade, status, dataAbertura);
        this.sistemaIndisponivel = sistemaIndisponivel;
        this.ambienteAfetado = ambienteAfetado != null && !ambienteAfetado.trim().isEmpty() 
                ? ambienteAfetado.trim() 
                : "Geral";
        // Recalcula o prazo limite considerando a flag sistemaIndisponivel
        this.setPrazoLimite(calcularPrazoLimite(this.getDataAbertura()));
    }

    /**
     * Construtor simplificado para novos incidentes.
     */
    public Incidente(String titulo, String descricao, String solicitante,
                     Setor setor, Categoria categoria, Prioridade prioridade,
                     boolean sistemaIndisponivel, String ambienteAfetado) {
        this(null, titulo, descricao, solicitante, setor, categoria, prioridade,
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
        LocalDateTime base = dataReferencia != null ? dataReferencia : LocalDateTime.now();
        return base.plusHours(getTempoMaximoResolucaoHoras());
    }

    @Override
    public boolean requerAtendimentoImediato() {
        return getPrioridade() == Prioridade.ALTA || sistemaIndisponivel;
    }

    @Override
    public String obterDescricaoSla() {
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
