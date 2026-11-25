package com.suporti.suporti.service;

import com.suporti.suporti.model.Chamado;
import com.suporti.suporti.model.Requisicao;
import com.suporti.suporti.model.enums.Categoria;
import com.suporti.suporti.model.enums.Prioridade;
import com.suporti.suporti.model.enums.Setor;
import com.suporti.suporti.model.enums.TipoChamado;
import com.suporti.suporti.model.exception.ChamadoNaoEncontradoException;
import com.suporti.suporti.model.exception.DadosChamadoInvalidosException;
import com.suporti.suporti.model.exception.TransicaoStatusInvalidaException;
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
     * @param tipo tipo do chamado: incidente ou requisição
     * @param titulo título resumido do chamado
     * @param descricao descrição detalhada do problema ou solicitação
     * @param solicitante nome de quem abriu o chamado
     * @param setor setor de origem do chamado
     * @param categoria categoria técnica do chamado
     * @param flagEspecifica    Incidente: sistema indisponível? / Requisição: precisa de aprovação?
     * @param detalheEspecifico Incidente: ambiente afetado / Requisição: justificativa
     * @return chamado criado e salvo, incluindo o identificador gerado
     * @throws DadosChamadoInvalidosException se o tipo ou algum dado obrigatório for inválido
     */
    public Chamado abrir(TipoChamado tipo, String titulo, String descricao, String solicitante,
                         Setor setor, Categoria categoria,
                         boolean flagEspecifica, String detalheEspecifico) {
        Chamado chamado = ChamadoFactory.criarChamado(tipo, titulo, descricao, solicitante,
                setor, categoria, flagEspecifica, detalheEspecifico);
        repository.salvar(chamado);
        return chamado;
    }

    /**
     * Lista os chamados visíveis para o solicitante, opcionalmente filtrados por setor.
     *
     * @param setor setor desejado; {@code null} retorna chamados de todos os setores
     * @return chamados ordenados da abertura mais recente para a mais antiga
     */
    public List<Chamado> listarPorSetor(Setor setor) {
        return listar(new FiltroPorSetor(setor));
    }

    // ==========================================
    // TÉCNICO: LISTAS
    // ==========================================

    /**
     * Lista os chamados abertos que ainda não têm prioridade definida.
     *
     * @return chamados aguardando triagem, ordenados conforme as regras do filtro
     */
    public List<Chamado> listarAguardandoTriagem() {
        return listar(new FiltroAguardandoTriagem());
    }

    /**
     * Lista os chamados priorizados que ainda não foram finalizados.
     *
     * @return chamados da fila de trabalho, ordenados pelo prazo e pela prioridade
     */
    public List<Chamado> listarFilaDeTrabalho() {
        return listar(new FiltroFilaDeTrabalho());
    }

    /**
     * Aplica o filtro recebido à lista de chamados persistidos, usando o padrão Strategy.
     *
     * @param filtro estratégia que seleciona e ordena os chamados
     * @return chamados resultantes da aplicação do filtro
     */
    public List<Chamado> listar(FiltroChamado filtro) {
        return filtro.aplicar(repository.listarTodos());
    }

    /**
     * Busca um chamado pelo identificador.
     *
     * @param id identificador do chamado
     * @return chamado encontrado
     * @throws ChamadoNaoEncontradoException se não existir chamado com esse identificador
     */
    public Chamado buscar(Long id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new ChamadoNaoEncontradoException(id));
    }

    // ==========================================
    // TÉCNICO: AÇÕES
    // ==========================================

    /**
     * Define a prioridade durante a triagem e recalcula o prazo de SLA. A prioridade não inicia
     * o atendimento nem altera o status do chamado.
     *
     * @param id identificador do chamado
     * @param prioridade prioridade a ser atribuída
     * @return chamado atualizado
     * @throws ChamadoNaoEncontradoException se o chamado não existir
     * @throws DadosChamadoInvalidosException se a prioridade for nula
     * @throws TransicaoStatusInvalidaException se o chamado já estiver finalizado
     */
    public Chamado classificarPrioridade(Long id, Prioridade prioridade) {
        Chamado chamado = buscar(id);
        chamado.classificarPrioridade(prioridade);
        repository.atualizar(chamado);
        return chamado;
    }

    /**
     * Inicia o atendimento do chamado e persiste a transição para {@code EM_ANDAMENTO}.
     *
     * @param id identificador do chamado
     * @return chamado atualizado
     * @throws ChamadoNaoEncontradoException se o chamado não existir
     * @throws TransicaoStatusInvalidaException se a transição não for válida para o status atual
     */
    public Chamado iniciarAtendimento(Long id) {
        Chamado chamado = buscar(id);
        chamado.iniciarAtendimento();
        repository.atualizar(chamado);
        return chamado;
    }

    /**
     * Conclui o chamado e persiste a data de fechamento.
     *
     * @param id identificador do chamado
     * @return chamado atualizado
     * @throws ChamadoNaoEncontradoException se o chamado não existir
     * @throws TransicaoStatusInvalidaException se o chamado já estiver concluído ou cancelado
     */
    public Chamado concluir(Long id) {
        Chamado chamado = buscar(id);
        chamado.concluir();
        repository.atualizar(chamado);
        return chamado;
    }

    /**
     * Cancela o chamado, registra o motivo informado e persiste a data de fechamento.
     *
     * @param id identificador do chamado
     * @param motivo justificativa do cancelamento; não pode ser nula nem vazia
     * @return chamado atualizado
     * @throws ChamadoNaoEncontradoException se o chamado não existir
     * @throws DadosChamadoInvalidosException se o motivo for nulo ou vazio
     * @throws TransicaoStatusInvalidaException se o chamado já estiver finalizado
     */
    public Chamado cancelar(Long id, String motivo) {
        Chamado chamado = buscar(id);
        chamado.cancelar(motivo);
        repository.atualizar(chamado);
        return chamado;
    }

    /**
     * Aprova uma requisição que exige aprovação e persiste a alteração. A aprovação, por si só,
     * não inicia o atendimento.
     *
     * @param id identificador da requisição
     * @return requisição aprovada
     * @throws ChamadoNaoEncontradoException se o chamado não existir
     * @throws DadosChamadoInvalidosException se o chamado não for uma requisição
     * @throws TransicaoStatusInvalidaException se a requisição já estiver finalizada
     */
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