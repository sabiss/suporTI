package com.suporti.suporti.model;

import com.suporti.suporti.model.enums.Categoria;
import com.suporti.suporti.model.enums.Prioridade;
import com.suporti.suporti.model.enums.Setor;
import com.suporti.suporti.model.enums.StatusChamado;
import com.suporti.suporti.model.enums.TipoChamado;
import com.suporti.suporti.model.exception.DadosChamadoInvalidosException;
import com.suporti.suporti.model.factory.ChamadoFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do Padrão Factory (ChamadoFactory)")
class ChamadoFactoryTest {

    @Test
    @DisplayName("Factory deve criar Incidente corretamente com atributos específicos e SLA inicial")
    void deveCriarIncidenteViaFactory() {
        Incidente inc = ChamadoFactory.criarIncidente(
                "Sem acesso à internet", "Cabo rompido no corredor", "Amanda",
                Setor.MARKETING, Categoria.REDE, false, "Bloco B");
        inc.classificarPrioridade(Prioridade.MEDIA);

        assertNotNull(inc);
        assertEquals(TipoChamado.INCIDENTE, inc.getTipo());
        assertEquals("Sem acesso à internet", inc.getTitulo());
        assertEquals(StatusChamado.ABERTO, inc.getStatus());
        assertEquals("Bloco B", inc.getAmbienteAfetado());
        assertFalse(inc.isSistemaIndisponivel());
        assertNotNull(inc.getDataAbertura());
        assertNotNull(inc.getPrazoLimite());
        assertEquals(8, inc.getTempoMaximoResolucaoHoras());
    }

    @Test
    @DisplayName("Factory deve criar Requisição corretamente com atributos específicos")
    void deveCriarRequisicaoViaFactory() {
        Requisicao req = ChamadoFactory.criarRequisicao(
                "Novo usuário no sistema ERP", "Cadastro de novo analista contábil", "Roberto",
                Setor.FINANCEIRO, Categoria.ACESSO_E_PERMISSAO,
                "Contratação CLT", true);
        req.classificarPrioridade(Prioridade.ALTA);

        assertNotNull(req);
        assertEquals(TipoChamado.REQUISICAO, req.getTipo());
        assertEquals("Novo usuário no sistema ERP", req.getTitulo());
        assertEquals(StatusChamado.ABERTO, req.getStatus());
        assertTrue(req.isPrecisaAprovacao());
        assertEquals("Contratação CLT", req.getJustificativaNegocio());
        assertNotNull(req.getPrazoLimite());
    }

    @Test
    @DisplayName("Factory deve instanciar polimorficamente via método genérico criarChamado")
    void deveCriarChamadoGenericoPolimorfico() {
        Chamado c1 = ChamadoFactory.criarChamado(
                TipoChamado.INCIDENTE,
                "Servidor MySQL parou", "Crash no processo mysqld", "SysAdmin",
                Setor.TI, Categoria.SOFTWARE, true, "Cluster Produção");
        c1.classificarPrioridade(Prioridade.ALTA);

        Chamado c2 = ChamadoFactory.criarChamado(
                TipoChamado.REQUISICAO,
                "Mouse sem fio", "Substituição de equipamento com defeito", "Mariana",
                Setor.RH, Categoria.HARDWARE, false, "Substituição simples");
        c2.classificarPrioridade(Prioridade.BAIXA);

        assertTrue(c1 instanceof Incidente);
        assertEquals(TipoChamado.INCIDENTE, c1.getTipo());

        assertTrue(c2 instanceof Requisicao);
        assertEquals(TipoChamado.REQUISICAO, c2.getTipo());
    }

    @Test
    @DisplayName("Factory deve lançar exceção ao tentar criar chamado com tipo nulo")
    void deveLancarExcecaoQuandoTipoNulo() {
        assertThrows(DadosChamadoInvalidosException.class, () ->
                ChamadoFactory.criarChamado(null, "Titulo", "Desc", "Sol",
                        Setor.TI, Categoria.HARDWARE, false, "Geral"));
    }
}
