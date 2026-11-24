package com.suporti.suporti.service;

import com.suporti.suporti.model.Chamado;
import com.suporti.suporti.model.Requisicao;
import com.suporti.suporti.model.enums.Categoria;
import com.suporti.suporti.model.enums.Prioridade;
import com.suporti.suporti.model.enums.Setor;
import com.suporti.suporti.model.enums.TipoChamado;
import com.suporti.suporti.model.exception.ChamadoNaoEncontradoException;
import com.suporti.suporti.model.exception.DadosChamadoInvalidosException;
import com.suporti.suporti.model.factory.ChamadoFactory;
import com.suporti.suporti.repository.ChamadoRepository;
import com.suporti.suporti.service.filtro.FiltroAguardandoTriagem;
import com.suporti.suporti.service.filtro.FiltroChamado;
import com.suporti.suporti.service.filtro.FiltroFilaDeTrabalho;
import com.suporti.suporti.service.filtro.FiltroPorSetor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Casos de uso do SuporTI: é o que as telas (controllers) chamam.
 *
 * Cada método segue o mesmo roteiro:
 * 1. busca o chamado no banco (repository);
 * 2. pede ao próprio Chamado para mudar (as regras ficam no model);
 * 3. grava a mudança no banco (repository).
 */
@Service
public class ChamadoService {

    private final ChamadoRepository repository;

    public ChamadoService(ChamadoRepository repository) {
        this.repository = repository;
    }

    // ==========================================
    // SOLICITANTE
    // ==========================================

    /**
     * Abre um chamado novo. Ele nasce ABERTO e sem prioridade, e vai para a triagem.
     *
     * @param flagEspecifica    Incidente: sistema indisponível? / Requisição: precisa de aprovação?
     * @param detalheEspecifico Incidente: ambiente afetado / Requisição: justificativa
     */
    public Chamado abrir(TipoChamado tipo, String titulo, String descricao, String solicitante,
                         Setor setor, Categoria categoria,
                         boolean flagEspecifica, String detalheEspecifico) {
        Chamado chamado = ChamadoFactory.criarChamado(tipo, titulo, descricao, solicitante,
                setor, categoria, flagEspecifica, detalheEspecifico);
        repository.salvar(chamado);
        return chamado;
    }

    /** Tela do solicitante. Setor nulo = todos os setores. */
    public List<Chamado> listarPorSetor(Setor setor) {
        return listar(new FiltroPorSetor(setor));
    }

    // ==========================================
    // TÉCNICO: LISTAS
    // ==========================================

    /** Aba "Aguardando triagem". */
    public List<Chamado> listarAguardandoTriagem() {
        return listar(new FiltroAguardandoTriagem());
    }

    /** Aba "Fila de trabalho". */
    public List<Chamado> listarFilaDeTrabalho() {
        return listar(new FiltroFilaDeTrabalho());
    }

    /**
     * Aplica qualquer filtro (padrão Strategy): o service não precisa saber qual é.
     */
    public List<Chamado> listar(FiltroChamado filtro) {
        return filtro.aplicar(repository.listarTodos());
    }

    /** Busca um chamado ou lança erro se o número não existir. */
    public Chamado buscar(Long id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new ChamadoNaoEncontradoException(id));
    }

    // ==========================================
    // TÉCNICO: AÇÕES
    // ==========================================

    /** Triagem: define a prioridade, e com ela o prazo. */
    public Chamado classificarPrioridade(Long id, Prioridade prioridade) {
        Chamado chamado = buscar(id);
        chamado.classificarPrioridade(prioridade);
        repository.atualizar(chamado);
        return chamado;
    }

    /** Botão "Em andamento". */
    public Chamado iniciarAtendimento(Long id) {
        Chamado chamado = buscar(id);
        chamado.iniciarAtendimento();
        repository.atualizar(chamado);
        return chamado;
    }

    /** Botão "Concluir". */
    public Chamado concluir(Long id) {
        Chamado chamado = buscar(id);
        chamado.concluir();
        repository.atualizar(chamado);
        return chamado;
    }

    /** Botão "Cancelar": o motivo é obrigatório. */
    public Chamado cancelar(Long id, String motivo) {
        Chamado chamado = buscar(id);
        chamado.cancelar(motivo);
        repository.atualizar(chamado);
        return chamado;
    }

    /** Aprova uma requisição que precisa de aprovação, liberando o início do atendimento. */
    public Chamado aprovarRequisicao(Long id) {
        Chamado chamado = buscar(id);
        if (!(chamado instanceof Requisicao requisicao)) {
            throw new DadosChamadoInvalidosException("tipo", "Só requisições podem ser aprovadas.");
        }
        requisicao.aprovar();
        repository.atualizar(requisicao);
        return requisicao;
    }
}