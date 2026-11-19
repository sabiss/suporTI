package com.suporti.suporti.service.filtro;

import com.suporti.suporti.model.Chamado;

import java.util.List;

/**
 * Estratégia de filtragem e ordenação de chamados (padrão Strategy).
 */
public interface FiltroChamado {

    List<Chamado> aplicar(List<Chamado> chamados);
}