package com.suporti.suporti.model;

import com.suporti.suporti.model.enums.Categoria;
import com.suporti.suporti.model.enums.Prioridade;
import com.suporti.suporti.model.enums.Setor;
import com.suporti.suporti.model.enums.StatusChamado;
import com.suporti.suporti.model.enums.TipoChamado;
import com.suporti.suporti.model.exception.DadosChamadoInvalidosException;
import com.suporti.suporti.model.exception.TransicaoStatusInvalidaException;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Classe base abstrata que modela um Chamado de suporte de TI.
 * Aplica os conceitos de POO: abstração, encapsulamento rígido e polimorfismo.
 */
public abstract class Chamado implements SlaCalculavel, Comparable<Chamado> {

    private Long id;
    private String titulo;
    private String descricao;
    private String solicitante;
    private Setor setor;
    private Categoria categoria;
    private Prioridade prioridade;
    private StatusChamado status;
    private LocalDateTime dataAbertura;
    private LocalDateTime prazoLimite;
    private LocalDateTime dataFechamento;
    private String motivoCancelamento;

    /**
     * Construtor protegido para instanciação através de subclasses ou Factory.
     */
    protected Chamado(Long id, String titulo, String descricao, String solicitante,
                      Setor setor, Categoria categoria, Prioridade prioridade,
                      StatusChamado status, LocalDateTime dataAbertura) {
        validarCamposObrigatorios(titulo, descricao, solicitante, setor, categoria, prioridade);

        this.id = id;
        this.titulo = titulo.trim();
        this.descricao = descricao.trim();
        this.solicitante = solicitante.trim();
        this.setor = setor;
        this.categoria = categoria;
        this.prioridade = prioridade;
        this.status = status != null ? status : StatusChamado.ABERTO;
        this.dataAbertura = dataAbertura != null ? dataAbertura : LocalDateTime.now();
        this.prazoLimite = calcularPrazoLimite(this.dataAbertura);
    }

    /**
     * Validação dos dados essenciais do chamado garantindo a integridade do domínio.
     */
    private void validarCamposObrigatorios(String titulo, String descricao, String solicitante,
                                           Setor setor, Categoria categoria, Prioridade prioridade) {
        if (titulo == null || titulo.trim().isEmpty()) {
            throw new DadosChamadoInvalidosException("titulo", "O título do chamado não pode ser nulo ou vazio.");
        }
        if (descricao == null || descricao.trim().isEmpty()) {
            throw new DadosChamadoInvalidosException("descricao", "A descrição do chamado não pode ser nula ou vazia.");
        }
        if (solicitante == null || solicitante.trim().isEmpty()) {
            throw new DadosChamadoInvalidosException("solicitante", "O solicitante deve ser informado.");
        }
        if (setor == null) {
            throw new DadosChamadoInvalidosException("setor", "O setor de origem é obrigatório.");
        }
        if (categoria == null) {
            throw new DadosChamadoInvalidosException("categoria", "A categoria é obrigatória.");
        }
        if (prioridade == null) {
            throw new DadosChamadoInvalidosException("prioridade", "A prioridade deve ser informada.");
        }
    }

    /**
     * Método abstrato a ser implementado pelas subclasses para identificar o tipo concreto.
     */
    public abstract TipoChamado getTipo();

    // ==========================================
    // REGRAS DE NEGÓCIO E MUDANÇA DE ESTADO
    // ==========================================

    /**
     * Inicia o atendimento do chamado pelo técnico (transição de ABERTO para EM_ANDAMENTO).
     */
    public void iniciarAtendimento() {
        if (this.status != StatusChamado.ABERTO) {
            throw new TransicaoStatusInvalidaException(this.status, StatusChamado.EM_ANDAMENTO);
        }
        this.status = StatusChamado.EM_ANDAMENTO;
    }

    /**
     * Conclui o atendimento do chamado registrando a data de fechamento.
     */
    public void concluir() {
        if (this.status == StatusChamado.CONCLUIDO || this.status == StatusChamado.CANCELADO) {
            throw new TransicaoStatusInvalidaException(this.status, StatusChamado.CONCLUIDO);
        }
        this.status = StatusChamado.CONCLUIDO;
        this.dataFechamento = LocalDateTime.now();
    }

    /**
     * Cancela o chamado informando um motivo. Não é permitido cancelar chamado já concluído.
     */
    public void cancelar(String motivo) {
        if (this.status == StatusChamado.CONCLUIDO) {
            throw new TransicaoStatusInvalidaException("Não é permitido cancelar um chamado que já foi concluído.");
        }
        if (this.status == StatusChamado.CANCELADO) {
            throw new TransicaoStatusInvalidaException("O chamado já se encontra cancelado.");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new DadosChamadoInvalidosException("motivoCancelamento", "O motivo do cancelamento deve ser informado.");
        }
        this.status = StatusChamado.CANCELADO;
        this.motivoCancelamento = motivo.trim();
        this.dataFechamento = LocalDateTime.now();
    }

    /**
     * Atualiza a prioridade durante a triagem e recalcula polimorficamente o prazo limite.
     */
    public void classificarPrioridade(Prioridade novaPrioridade) {
        if (novaPrioridade == null) {
            throw new DadosChamadoInvalidosException("prioridade", "Nova prioridade não pode ser nula.");
        }
        if (this.status.isFinalizado()) {
            throw new TransicaoStatusInvalidaException("Não é possível alterar a prioridade de um chamado já finalizado.");
        }
        this.prioridade = novaPrioridade;
        this.prazoLimite = calcularPrazoLimite(this.dataAbertura);
    }

    /**
     * Verifica se o chamado ultrapassou o SLA estipulado.
     */
    public boolean isAtrasado() {
        if (this.prazoLimite == null) {
            return false;
        }
        if (this.dataFechamento != null) {
            return this.dataFechamento.isAfter(this.prazoLimite);
        }
        return LocalDateTime.now().isAfter(this.prazoLimite);
    }

    /**
     * Ordenação natural para a fila de atendimento do técnico:
     * 1. Maior prioridade primeiro (ALTA > MEDIA > BAIXA);
     * 2. Menor prazo limite (quem vence antes tem preferência);
     * 3. Data de abertura mais antiga.
     */
    @Override
    public int compareTo(Chamado outro) {
        if (outro == null) {
            return -1;
        }
        // Compara nível de prioridade (descendente: maior nível primeiro)
        int compPrioridade = Integer.compare(outro.getPrioridade().getNivel(), this.getPrioridade().getNivel());
        if (compPrioridade != 0) {
            return compPrioridade;
        }

        // Compara prazo limite (ascendente: menor data primeiro)
        if (this.prazoLimite != null && outro.getPrazoLimite() != null) {
            int compPrazo = this.prazoLimite.compareTo(outro.getPrazoLimite());
            if (compPrazo != 0) {
                return compPrazo;
            }
        }

        // Desempate por data de abertura (FIFO)
        if (this.dataAbertura != null && outro.getDataAbertura() != null) {
            return this.dataAbertura.compareTo(outro.getDataAbertura());
        }

        return 0;
    }

    // ==========================================
    // GETTERS E SETTERS ENCAPSULADOS
    // ==========================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        if (titulo == null || titulo.trim().isEmpty()) {
            throw new DadosChamadoInvalidosException("titulo", "O título não pode ser vazio.");
        }
        this.titulo = titulo.trim();
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        if (descricao == null || descricao.trim().isEmpty()) {
            throw new DadosChamadoInvalidosException("descricao", "A descrição não pode ser vazia.");
        }
        this.descricao = descricao.trim();
    }

    public String getSolicitante() {
        return solicitante;
    }

    public Setor getSetor() {
        return setor;
    }

    public void setSetor(Setor setor) {
        if (setor == null) {
            throw new DadosChamadoInvalidosException("setor", "O setor não pode ser nulo.");
        }
        this.setor = setor;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        if (categoria == null) {
            throw new DadosChamadoInvalidosException("categoria", "A categoria não pode ser nula.");
        }
        this.categoria = categoria;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public StatusChamado getStatus() {
        return status;
    }

    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }

    public LocalDateTime getPrazoLimite() {
        return prazoLimite;
    }

    public LocalDateTime getDataFechamento() {
        return dataFechamento;
    }

    public void setDataFechamento(LocalDateTime dataFechamento) {
        this.dataFechamento = dataFechamento;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public void setPrazoLimite(LocalDateTime prazoLimite) {
        this.prazoLimite = prazoLimite;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chamado chamado = (Chamado) o;
        return Objects.equals(id, chamado.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("[%s #%s] %s (Prioridade: %s, Status: %s, Prazo: %s)",
                getTipo(), id != null ? id : "Novo", titulo, prioridade, status, prazoLimite);
    }
}
