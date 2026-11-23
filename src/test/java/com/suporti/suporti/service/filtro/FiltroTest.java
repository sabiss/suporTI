package com.suporti.suporti.service.filtro;

import com.suporti.suporti.model.Chamado;
import com.suporti.suporti.model.Incidente;
import com.suporti.suporti.model.Requisicao;
import com.suporti.suporti.model.enums.Categoria;
import com.suporti.suporti.model.enums.Prioridade;
import com.suporti.suporti.model.enums.Setor;
import com.suporti.suporti.model.enums.StatusChamado;
import com.suporti.suporti.model.factory.ChamadoFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes dos filtros de chamados (padrão Strategy).
 */
class FiltroTest {

    // ---------- Ajudantes para criar chamados de exemplo ----------

    private Chamado incidente(String titulo, Setor setor, Prioridade prioridade) {
        Chamado chamado = ChamadoFactory.criarIncidente(titulo, "Descrição de teste", "Teste",
                setor, Categoria.HARDWARE, false, "Ambiente de teste");
        chamado.classificarPrioridade(prioridade);
        return chamado;
    }

    private Chamado comPrazo(Chamado chamado, LocalDateTime prazo) {
        chamado.setPrazoLimite(prazo);
        return chamado;
    }

    private Chamado incidenteSemPrioridade(String titulo, LocalDateTime abertura) {
        return new Incidente(null, titulo, "Descrição de teste", "Teste", Setor.TI,
                Categoria.REDE, StatusChamado.ABERTO, abertura, false, "Ambiente de teste");
    }

    private Chamado requisicaoSemPrioridade(String titulo, LocalDateTime abertura) {
        return new Requisicao(null, titulo, "Descrição de teste", "Teste", Setor.TI,
                Categoria.SOFTWARE, StatusChamado.ABERTO, abertura, "Justificativa", false, true);
    }

    // ---------- FiltroPorSetor ----------

    @Test
    void filtroPorSetorMostraSoOSetorEscolhido() {
        Chamado financeiro = incidente("Impressora", Setor.FINANCEIRO, Prioridade.MEDIA);
        Chamado marketing = incidente("Internet", Setor.MARKETING, Prioridade.ALTA);

        List<Chamado> resultado = new FiltroPorSetor(Setor.FINANCEIRO)
                .aplicar(List.of(financeiro, marketing));

        assertEquals(List.of(financeiro), resultado);
    }

    @Test
    void filtroPorSetorNuloMostraTodos() {
        Chamado financeiro = incidente("Impressora", Setor.FINANCEIRO, Prioridade.MEDIA);
        Chamado marketing = incidente("Internet", Setor.MARKETING, Prioridade.ALTA);

        List<Chamado> resultado = new FiltroPorSetor(null)
                .aplicar(List.of(financeiro, marketing));

        assertEquals(2, resultado.size());
    }

    // ---------- FiltroFilaDeTrabalho ----------

    @Test
    void filaOrdenaPrimeiroPeloDiaDepoisPelaPrioridade() {
        Chamado ontemBaixa = comPrazo(incidente("Ontem baixa", Setor.TI, Prioridade.BAIXA),
                LocalDateTime.of(2026, 11, 19, 10, 0));
        Chamado hojeMedia9h = comPrazo(incidente("Hoje média", Setor.TI, Prioridade.MEDIA),
                LocalDateTime.of(2026, 11, 20, 9, 0));
        Chamado hojeAlta15h = comPrazo(incidente("Hoje alta", Setor.TI, Prioridade.ALTA),
                LocalDateTime.of(2026, 11, 20, 15, 0));
        Chamado amanhaAlta = comPrazo(incidente("Amanhã alta", Setor.TI, Prioridade.ALTA),
                LocalDateTime.of(2026, 11, 21, 8, 0));

        List<Chamado> resultado = new FiltroFilaDeTrabalho()
                .aplicar(List.of(amanhaAlta, hojeMedia9h, ontemBaixa, hojeAlta15h));

        // O dia vem primeiro; no mesmo dia (20/11), a Alta passa a Média mesmo vencendo mais tarde
        assertEquals(List.of(ontemBaixa, hojeAlta15h, hojeMedia9h, amanhaAlta), resultado);
    }

    @Test
    void filaNaoMostraChamadosConcluidosNemCancelados() {
        Chamado aberto = incidente("Aberto", Setor.TI, Prioridade.ALTA);

        Chamado concluido = incidente("Concluído", Setor.TI, Prioridade.ALTA);
        concluido.iniciarAtendimento();
        concluido.concluir();

        Chamado cancelado = incidente("Cancelado", Setor.TI, Prioridade.ALTA);
        cancelado.cancelar("Aberto por engano");

        List<Chamado> resultado = new FiltroFilaDeTrabalho()
                .aplicar(List.of(aberto, concluido, cancelado));

        assertEquals(List.of(aberto), resultado);
    }

    // ---------- FiltroAguardandoTriagem ----------

    @Test
    void triagemNaoMostraChamadosQueJaTemPrioridade() {
        Chamado comPrioridade = incidente("Já triado", Setor.TI, Prioridade.MEDIA);

        List<Chamado> resultado = new FiltroAguardandoTriagem()
                .aplicar(List.of(comPrioridade));

        assertTrue(resultado.isEmpty());
    }

    @Test
    void triagemMostraSoChamadosAbertosSemPrioridade() {
        Chamado semPrioridade = incidenteSemPrioridade("Novo", LocalDateTime.of(2026, 11, 20, 9, 0));
        Chamado comPrioridade = incidente("Já triado", Setor.TI, Prioridade.MEDIA);
        Chamado cancelado = incidenteSemPrioridade("Cancelado", LocalDateTime.of(2026, 11, 20, 8, 0));
        cancelado.cancelar("Aberto por engano");

        List<Chamado> resultado = new FiltroAguardandoTriagem()
                .aplicar(List.of(semPrioridade, comPrioridade, cancelado));

        assertEquals(List.of(semPrioridade), resultado);
    }

    @Test
    void triagemColocaIncidentesAntesDeRequisicoes() {
        // A requisição é mais antiga, mas o incidente vem primeiro
        Chamado requisicao = requisicaoSemPrioridade("Instalar Excel", LocalDateTime.of(2026, 11, 18, 9, 0));
        Chamado incidente = incidenteSemPrioridade("Sem internet", LocalDateTime.of(2026, 11, 20, 9, 0));

        List<Chamado> resultado = new FiltroAguardandoTriagem()
                .aplicar(List.of(requisicao, incidente));

        assertEquals(List.of(incidente, requisicao), resultado);
    }

    @Test
    void triagemMostraOMaisAntigoPrimeiro() {
        Chamado maisNovo = incidenteSemPrioridade("Mais novo", LocalDateTime.of(2026, 11, 20, 15, 0));
        Chamado maisAntigo = incidenteSemPrioridade("Mais antigo", LocalDateTime.of(2026, 11, 19, 10, 0));

        List<Chamado> resultado = new FiltroAguardandoTriagem()
                .aplicar(List.of(maisNovo, maisAntigo));

        assertEquals(List.of(maisAntigo, maisNovo), resultado);
    }
}