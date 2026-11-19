package com.suporti.suporti.service.filtro;

import com.suporti.suporti.model.Chamado;

import java.util.Comparator;
import java.util.List;

/**
 * Aba "Fila de trabalho" do técnico: chamados com prioridade e ainda não finalizados.
 * Ordem: primeiro o dia do prazo; no mesmo dia, ALTA > MEDIA > BAIXA.
 */
public class FiltroFilaDeTrabalho implements FiltroChamado {

    @Override
    public List<Chamado> aplicar(List<Chamado> chamados) {
        return chamados.stream()
                .filter(c -> c.getPrioridade() != null)
                .filter(c -> c.getPrazoLimite() != null)
                .filter(c -> !c.getStatus().isFinalizado())
                .sorted(Comparator
                        .comparing((Chamado c) -> c.getPrazoLimite().toLocalDate())
                        .thenComparing(c -> c.getPrioridade().getNivel(), Comparator.reverseOrder()))
                .toList();
    }
}