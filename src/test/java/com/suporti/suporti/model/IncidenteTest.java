package com.suporti.suporti.model;

import com.suporti.suporti.model.enums.Categoria;
import com.suporti.suporti.model.enums.Prioridade;
import com.suporti.suporti.model.enums.Setor;
import com.suporti.suporti.model.enums.TipoChamado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Regras de Negócio e SLA de Incidente")
class IncidenteTest {

    @Test
    @DisplayName("Deve calcular 2 horas de SLA para incidente de prioridade ALTA com sistema indisponível")
    void deveCalcularSlaAltaComIndisponibilidade() {
        Incidente inc = new Incidente(
                "Servidor de BD fora do ar",
                "Conexão com banco recusada",
                "Carlos",
                Setor.TI,
                Categoria.SOFTWARE,
                true,
                "Produção"
        );
        inc.classificarPrioridade(Prioridade.ALTA);

        assertEquals(TipoChamado.INCIDENTE, inc.getTipo());
        assertEquals(2, inc.getTempoMaximoResolucaoHoras());
        assertTrue(inc.requerAtendimentoImediato());
        assertTrue(inc.obterDescricaoSla().contains("2 horas"));
        assertTrue(inc.obterDescricaoSla().contains("Indisponibilidade Crítica: Sim"));
    }

    @Test
    @DisplayName("Deve calcular 4 horas de SLA para incidente de prioridade ALTA sem indisponibilidade de sistema")
    void deveCalcularSlaAltaSemIndisponibilidade() {
        Incidente inc = new Incidente(
                "Lentidão extrema no faturamento",
                "Notas fiscais demoram mais de 10 min",
                "Beatriz",
                Setor.FINANCEIRO,
                Categoria.SOFTWARE,
                false,
                "Produção"
        );
        inc.classificarPrioridade(Prioridade.ALTA);

        assertEquals(4, inc.getTempoMaximoResolucaoHoras());
        assertTrue(inc.requerAtendimentoImediato()); // ALTA sempre requer atendimento imediato
    }

    @Test
    @DisplayName("Deve calcular 8 horas de SLA para incidente de prioridade MEDIA")
    void deveCalcularSlaMedia() {
        Incidente inc = new Incidente(
                "Impressora travada",
                "Papel atolado no setor",
                "Renata",
                Setor.RH,
                Categoria.HARDWARE,
                false,
                "Escritório"
        );
        inc.classificarPrioridade(Prioridade.MEDIA);

        assertEquals(8, inc.getTempoMaximoResolucaoHoras());
        assertFalse(inc.requerAtendimentoImediato());
    }

    @Test
    @DisplayName("Deve calcular 24 horas de SLA para incidente de prioridade BAIXA")
    void deveCalcularSlaBaixa() {
        Incidente inc = new Incidente(
                "Teclado com tecla solta",
                "Tecla espaço solta no teclado USB",
                "Marcos",
                Setor.VENDAS,
                Categoria.HARDWARE,
                false,
                "Escritório"
        );
        inc.classificarPrioridade(Prioridade.BAIXA);

        assertEquals(24, inc.getTempoMaximoResolucaoHoras());
        assertFalse(inc.requerAtendimentoImediato());
    }

    @Test
    @DisplayName("Deve recalcular prazo limite quando alterada a flag de indisponibilidade")
    void deveRecalcularPrazoAoAlterarIndisponibilidade() {
        Incidente inc = new Incidente(
                "Instabilidade no portal",
                "Portal apresentando erros ocasionais",
                "Admin",
                Setor.TI,
                Categoria.SOFTWARE,
                false,
                "Produção"
        );
        inc.classificarPrioridade(Prioridade.ALTA);

        LocalDateTime prazoSemQueda = inc.getPrazoLimite();

        // O portal cai completamente, tornando o incidente crítico
        inc.setSistemaIndisponivel(true); // Reduz para 2 horas

        assertTrue(inc.getPrazoLimite().isBefore(prazoSemQueda),
                "O prazo limite deve ser antecipado após o sistema se tornar indisponível");
        assertEquals(2, inc.getTempoMaximoResolucaoHoras());
    }
}
