package com.suporti.suporti.config;

import com.suporti.suporti.model.Chamado;
import com.suporti.suporti.model.enums.Categoria;
import com.suporti.suporti.model.enums.Prioridade;
import com.suporti.suporti.model.enums.Setor;
import com.suporti.suporti.model.enums.StatusChamado;
import com.suporti.suporti.model.enums.TipoChamado;
import com.suporti.suporti.model.factory.ChamadoFactory;
import com.suporti.suporti.repository.ChamadoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Seeder de dados iniciais para o SQLite.
 *
 * Popula o banco com chamados de exemplo inspirados no documento "Regras e Telas v2"
 * caso a tabela esteja vazia na inicialização.
 *
 * Inclui:
 * - Chamados aguardando triagem (Incidente e Requisição);
 * - Chamados na fila de trabalho: Atrasado (vermelho), Vence hoje (amarelo) e No prazo (verde);
 * - Chamado concluído para histórico e filtro por setor.
 */
@Component
public class DadosIniciaisSeeder implements CommandLineRunner {

    private final ChamadoRepository repository;

    public DadosIniciaisSeeder(ChamadoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (!repository.listarTodos().isEmpty()) {
            return; // Já existem dados no banco, não sobrescreve
        }

        LocalDateTime agora = LocalDateTime.now();

        // 1. Aguardando Triagem — Incidente Crítico
        Chamado c1 = ChamadoFactory.reconstruir(
                null,
                TipoChamado.INCIDENTE,
                "Sem internet no setor",
                "Desde cedo nenhum computador do Marketing acessa a internet.",
                "Roberto Silva",
                Setor.MARKETING,
                Categoria.REDE,
                null,
                StatusChamado.ABERTO,
                agora.minusHours(3),
                null,
                null,
                null,
                true,
                "Marketing 2º Andar",
                null
        );
        repository.salvar(c1);

        // 2. Aguardando Triagem — Requisição de Equipamento
        Chamado c2 = ChamadoFactory.reconstruir(
                null,
                TipoChamado.REQUISICAO,
                "Mouse novo para estagiário",
                "Solicito um mouse USB novo para o novo estagiário da equipe.",
                "Mariana Costa",
                Setor.FINANCEIRO,
                Categoria.HARDWARE,
                null,
                StatusChamado.ABERTO,
                agora.minusHours(1),
                null,
                null,
                null,
                false,
                "Equipamento com defeito",
                true
        );
        repository.salvar(c2);

        // 3. Fila de Trabalho — Incidente Atrasado (Prazo no passado)
        Chamado c3 = ChamadoFactory.reconstruir(
                null,
                TipoChamado.INCIDENTE,
                "Wi-fi caindo no setor",
                "Ponto de acesso na sala de reuniões desconectando a cada 10 minutos.",
                "Lucas Pereira",
                Setor.OPERACIONAL,
                Categoria.REDE,
                Prioridade.ALTA,
                StatusChamado.EM_ANDAMENTO,
                agora.minusDays(2),
                agora.minusHours(5), // Atrasado!
                null,
                null,
                true,
                "Sala de Reuniões B",
                null
        );
        repository.salvar(c3);

        // 4. Fila de Trabalho — Incidente Vence Hoje (Alta prioridade)
        Chamado c4 = ChamadoFactory.reconstruir(
                null,
                TipoChamado.INCIDENTE,
                "Impressora não imprime",
                "A impressora do 2º andar não imprime desde ontem. Já reiniciei o computador e a impressora.",
                "Juliana Santos",
                Setor.FINANCEIRO,
                Categoria.HARDWARE,
                Prioridade.ALTA,
                StatusChamado.EM_ANDAMENTO,
                agora.minusHours(4),
                agora.plusHours(2), // Vence hoje!
                null,
                null,
                false,
                "Financeiro 2º Andar",
                null
        );
        repository.salvar(c4);

        // 5. Fila de Trabalho — Incidente Vence Hoje (Status Aberto)
        Chamado c5 = ChamadoFactory.reconstruir(
                null,
                TipoChamado.INCIDENTE,
                "Sistema de vendas lento",
                "Transações demorando mais de 30 segundos para processar no caixa.",
                "Paulo Andrade",
                Setor.VENDAS,
                Categoria.SOFTWARE,
                Prioridade.MEDIA,
                StatusChamado.ABERTO,
                agora.minusHours(6),
                agora.plusHours(4), // Vence hoje!
                null,
                null,
                false,
                "Caixas 1 a 4",
                null
        );
        repository.salvar(c5);

        // 6. Fila de Trabalho — Requisição No Prazo
        Chamado c6 = ChamadoFactory.reconstruir(
                null,
                TipoChamado.REQUISICAO,
                "Instalar o Excel",
                "Instalação do pacote Office / Excel na máquina recém formatada.",
                "Beatriz Lima",
                Setor.RH,
                Categoria.SOFTWARE,
                Prioridade.BAIXA,
                StatusChamado.EM_ANDAMENTO,
                agora.minusHours(12),
                agora.plusDays(3), // No prazo
                null,
                null,
                false,
                "Treinamento de novos funcionários",
                true
        );
        repository.salvar(c6);

        // 7. Histórico — Requisição Concluída
        Chamado c7 = ChamadoFactory.reconstruir(
                null,
                TipoChamado.REQUISICAO,
                "Criar acesso para estagiário",
                "Criação de usuário e e-mail institucional para novo estagiário.",
                "Fernanda Rocha",
                Setor.TI,
                Categoria.ACESSO_E_PERMISSAO,
                Prioridade.MEDIA,
                StatusChamado.CONCLUIDO,
                agora.minusDays(3),
                agora.minusDays(1),
                agora.minusHours(18),
                null,
                false,
                "Onboarding",
                true
        );
        repository.salvar(c7);
    }
}
