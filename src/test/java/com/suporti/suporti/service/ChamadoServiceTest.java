package com.suporti.suporti.service;

import com.suporti.suporti.model.Chamado;
import com.suporti.suporti.model.enums.Categoria;
import com.suporti.suporti.model.enums.Prioridade;
import com.suporti.suporti.model.enums.Setor;
import com.suporti.suporti.model.enums.StatusChamado;
import com.suporti.suporti.model.enums.TipoChamado;
import com.suporti.suporti.model.exception.ChamadoNaoEncontradoException;
import com.suporti.suporti.model.exception.DadosChamadoInvalidosException;
import com.suporti.suporti.model.exception.TransicaoStatusInvalidaException;
import com.suporti.suporti.repository.ChamadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do ChamadoService sem banco de dados: o repository é trocado por um
 * "falso" que guarda os chamados num mapa em memória. Assim os testes
 * conferem só as regras do service e rodam rápido.
 */
class ChamadoServiceTest {

    /** Repository falso: mesma "cara" do verdadeiro, mas guarda tudo num Map. */
    static class RepositoryEmMemoria extends ChamadoRepository {
        private final Map<Long, Chamado> chamados = new HashMap<>();
        private long proximoId = 1;

        RepositoryEmMemoria() {
            super(null); // não usa JdbcTemplate
        }

        @Override
        public Long salvar(Chamado chamado) {
            chamado.setId(proximoId++);
            chamados.put(chamado.getId(), chamado);
            return chamado.getId();
        }

        @Override
        public boolean atualizar(Chamado chamado) {
            return chamados.replace(chamado.getId(), chamado) != null;
        }

        @Override
        public Optional<Chamado> buscarPorId(Long id) {
            return Optional.ofNullable(chamados.get(id));
        }

        @Override
        public List<Chamado> listarTodos() {
            return new ArrayList<>(chamados.values());
        }
    }

    private ChamadoService service;

    @BeforeEach
    void criarService() {
        service = new ChamadoService(new RepositoryEmMemoria());
    }

    private Chamado abrirIncidente(Setor setor) {
        return service.abrir(TipoChamado.INCIDENTE, "Sem internet", "Nenhum PC acessa a internet",
                "Teste", setor, Categoria.REDE, false, "Escritório");
    }

    @Test
    void abrirChamadoGeraIdEMandaParaATriagem() {
        Chamado chamado = abrirIncidente(Setor.MARKETING);

        assertNotNull(chamado.getId());
        assertEquals(StatusChamado.ABERTO, chamado.getStatus());
        assertEquals(List.of(chamado), service.listarAguardandoTriagem());
        assertTrue(service.listarFilaDeTrabalho().isEmpty());
    }

    @Test
    void classificarPrioridadeTiraDaTriagemEColocaNaFila() {
        Chamado chamado = abrirIncidente(Setor.MARKETING);

        service.classificarPrioridade(chamado.getId(), Prioridade.ALTA);

        assertTrue(service.listarAguardandoTriagem().isEmpty());
        assertEquals(List.of(chamado), service.listarFilaDeTrabalho());
        assertNotNull(service.buscar(chamado.getId()).getPrazoLimite());
    }

    @Test
    void fluxoCompletoAteConcluirTiraDaFila() {
        Chamado chamado = abrirIncidente(Setor.TI);

        service.classificarPrioridade(chamado.getId(), Prioridade.MEDIA);
        service.iniciarAtendimento(chamado.getId());
        service.concluir(chamado.getId());

        assertEquals(StatusChamado.CONCLUIDO, service.buscar(chamado.getId()).getStatus());
        assertTrue(service.listarFilaDeTrabalho().isEmpty());
    }

    @Test
    void cancelarSemMotivoLancaExcecao() {
        Chamado chamado = abrirIncidente(Setor.TI);

        assertThrows(DadosChamadoInvalidosException.class,
                () -> service.cancelar(chamado.getId(), "  "));
    }

    @Test
    void concluirChamadoCanceladoLancaExcecao() {
        Chamado chamado = abrirIncidente(Setor.TI);
        service.cancelar(chamado.getId(), "Aberto por engano");

        assertThrows(TransicaoStatusInvalidaException.class,
                () -> service.concluir(chamado.getId()));
    }

    @Test
    void buscarIdInexistenteLancaExcecao() {
        assertThrows(ChamadoNaoEncontradoException.class, () -> service.buscar(999L));
    }

    @Test
    void listarPorSetorMostraSoOSetorEscolhido() {
        Chamado marketing = abrirIncidente(Setor.MARKETING);
        abrirIncidente(Setor.FINANCEIRO);

        assertEquals(List.of(marketing), service.listarPorSetor(Setor.MARKETING));
        assertEquals(2, service.listarPorSetor(null).size());
    }

    @Test
    void aprovarIncidenteLancaExcecao() {
        Chamado incidente = abrirIncidente(Setor.TI);

        assertThrows(DadosChamadoInvalidosException.class,
                () -> service.aprovarRequisicao(incidente.getId()));
    }
}