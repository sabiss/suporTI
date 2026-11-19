package com.suporti.suporti.service.filtro;

import com.suporti.suporti.model.Chamado;
import com.suporti.suporti.model.enums.Setor;

import java.util.Comparator;
import java.util.List;

/**
 * Filtro da tela do solicitante: chamados de um setor, do mais novo para o mais antigo.
 * Setor nulo significa "Todos".
 */
public class FiltroPorSetor implements FiltroChamado {

    private final Setor setor;

    public FiltroPorSetor(Setor setor) {
        this.setor = setor;
    }

    @Override
    public List<Chamado> aplicar(List<Chamado> chamados) {
        return chamados.stream()
                .filter(c -> setor == null || c.getSetor() == setor)
                .sorted(Comparator.comparing(Chamado::getDataAbertura).reversed())
                .toList();
    }
}