package com.suporti.suporti.service.filtro;

import com.suporti.suporti.model.Chamado;

import java.util.Comparator;
import java.util.List;

/**
 * Aba "Fila de trabalho" do técnico: chamados com prioridade e ainda não finalizados.
 * Ordem:
 * 1. atrasados primeiro (o prazo já passou);
 * 2. depois pelo dia do prazo;
 * 3. no mesmo dia, ALTA > MEDIA > BAIXA;
 * 4. empate de dia e prioridade: o horário que vence antes.
 */
public class FiltroFilaDeTrabalho implements FiltroChamado {

    @Override
    public List<Chamado> aplicar(List<Chamado> chamados) {
        return chamados.stream()
                .filter(c -> c.getPrioridade() != null)
                .filter(c -> c.getPrazoLimite() != null)
                .filter(c -> !c.getStatus().isFinalizado())
                .sorted(Comparator
                        .comparing((Chamado c) -> !c.isAtrasado())
                        .thenComparing(c -> c.getPrazoLimite().toLocalDate())
                        .thenComparing(c -> c.getPrioridade().getNivel(), Comparator.reverseOrder())
                        .thenComparing(Chamado::getPrazoLimite))
                .toList();
    }
}