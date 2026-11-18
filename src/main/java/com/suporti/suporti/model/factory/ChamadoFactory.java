package com.suporti.suporti.model.factory;

import com.suporti.suporti.model.Chamado;
import com.suporti.suporti.model.Incidente;
import com.suporti.suporti.model.Requisicao;
import com.suporti.suporti.model.enums.Categoria;
import com.suporti.suporti.model.enums.Prioridade;
import com.suporti.suporti.model.enums.Setor;
import com.suporti.suporti.model.enums.TipoChamado;
import com.suporti.suporti.model.exception.DadosChamadoInvalidosException;

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
     * Cria uma nova instância de Incidente com validações e prazos iniciais configurados.
     */
    public static Incidente criarIncidente(String titulo, String descricao, String solicitante,
                                          Setor setor, Categoria categoria, Prioridade prioridade,
                                          boolean sistemaIndisponivel, String ambienteAfetado) {
        return new Incidente(titulo, descricao, solicitante, setor, categoria, prioridade,
                sistemaIndisponivel, ambienteAfetado);
    }

    /**
     * Cria uma nova instância de Requisição com validações e prazos iniciais configurados.
     */
    public static Requisicao criarRequisicao(String titulo, String descricao, String solicitante,
                                            Setor setor, Categoria categoria, Prioridade prioridade,
                                            String justificativaNegocio, boolean precisaAprovacao) {
        return new Requisicao(titulo, descricao, solicitante, setor, categoria, prioridade,
                justificativaNegocio, precisaAprovacao);
    }

    /**
     * Método fábrica polimórfico genérico que instancia o chamado correto baseado no TipoChamado informado.
     * Útil para controllers e formulários web dinâmicos.
     */
    public static Chamado criarChamado(TipoChamado tipo, String titulo, String descricao, String solicitante,
                                       Setor setor, Categoria categoria, Prioridade prioridade,
                                       boolean flagEspecifica, String detalheEspecifico) {
        if (tipo == null) {
            throw new DadosChamadoInvalidosException("tipo", "O tipo de chamado deve ser informado (Incidente ou Requisição).");
        }

        switch (tipo) {
            case INCIDENTE:
                return criarIncidente(titulo, descricao, solicitante, setor, categoria, prioridade,
                        flagEspecifica, detalheEspecifico);
            case REQUISICAO:
                return criarRequisicao(titulo, descricao, solicitante, setor, categoria, prioridade,
                        detalheEspecifico, flagEspecifica);
            default:
                throw new DadosChamadoInvalidosException("tipo", "Tipo de chamado não suportado: " + tipo);
        }
    }

    /**
     * Reconstrói uma instância de Chamado a partir de dados persistidos (útil para o Repository/JdbcTemplate).
     */
    public static Chamado reconstruir(Long id, TipoChamado tipo, String titulo, String descricao,
                                      String solicitante, Setor setor, Categoria categoria,
                                      Prioridade prioridade, com.suporti.suporti.model.enums.StatusChamado status,
                                      LocalDateTime dataAbertura, LocalDateTime prazoLimite,
                                      LocalDateTime dataFechamento, String motivoCancelamento,
                                      boolean flagEspecifica, String detalheEspecifico, Boolean aprovado) {
        if (tipo == null) {
            throw new DadosChamadoInvalidosException("tipo", "Tipo de chamado nulo ao reconstruir objeto.");
        }

        Chamado chamado;
        if (tipo == TipoChamado.INCIDENTE) {
            chamado = new Incidente(id, titulo, descricao, solicitante, setor, categoria, prioridade,
                    status, dataAbertura, flagEspecifica, detalheEspecifico);
        } else {
            chamado = new Requisicao(id, titulo, descricao, solicitante, setor, categoria, prioridade,
                    status, dataAbertura, detalheEspecifico, flagEspecifica, aprovado);
        }

        if (prazoLimite != null) {
            chamado.setPrazoLimite(prazoLimite);
        }
        if (dataFechamento != null) {
            chamado.setDataFechamento(dataFechamento);
        }
        if (motivoCancelamento != null) {
            try {
                // Se foi cancelado, mantém o motivo
                java.lang.reflect.Field field = Chamado.class.getDeclaredField("motivoCancelamento");
                field.setAccessible(true);
                field.set(chamado, motivoCancelamento);
            } catch (Exception ignored) {
            }
        }

        return chamado;
    }
}
