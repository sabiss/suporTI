package com.suporti.suporti.model;

import com.suporti.suporti.model.enums.Categoria;
import com.suporti.suporti.model.enums.Prioridade;
import com.suporti.suporti.model.enums.Setor;
import com.suporti.suporti.model.enums.StatusChamado;
import com.suporti.suporti.model.exception.DadosChamadoInvalidosException;
import com.suporti.suporti.model.exception.TransicaoStatusInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Domínio e Ciclo de Vida do Chamado")
class ChamadoCicloDeVidaTest {

    private Incidente criarIncidenteExemplo() {
        return new Incidente(
                "Falha na rede do 2º andar",
                "Switches sem conexão e sem acesso à internet",
                "Lucas Silva",
                Setor.OPERACIONAL,
                Categoria.REDE,
                Prioridade.ALTA,
                true,
                "Produção"
        );
    }

    @Test
    @DisplayName("Deve inicializar chamado com status ABERTO e data de abertura definida")
    void deveInicializarChamadoComStatusAberto() {
        Chamado chamado = criarIncidenteExemplo();

        assertNotNull(chamado.getDataAbertura());
        assertNotNull(chamado.getPrazoLimite());
        assertEquals(StatusChamado.ABERTO, chamado.getStatus());
        assertNull(chamado.getDataFechamento());
        assertNull(chamado.getMotivoCancelamento());
    }

    @Test
    @DisplayName("Deve executar ciclo completo: ABERTO -> EM_ANDAMENTO -> CONCLUIDO")
    void deveExecutarCicloCompletoComSucesso() {
        Chamado chamado = criarIncidenteExemplo();

        chamado.iniciarAtendimento();
        assertEquals(StatusChamado.EM_ANDAMENTO, chamado.getStatus());

        chamado.concluir();
        assertEquals(StatusChamado.CONCLUIDO, chamado.getStatus());
        assertNotNull(chamado.getDataFechamento());
        assertTrue(chamado.getStatus().isFinalizado());
    }

    @Test
    @DisplayName("Deve permitir cancelamento de chamado aberto com justificativa")
    void devePermitirCancelamentoComJustificativa() {
        Chamado chamado = criarIncidenteExemplo();

        chamado.cancelar("Problema resolvido reiniciando o roteador pelo próprio setor");

        assertEquals(StatusChamado.CANCELADO, chamado.getStatus());
        assertNotNull(chamado.getDataFechamento());
        assertTrue(chamado.getMotivoCancelamento().contains("reiniciando"));
        assertTrue(chamado.getStatus().isFinalizado());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar iniciar atendimento de chamado já em andamento ou finalizado")
    void deveLancarExcecaoTransicaoInvalidaIniciarAtendimento() {
        Chamado chamado = criarIncidenteExemplo();
        chamado.iniciarAtendimento();

        // Tenta iniciar novamente
        TransicaoStatusInvalidaException ex1 = assertThrows(TransicaoStatusInvalidaException.class, 
                chamado::iniciarAtendimento);
        assertEquals(StatusChamado.EM_ANDAMENTO, ex1.getStatusOrigem());

        chamado.concluir();
        // Tenta iniciar após concluído
        assertThrows(TransicaoStatusInvalidaException.class, chamado::iniciarAtendimento);
    }

    @Test
    @DisplayName("Deve impedir cancelamento de chamado que já foi concluído")
    void deveImpedirCancelamentoDeChamadoConcluido() {
        Chamado chamado = criarIncidenteExemplo();
        chamado.iniciarAtendimento();
        chamado.concluir();

        TransicaoStatusInvalidaException ex = assertThrows(TransicaoStatusInvalidaException.class, 
                () -> chamado.cancelar("Cancelamento tardio"));
        assertTrue(ex.getMessage().toLowerCase().contains("não é permitido cancelar um chamado que já foi concluído"));
    }

    @Test
    @DisplayName("Deve lançar exceção ao cancelar com motivo em branco")
    void deveLancarExcecaoAoCancelarSemMotivo() {
        Chamado chamado = criarIncidenteExemplo();

        assertThrows(DadosChamadoInvalidosException.class, () -> chamado.cancelar("   "));
        assertThrows(DadosChamadoInvalidosException.class, () -> chamado.cancelar(null));
    }

    @Test
    @DisplayName("Deve validar campos obrigatórios na instanciação e lançar exceção apropriada")
    void deveValidarCamposObrigatorios() {
        assertThrows(DadosChamadoInvalidosException.class, () -> 
                new Incidente("", "desc", "solic", Setor.TI, Categoria.HARDWARE, Prioridade.BAIXA, false, "Geral"));

        assertThrows(DadosChamadoInvalidosException.class, () -> 
                new Incidente("Titulo", "", "solic", Setor.TI, Categoria.HARDWARE, Prioridade.BAIXA, false, "Geral"));

        assertThrows(DadosChamadoInvalidosException.class, () -> 
                new Incidente("Titulo", "desc", null, Setor.TI, Categoria.HARDWARE, Prioridade.BAIXA, false, "Geral"));

        assertThrows(DadosChamadoInvalidosException.class, () -> 
                new Incidente("Titulo", "desc", "solic", null, Categoria.HARDWARE, Prioridade.BAIXA, false, "Geral"));

        assertThrows(DadosChamadoInvalidosException.class, () -> 
                new Incidente("Titulo", "desc", "solic", Setor.TI, null, Prioridade.BAIXA, false, "Geral"));

        assertThrows(DadosChamadoInvalidosException.class, () -> 
                new Incidente("Titulo", "desc", "solic", Setor.TI, Categoria.HARDWARE, null, false, "Geral"));
    }

    @Test
    @DisplayName("Deve atualizar prioridade e recalcular prazo limite na triagem")
    void deveAtualizarPrioridadeERecalcularPrazo() {
        Incidente incidente = new Incidente(
                "Monitor piscando",
                "Monitor secundário desliga sozinho",
                "Joana",
                Setor.RH,
                Categoria.HARDWARE,
                Prioridade.BAIXA, // SLA inicial BAIXA = 24h
                false,
                "Escritório"
        );

        LocalDateTime prazoInicial = incidente.getPrazoLimite();

        // Técnico realiza triagem e eleva prioridade para ALTA (sem indisponibilidade = 4h)
        incidente.classificarPrioridade(Prioridade.ALTA);

        assertEquals(Prioridade.ALTA, incidente.getPrioridade());
        assertTrue(incidente.getPrazoLimite().isBefore(prazoInicial),
                "O novo prazo com prioridade ALTA deve ser mais curto que o prazo inicial de prioridade BAIXA");
    }

    @Test
    @DisplayName("Deve identificar corretamente quando o chamado está com SLA atrasado")
    void deveIdentificarChamadoAtrasado() {
        Incidente incidente = criarIncidenteExemplo();

        // Força prazo limite no passado para simular estouro de SLA
        incidente.setPrazoLimite(LocalDateTime.now().minusHours(2));

        assertTrue(incidente.isAtrasado());

        // Conclui o chamado agora (fechamento posterior ao prazo limite -> concluído com atraso)
        incidente.iniciarAtendimento();
        incidente.concluir();

        assertTrue(incidente.isAtrasado(), "Chamado concluído após o prazo deve constar como atrasado no histórico");
    }
}
