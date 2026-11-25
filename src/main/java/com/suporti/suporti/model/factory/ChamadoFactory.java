package com.suporti.suporti.model.factory;

import com.suporti.suporti.model.Chamado;
import com.suporti.suporti.model.Incidente;
import com.suporti.suporti.model.Requisicao;
import com.suporti.suporti.model.enums.Categoria;
import com.suporti.suporti.model.enums.Prioridade;
import com.suporti.suporti.model.enums.StatusChamado;
import com.suporti.suporti.model.enums.Setor;
import com.suporti.suporti.model.enums.TipoChamado;
import com.suporti.suporti.model.exception.DadosChamadoInvalidosException;
import com.suporti.suporti.model.exception.TransicaoStatusInvalidaException;

import java.time.LocalDateTime;

/**
 * Padrão Criacional Factory Method / Simple Factory para a criação padronizada de Chamados.
 * 
 * Justificativa técnica (Requisito 4 de LP2):
 * - Isola a complexidade de criação e instanciação das subclasses concretas (Incidente e Requisição).
 * - Garante que regras de inicialização (dataAbertura, status inicial ABERTO, cálculo automático de SLA)
 *   sejam aplicadas uniformemente em toda a aplicação.
 * - Desacopla as camadas de Controller e Service dos detalhes de implementação das subclasses concretas,
 *   facilitando a extensão futura com novos tipos de chamado sem alterar código cliente.
 */
public class ChamadoFactory {

    /**
     * Cria um incidente novo, inicialmente aberto e sem prioridade.
     *
     * @param titulo título resumido do incidente
     * @param descricao descrição do problema
     * @param solicitante nome de quem abriu o incidente
     * @param setor setor de origem
     * @param categoria categoria técnica
     * @param sistemaIndisponivel indica se o sistema está indisponível
     * @param ambienteAfetado ambiente em que o problema ocorre; se vazio, a classe usa {@code Geral}
     * @return incidente criado com a data de abertura atual
     * @throws DadosChamadoInvalidosException se algum dado obrigatório for inválido
     */
    public static Incidente criarIncidente(String titulo, String descricao, String solicitante,
                                          Setor setor, Categoria categoria,
                                          boolean sistemaIndisponivel, String ambienteAfetado) {
        return new Incidente(titulo, descricao, solicitante, setor, categoria,
                sistemaIndisponivel, ambienteAfetado);
    }

    /**
     * Cria uma requisição nova, inicialmente aberta e sem prioridade.
     *
     * @param titulo título resumido da requisição
     * @param descricao descrição do que está sendo solicitado
     * @param solicitante nome de quem abriu a requisição
     * @param setor setor de origem
     * @param categoria categoria técnica
     * @param justificativaNegocio justificativa para a solicitação
     * @param precisaAprovacao indica se a requisição depende de aprovação gerencial
     * @return requisição criada com a data de abertura atual
     * @throws DadosChamadoInvalidosException se algum dado obrigatório for inválido
     */
    public static Requisicao criarRequisicao(String titulo, String descricao, String solicitante,
                                            Setor setor, Categoria categoria,
                                            String justificativaNegocio, boolean precisaAprovacao) {
        return new Requisicao(titulo, descricao, solicitante, setor, categoria,
                justificativaNegocio, precisaAprovacao);
    }

    /**
     * Cria um chamado do tipo solicitado, encaminhando os dados específicos para a criação
     * de um incidente ou de uma requisição.
     *
     * @param tipo tipo concreto a criar
     * @param titulo título resumido do chamado
     * @param descricao descrição do problema ou solicitação
     * @param solicitante nome de quem abriu o chamado
     * @param setor setor de origem
     * @param categoria categoria técnica
     * @param flagEspecifica indica indisponibilidade para incidente ou necessidade de aprovação para requisição
     * @param detalheEspecifico ambiente afetado do incidente ou justificativa da requisição
     * @return chamado criado como {@link Incidente} ou {@link Requisicao}
     * @throws DadosChamadoInvalidosException se o tipo for nulo ou algum dado obrigatório for inválido
     */
    public static Chamado criarChamado(TipoChamado tipo, String titulo, String descricao, String solicitante,
                                       Setor setor, Categoria categoria,
                                       boolean flagEspecifica, String detalheEspecifico) {
        if (tipo == null) {
            throw new DadosChamadoInvalidosException("tipo", "O tipo de chamado deve ser informado (Incidente ou Requisição).");
        }

        switch (tipo) {
            case INCIDENTE:
                return criarIncidente(titulo, descricao, solicitante, setor, categoria,
                        flagEspecifica, detalheEspecifico);
            case REQUISICAO:
                return criarRequisicao(titulo, descricao, solicitante, setor, categoria,
                        detalheEspecifico, flagEspecifica);
            default:
                throw new DadosChamadoInvalidosException("tipo", "Tipo de chamado não suportado: " + tipo);
        }
    }

    /**
     * Reconstrói uma instância de {@link Chamado} a partir dos dados persistidos pelo repositório.
     *
     * O objeto é inicialmente criado como aberto, recebe a prioridade e é levado ao status
     * persistido usando as regras de domínio. Por fim, as datas e demais informações salvas
     * são restauradas.
     *
     * @param id identificador persistido do chamado
     * @param tipo tipo concreto a reconstruir
     * @param titulo título salvo
     * @param descricao descrição salva
     * @param solicitante nome de quem abriu o chamado
     * @param setor setor de origem
     * @param categoria categoria técnica
     * @param prioridade prioridade salva; pode ser nula se ainda não houve triagem
     * @param status status salvo; se nulo, o chamado permanece aberto
     * @param dataAbertura data e hora de abertura; se nula, o construtor usa a data atual
     * @param prazoLimite prazo salvo; se nulo, mantém o prazo calculado a partir da prioridade
     * @param dataFechamento data e hora de fechamento; pode ser nula
     * @param motivoCancelamento justificativa salva para o cancelamento; pode ser nula
     * @param flagEspecifica indisponibilidade para incidente ou necessidade de aprovação para requisição
     * @param detalheEspecifico ambiente afetado do incidente ou justificativa da requisição
     * @param aprovado estado de aprovação da requisição; ignorado para incidentes
     * @return chamado reconstruído como {@link Incidente} ou {@link Requisicao}
     * @throws DadosChamadoInvalidosException se o tipo ou os dados obrigatórios forem inválidos
     * @throws TransicaoStatusInvalidaException se as regras de domínio impedirem a restauração do status
     */
    public static Chamado reconstruir(Long id, TipoChamado tipo, String titulo, String descricao,
                                      String solicitante, Setor setor, Categoria categoria,
                                      Prioridade prioridade, StatusChamado status,
                                      LocalDateTime dataAbertura, LocalDateTime prazoLimite,
                                      LocalDateTime dataFechamento, String motivoCancelamento,
                                      boolean flagEspecifica, String detalheEspecifico, Boolean aprovado) {
        if (tipo == null) {
            throw new DadosChamadoInvalidosException("tipo", "Tipo de chamado nulo ao reconstruir objeto.");
        }

        // 1. Recria o chamado como ABERTO, que é o estado em que todo chamado nasce
        Chamado chamado;
        if (tipo == TipoChamado.INCIDENTE) {
            chamado = new Incidente(id, titulo, descricao, solicitante, setor, categoria,
                    StatusChamado.ABERTO, dataAbertura, flagEspecifica, detalheEspecifico);
        } else {
            chamado = new Requisicao(id, titulo, descricao, solicitante, setor, categoria,
                    StatusChamado.ABERTO, dataAbertura, detalheEspecifico, flagEspecifica, aprovado);
        }

        // 2. Refaz a triagem enquanto o chamado ainda está aberto
        if (prioridade != null) {
            chamado.classificarPrioridade(prioridade);
        }

        // 3. Leva o chamado até o status salvo usando as próprias regras do domínio
        if (status == StatusChamado.EM_ANDAMENTO) {
            chamado.iniciarAtendimento();
        } else if (status == StatusChamado.CONCLUIDO) {
            chamado.concluir();
        } else if (status == StatusChamado.CANCELADO) {
            chamado.cancelar(motivoCancelamento != null ? motivoCancelamento : "Motivo não registrado");
        }

        // 4. Restaura as datas exatamente como estavam no banco
        if (prazoLimite != null) {
            chamado.setPrazoLimite(prazoLimite);
        }
        if (dataFechamento != null) {
            chamado.setDataFechamento(dataFechamento);
        }

        return chamado;
    }

}
