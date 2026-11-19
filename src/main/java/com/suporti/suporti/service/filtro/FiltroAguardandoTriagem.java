package com.suporti.suporti.service.filtro;

import com.suporti.suporti.model.Chamado;
import com.suporti.suporti.model.enums.StatusChamado;
import com.suporti.suporti.model.enums.TipoChamado;

import java.util.Comparator;
import java.util.List;

/**
 * Aba "Aguardando triagem" do técnico: chamados abertos e ainda sem prioridade.
 * Incidentes antes de requisições; depois, o mais antigo primeiro.
 */
public class FiltroAguardandoTriagem implements FiltroChamado {

    @Override
    public List<Chamado> aplicar(List<Chamado> chamados) {
        return chamados.stream()
                .filter(c -> c.getStatus() == StatusChamado.ABERTO)
                .filter(c -> c.getPrioridade() == null)
                .sorted(Comparator
                        .comparing((Chamado c) -> c.getTipo() == TipoChamado.INCIDENTE ? 0 : 1)
                        .thenComparing(Chamado::getDataAbertura))
                .toList();
    }
}