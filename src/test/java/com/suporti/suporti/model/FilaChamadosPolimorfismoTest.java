package com.suporti.suporti.model;

import com.suporti.suporti.model.enums.Categoria;
import com.suporti.suporti.model.enums.Prioridade;
import com.suporti.suporti.model.enums.Setor;
import com.suporti.suporti.model.factory.ChamadoFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Polimorfismo e Ordenação da Fila de Atendimento")
class FilaChamadosPolimorfismoTest {

    @Test
    @DisplayName("Deve manipular coleções polimórficas de SlaCalculavel e invocar métodos dinamicamente")
    void deveManipularSlaPolimorficamente() {
        List<SlaCalculavel> itensComSla = new ArrayList<>();

        itensComSla.add(ChamadoFactory.criarIncidente(
                "Falha no Gateway de Pagamento", "API de pagamentos fora", "Financeiro",
                Setor.FINANCEIRO, Categoria.SOFTWARE, Prioridade.ALTA, true, "Produção"));

        itensComSla.add(ChamadoFactory.criarRequisicao(
                "Instalação de Antivírus", "Atualizar endpoints", "Segurança",
                Setor.TI, Categoria.SOFTWARE, Prioridade.BAIXA, "Compliance", false));

        itensComSla.add(ChamadoFactory.criarIncidente(
                "Mouse com falha no scroll", "Scroll travando", "Colaborador",
                Setor.RH, Categoria.HARDWARE, Prioridade.BAIXA, false, "Escritório"));

        // Verificação polimórfica: sem saber se é Incidente ou Requisição em tempo de compilação
        int totalHoras = 0;
        int criticosImediatos = 0;

        for (SlaCalculavel item : itensComSla) {
            totalHoras += item.getTempoMaximoResolucaoHoras();
            if (item.requerAtendimentoImediato()) {
                criticosImediatos++;
            }
            assertNotNull(item.obterDescricaoSla());
        }

        // Incidente crítico alta c/ indisponibilidade = 2h
        // Requisição baixa s/ aprovação = 72h
        // Incidente baixa = 24h
        // Total = 98h
        assertEquals(98, totalHoras);
        assertEquals(1, criticosImediatos);
    }

    @Test
    @DisplayName("Deve ordenar a fila de tarefas do técnico: maior prioridade primeiro, desempate por menor prazo limite")
    void deveOrdenarFilaPorPrioridadeEPrazo() {
        Chamado chamadoBaixa = ChamadoFactory.criarRequisicao(
                "Cadeira nova", "Troca ergonômica", "Ana",
                Setor.RH, Categoria.HARDWARE, Prioridade.BAIXA, "Ergonomia", false);

        Chamado chamadoMedia = ChamadoFactory.criarIncidente(
                "Impressora lenta", "Demora na impressão", "Beto",
                Setor.VENDAS, Categoria.HARDWARE, Prioridade.MEDIA, false, "Loja");

        Chamado chamadoAltaCritica = ChamadoFactory.criarIncidente(
                "Servidor de autenticação caiu", "Ninguém consegue logar", "Carlos",
                Setor.TI, Categoria.SOFTWARE, Prioridade.ALTA, true, "Produção");

        Chamado chamadoAltaNormal = ChamadoFactory.criarRequisicao(
                "Liberar acesso a cliente VIP", "Acesso urgente", "Diretor",
                Setor.DIRETORIA, Categoria.ACESSO_E_PERMISSAO, Prioridade.ALTA, "Urgência cliente", false);

        List<Chamado> fila = new ArrayList<>();
        fila.add(chamadoBaixa);
        fila.add(chamadoMedia);
        fila.add(chamadoAltaCritica);
        fila.add(chamadoAltaNormal);

        // Ordena usando o compareTo implementado em Chamado
        Collections.sort(fila);

        // O primeiro deve ser o de prioridade ALTA com menor prazo limite (incidente crítico com 2h de prazo)
        assertEquals(chamadoAltaCritica, fila.get(0), "Primeiro lugar deve ser o Incidente Crítico de prioridade ALTA");
        // O segundo deve ser o outro de prioridade ALTA (requisição urgente com 24h)
        assertEquals(chamadoAltaNormal, fila.get(1));
        // O terceiro deve ser prioridade MEDIA
        assertEquals(chamadoMedia, fila.get(2));
        // O último deve ser prioridade BAIXA
        assertEquals(chamadoBaixa, fila.get(3));
    }

    @Test
    @DisplayName("Deve funcionar corretamente com PriorityQueue (fila de prioridade)")
    void deveFuncionarComPriorityQueue() {
        PriorityQueue<Chamado> queue = new PriorityQueue<>();

        Chamado c1 = ChamadoFactory.criarRequisicao("Item 1", "desc", "u1", Setor.TI, Categoria.HARDWARE, Prioridade.BAIXA, "just", false);
        Chamado c2 = ChamadoFactory.criarIncidente("Item 2", "desc", "u2", Setor.TI, Categoria.SOFTWARE, Prioridade.ALTA, true, "Prod");
        Chamado c3 = ChamadoFactory.criarIncidente("Item 3", "desc", "u3", Setor.TI, Categoria.REDE, Prioridade.MEDIA, false, "Geral");

        queue.add(c1);
        queue.add(c2);
        queue.add(c3);

        assertEquals(c2, queue.poll(), "PriorityQueue deve entregar primeiro o chamado de maior prioridade");
        assertEquals(c3, queue.poll(), "PriorityQueue deve entregar em seguida o de prioridade média");
        assertEquals(c1, queue.poll(), "PriorityQueue deve entregar por último o de menor prioridade");
    }
}
