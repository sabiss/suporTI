package com.suporti.suporti.model;

import java.time.LocalDateTime;

/**
 * Interface que define o contrato de cálculo de Acordo de Nível de Serviço (SLA)
 * e tempo estimado de resolução para diferentes tipos de chamados.
 * 
 * Atende ao requisito de polimorfismo via interface exigido na disciplina de LP2.
 */
public interface SlaCalculavel {

    /**
     * Retorna o tempo máximo em horas estimado para a resolução do chamado
     * conforme seu tipo e prioridade.
     */
    int getTempoMaximoResolucaoHoras();

    /**
     * Calcula a data e hora limite para conclusão a partir de uma data de referência.
     * 
     * @param dataReferencia data a partir da qual o prazo é contado (ex: data de abertura)
     * @return data e hora limite de conclusão
     */
    LocalDateTime calcularPrazoLimite(LocalDateTime dataReferencia);

    /**
     * Indica se o chamado demanda resposta técnica imediata (ex: incidente crítico com indisponibilidade).
     */
    boolean requerAtendimentoImediato();

    /**
     * Retorna uma descrição textual explicativa da regra de SLA aplicada.
     */
    String obterDescricaoSla();
}
