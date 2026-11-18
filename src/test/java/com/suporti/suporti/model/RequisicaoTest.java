package com.suporti.suporti.model;

import com.suporti.suporti.model.enums.Categoria;
import com.suporti.suporti.model.enums.Prioridade;
import com.suporti.suporti.model.enums.Setor;
import com.suporti.suporti.model.enums.StatusChamado;
import com.suporti.suporti.model.enums.TipoChamado;
import com.suporti.suporti.model.exception.TransicaoStatusInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Regras de Negócio, Aprovação e SLA de Requisição")
class RequisicaoTest {

    @Test
    @DisplayName("Deve calcular tempos de SLA corretos para requisições que não precisam de aprovação")
    void deveCalcularSlaSemAprovacao() {
        Requisicao reqAlta = new Requisicao(
                "Acesso emergencial VPN",
                "Liberação de VPN para plantão",
                "Mariana",
                Setor.TI,
                Categoria.ACESSO_E_PERMISSAO,
                Prioridade.ALTA,
                "Plantão do final de semana",
                false
        );
        assertEquals(TipoChamado.REQUISICAO, reqAlta.getTipo());
        assertEquals(24, reqAlta.getTempoMaximoResolucaoHoras());
        assertFalse(reqAlta.requerAtendimentoImediato());

        Requisicao reqMedia = new Requisicao(
                "Instalação do VS Code",
                "Instalar ambiente de desenvolvimento",
                "Paula",
                Setor.TI,
                Categoria.SOFTWARE,
                Prioridade.MEDIA,
                "Nova estagiária",
                false
        );
        assertEquals(48, reqMedia.getTempoMaximoResolucaoHoras());

        Requisicao reqBaixa = new Requisicao(
                "Segundo monitor",
                "Solicitação de periférico ergonômico",
                "Fernanda",
                Setor.RH,
                Categoria.HARDWARE,
                Prioridade.BAIXA,
                "Melhoria de posto de trabalho",
                false
        );
        assertEquals(72, reqBaixa.getTempoMaximoResolucaoHoras());
    }

    @Test
    @DisplayName("Deve adicionar 24 horas ao SLA quando a requisição necessita de aprovação prévia pendente")
    void deveAdicionarPrazoQuandoNecessitaAprovacao() {
        Requisicao reqPendente = new Requisicao(
                "Compra de Notebook Dell",
                "Upgrade de máquina para analista sênior",
                "Diretor Financeiro",
                Setor.FINANCEIRO,
                Categoria.HARDWARE,
                Prioridade.ALTA, // 24h base + 24h aprovação = 48h
                "Renovação de parque",
                true
        );

        assertEquals(48, reqPendente.getTempoMaximoResolucaoHoras());
        assertNull(reqPendente.getAprovado()); // Pendente

        // Quando o gestor aprova, o tempo base volta para 24h
        reqPendente.aprovar();
        assertEquals(Boolean.TRUE, reqPendente.getAprovado());
        assertEquals(24, reqPendente.getTempoMaximoResolucaoHoras());
    }

    @Test
    @DisplayName("Deve impedir atendimento de requisição pendente de aprovação")
    void deveImpedirAtendimentoSemAprovacao() {
        Requisicao req = new Requisicao(
                "Acesso a dados confidenciais do RH",
                "Acesso a folha de pagamento",
                "Estagiário",
                Setor.RH,
                Categoria.ACESSO_E_PERMISSAO,
                Prioridade.MEDIA,
                "Auditoria externa",
                true
        );

        // Tenta iniciar sem aprovação
        TransicaoStatusInvalidaException ex = assertThrows(TransicaoStatusInvalidaException.class, 
                req::iniciarAtendimento);
        assertTrue(ex.getMessage().contains("pendente de aprovação"));

        // Após aprovar, atendimento deve ser liberado
        req.aprovar();
        req.iniciarAtendimento();
        assertEquals(StatusChamado.EM_ANDAMENTO, req.getStatus());
    }

    @Test
    @DisplayName("Deve cancelar automaticamente a requisição quando ela for rejeitada")
    void deveCancelarRequisicaoQuandoRejeitada() {
        Requisicao req = new Requisicao(
                "Licença Adobe Photoshop",
                "Uso para edição pessoal de fotos",
                "João",
                Setor.OPERACIONAL,
                Categoria.SOFTWARE,
                Prioridade.BAIXA,
                "Uso pessoal",
                true
        );

        req.rejeitar("Software não condizente com as funções do cargo.");

        assertEquals(Boolean.FALSE, req.getAprovado());
        assertEquals(StatusChamado.CANCELADO, req.getStatus());
        assertTrue(req.getMotivoCancelamento().contains("Software não condizente"));
        assertNotNull(req.getDataFechamento());
    }
}
