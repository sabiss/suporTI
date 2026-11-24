package com.suporti.suporti.repository;

import com.suporti.suporti.model.Chamado;
import com.suporti.suporti.model.Incidente;
import com.suporti.suporti.model.Requisicao;
import com.suporti.suporti.model.enums.Categoria;
import com.suporti.suporti.model.enums.Prioridade;
import com.suporti.suporti.model.enums.Setor;
import com.suporti.suporti.model.enums.StatusChamado;
import com.suporti.suporti.model.factory.ChamadoFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do ChamadoRepository com um banco SQLite de teste (target/teste-suporti.db),
 * separado do suporti.db usado ao rodar o sistema.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/teste-suporti.db")
class ChamadoRepositoryTest {

    @Autowired
    private ChamadoRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limparTabela() {
        jdbc.update("DELETE FROM chamados");
    }

    private Incidente novoIncidente(String titulo) {
        return ChamadoFactory.criarIncidente(titulo, "Sem acesso à internet", "Teste",
                Setor.MARKETING, Categoria.REDE, true, "Escritório");
    }

    @Test
    void salvarGeraIdEBuscarDevolveOMesmoIncidente() {
        Incidente incidente = novoIncidente("Sem internet");

        Long id = repository.salvar(incidente);
        Chamado lido = repository.buscarPorId(id).orElseThrow();

        assertNotNull(id);
        assertEquals(id, incidente.getId());
        assertInstanceOf(Incidente.class, lido);
        assertEquals("Sem internet", lido.getTitulo());
        assertEquals(Setor.MARKETING, lido.getSetor());
        assertEquals(StatusChamado.ABERTO, lido.getStatus());
        assertNull(lido.getPrioridade());
        assertTrue(((Incidente) lido).isSistemaIndisponivel());
        assertEquals("Escritório", ((Incidente) lido).getAmbienteAfetado());
    }

    @Test
    void salvarRequisicaoGuardaOsCamposDaRequisicao() {
        Requisicao requisicao = ChamadoFactory.criarRequisicao("Instalar Excel", "Preciso do Excel",
                "Teste", Setor.FINANCEIRO, Categoria.SOFTWARE, "Planilhas de cobrança", false);

        Long id = repository.salvar(requisicao);
        Chamado lido = repository.buscarPorId(id).orElseThrow();

        assertInstanceOf(Requisicao.class, lido);
        assertEquals("Planilhas de cobrança", ((Requisicao) lido).getJustificativaNegocio());
        assertFalse(((Requisicao) lido).isPrecisaAprovacao());
        assertEquals(Boolean.TRUE, ((Requisicao) lido).getAprovado());
    }

    @Test
    void atualizarGravaATriagemEAMudancaDeStatus() {
        Incidente incidente = novoIncidente("Sem internet");
        repository.salvar(incidente);

        incidente.classificarPrioridade(Prioridade.ALTA);
        incidente.iniciarAtendimento();
        boolean atualizou = repository.atualizar(incidente);
        Chamado lido = repository.buscarPorId(incidente.getId()).orElseThrow();

        assertTrue(atualizou);
        assertEquals(Prioridade.ALTA, lido.getPrioridade());
        assertEquals(StatusChamado.EM_ANDAMENTO, lido.getStatus());
        assertEquals(incidente.getPrazoLimite(), lido.getPrazoLimite());
    }

    @Test
    void atualizarChamadoQueNaoExisteDevolveFalse() {
        Incidente incidente = novoIncidente("Nunca salvo");
        incidente.setId(9999L);

        assertFalse(repository.atualizar(incidente));
    }

    @Test
    void buscarIdQueNaoExisteDevolveVazio() {
        assertTrue(repository.buscarPorId(9999L).isEmpty());
    }

    @Test
    void listarTodosTrazDoMaisNovoParaOMaisAntigo() {
        Incidente primeiro = novoIncidente("Primeiro");
        Incidente segundo = novoIncidente("Segundo");
        repository.salvar(primeiro);
        repository.salvar(segundo);

        List<Chamado> todos = repository.listarTodos();

        assertEquals(2, todos.size());
        assertEquals("Segundo", todos.get(0).getTitulo());
        assertEquals("Primeiro", todos.get(1).getTitulo());
    }

    @Test
    void chamadoConcluidoComPrioridadeConsegueSerLidoDoBanco() {
        Incidente incidente = novoIncidente("Já resolvido");
        repository.salvar(incidente);
        incidente.classificarPrioridade(Prioridade.MEDIA);
        incidente.iniciarAtendimento();
        incidente.concluir();
        repository.atualizar(incidente);

        Chamado lido = repository.buscarPorId(incidente.getId()).orElseThrow();

        assertEquals(StatusChamado.CONCLUIDO, lido.getStatus());
        assertEquals(Prioridade.MEDIA, lido.getPrioridade());
    }
}