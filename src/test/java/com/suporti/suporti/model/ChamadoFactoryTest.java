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
                "Sem acesso à internet",
                "Cabo rompido no corredor",
                "Amanda",
                Setor.MARKETING,
                Categoria.REDE,
                Prioridade.MEDIA,
                false,
                "Bloco B"
        );

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
                "Novo usuário no sistema ERP",
                "Cadastro de novo analista contábil",
                "Roberto",
                Setor.FINANCEIRO,
                Categoria.ACESSO_E_PERMISSAO,
                Prioridade.ALTA,
                "Contratação CLT",
                true
        );

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
                "Servidor MySQL parou",
                "Crash no processo mysqld",
                "SysAdmin",
                Setor.TI,
                Categoria.SOFTWARE,
                Prioridade.ALTA,
                true,
                "Cluster Produção"
        );
        assertTrue(c1 instanceof Incidente);
        assertEquals(TipoChamado.INCIDENTE, c1.getTipo());

        Chamado c2 = ChamadoFactory.criarChamado(
                TipoChamado.REQUISICAO,
                "Mouse sem fio",
                "Substituição de equipamento com defeito",
                "Mariana",
                Setor.RH,
                Categoria.HARDWARE,
                Prioridade.BAIXA,
                false,
                "Substituição simples"
        );
        assertTrue(c2 instanceof Requisicao);
        assertEquals(TipoChamado.REQUISICAO, c2.getTipo());
    }

    @Test
    @DisplayName("Factory deve lançar exceção ao tentar criar chamado com tipo nulo")
    void deveLancarExcecaoQuandoTipoNulo() {
        assertThrows(DadosChamadoInvalidosException.class, () ->
                ChamadoFactory.criarChamado(null, "Titulo", "Desc", "Sol",
                        Setor.TI, Categoria.HARDWARE, Prioridade.BAIXA, false, "Geral"));
    }

    @Test
    @DisplayName("Factory deve reconstruir chamado a partir de dados de banco de dados")
    void deveReconstruirChamadoComDadosPersistidos() {
        LocalDateTime abertura = LocalDateTime.now().minusDays(1);
        LocalDateTime prazo = abertura.plusHours(8);
        LocalDateTime fechamento = LocalDateTime.now().minusHours(2);

        Chamado reconstruido = ChamadoFactory.reconstruir(
                100L,
                TipoChamado.INCIDENTE,
                "Incidente Histórico",
                "Descrição salva no banco",
                "UsuarioAntigo",
                Setor.OPERACIONAL,
                Categoria.REDE,
                Prioridade.MEDIA,
                StatusChamado.CONCLUIDO,
                abertura,
                prazo,
                fechamento,
                null,
                false,
                "Produção",
                null
        );

        assertEquals(100L, reconstruido.getId());
        assertEquals(StatusChamado.CONCLUIDO, reconstruido.getStatus());
        assertEquals(abertura, reconstruido.getDataAbertura());
        assertEquals(prazo, reconstruido.getPrazoLimite());
        assertEquals(fechamento, reconstruido.getDataFechamento());
        assertTrue(reconstruido instanceof Incidente);
    }
}
