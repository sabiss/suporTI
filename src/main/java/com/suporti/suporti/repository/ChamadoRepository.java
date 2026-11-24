package com.suporti.suporti.repository;

import com.suporti.suporti.model.Chamado;
import com.suporti.suporti.model.Incidente;
import com.suporti.suporti.model.Requisicao;
import com.suporti.suporti.model.enums.Categoria;
import com.suporti.suporti.model.enums.Prioridade;
import com.suporti.suporti.model.enums.Setor;
import com.suporti.suporti.model.enums.StatusChamado;
import com.suporti.suporti.model.enums.TipoChamado;
import com.suporti.suporti.model.factory.ChamadoFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Camada de persistência dos chamados no SQLite, usando JdbcTemplate.
 * É a única classe do sistema que escreve SQL.
 *
 * Incidente e Requisicao ficam na mesma tabela ("tabela única por hierarquia"):
 * a coluna tipo diz qual subclasse recriar ao ler.
 */
@Repository
public class ChamadoRepository {

    private final JdbcTemplate jdbc;

    /**
     * O Spring entrega o JdbcTemplate já ligado ao banco (injeção de dependência).
     */
    public ChamadoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ==========================================
    // SALVAR (INSERT)
    // ==========================================

    private static final String SQL_INSERT = """
            INSERT INTO chamados (
                tipo, titulo, descricao, solicitante, setor, categoria,
                prioridade, status, data_abertura, prazo_limite, data_fechamento, motivo_cancelamento,
                sistema_indisponivel, ambiente_afetado,
                justificativa_negocio, precisa_aprovacao, aprovado
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    /**
     * Grava um chamado novo e devolve o id gerado pelo banco.
     * O id também é colocado no próprio objeto.
     */
    public Long salvar(Chamado chamado) {
        KeyHolder chaveGerada = new GeneratedKeyHolder();

        jdbc.update(conexao -> {
            PreparedStatement ps = conexao.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, chamado.getTipo().name());
            ps.setString(2, chamado.getTitulo());
            ps.setString(3, chamado.getDescricao());
            ps.setString(4, chamado.getSolicitante());
            ps.setString(5, chamado.getSetor().name());
            ps.setString(6, chamado.getCategoria().name());
            ps.setString(7, nome(chamado.getPrioridade()));
            ps.setString(8, chamado.getStatus().name());
            ps.setString(9, texto(chamado.getDataAbertura()));
            ps.setString(10, texto(chamado.getPrazoLimite()));
            ps.setString(11, texto(chamado.getDataFechamento()));
            ps.setString(12, chamado.getMotivoCancelamento());
            preencherCamposDoTipo(ps, 13, chamado);
            return ps;
        }, chaveGerada);

        Long id = chaveGerada.getKey().longValue();
        chamado.setId(id);
        return id;
    }

    // ==========================================
    // ATUALIZAR (UPDATE)
    // ==========================================

    private static final String SQL_UPDATE = """
            UPDATE chamados SET
                titulo = ?, descricao = ?, setor = ?, categoria = ?,
                prioridade = ?, status = ?, prazo_limite = ?, data_fechamento = ?, motivo_cancelamento = ?,
                sistema_indisponivel = ?, ambiente_afetado = ?,
                justificativa_negocio = ?, precisa_aprovacao = ?, aprovado = ?
            WHERE id = ?
            """;

    /**
     * Grava as mudanças de um chamado que já existe no banco
     * (triagem, mudança de status, cancelamento...).
     *
     * @return true se encontrou o chamado e atualizou; false se o id não existe
     */
    public boolean atualizar(Chamado chamado) {
        int linhasAlteradas = jdbc.update(conexao -> {
            PreparedStatement ps = conexao.prepareStatement(SQL_UPDATE);
            ps.setString(1, chamado.getTitulo());
            ps.setString(2, chamado.getDescricao());
            ps.setString(3, chamado.getSetor().name());
            ps.setString(4, chamado.getCategoria().name());
            ps.setString(5, nome(chamado.getPrioridade()));
            ps.setString(6, chamado.getStatus().name());
            ps.setString(7, texto(chamado.getPrazoLimite()));
            ps.setString(8, texto(chamado.getDataFechamento()));
            ps.setString(9, chamado.getMotivoCancelamento());
            preencherCamposDoTipo(ps, 10, chamado);
            ps.setLong(15, chamado.getId());
            return ps;
        });
        return linhasAlteradas > 0;
    }

    // ==========================================
    // BUSCAR (SELECT)
    // ==========================================

    /**
     * Busca um chamado pelo número. Devolve Optional vazio se não existir.
     */
    public Optional<Chamado> buscarPorId(Long id) {
        List<Chamado> encontrados = jdbc.query(
                "SELECT * FROM chamados WHERE id = ?", this::linhaParaChamado, id);
        return encontrados.stream().findFirst();
    }

    /**
     * Lista todos os chamados, do mais novo para o mais antigo.
     * Os filtros (FiltroChamado) recebem essa lista e escolhem o que cada tela mostra.
     */
    public List<Chamado> listarTodos() {
        return jdbc.query("SELECT * FROM chamados ORDER BY id DESC", this::linhaParaChamado);
    }

    // ==========================================
    // CONVERSÕES ENTRE BANCO E OBJETO
    // ==========================================

    /**
     * Transforma uma linha da tabela em um objeto Incidente ou Requisicao,
     * usando o método reconstruir da ChamadoFactory.
     */
    private Chamado linhaParaChamado(ResultSet rs, int numeroDaLinha) throws SQLException {
        TipoChamado tipo = TipoChamado.valueOf(rs.getString("tipo"));
        boolean ehIncidente = tipo == TipoChamado.INCIDENTE;

        return ChamadoFactory.reconstruir(
                rs.getLong("id"),
                tipo,
                rs.getString("titulo"),
                rs.getString("descricao"),
                rs.getString("solicitante"),
                Setor.valueOf(rs.getString("setor")),
                Categoria.valueOf(rs.getString("categoria")),
                rs.getString("prioridade") != null ? Prioridade.valueOf(rs.getString("prioridade")) : null,
                StatusChamado.valueOf(rs.getString("status")),
                data(rs.getString("data_abertura")),
                data(rs.getString("prazo_limite")),
                data(rs.getString("data_fechamento")),
                rs.getString("motivo_cancelamento"),
                ehIncidente ? rs.getInt("sistema_indisponivel") == 1 : rs.getInt("precisa_aprovacao") == 1,
                ehIncidente ? rs.getString("ambiente_afetado") : rs.getString("justificativa_negocio"),
                booleanoOuNulo(rs, "aprovado")
        );
    }

    /**
     * Preenche as 5 colunas específicas de cada tipo, a partir da posição indicada.
     * O tipo que não se aplica fica com as colunas vazias (NULL).
     */
    private void preencherCamposDoTipo(PreparedStatement ps, int posicao, Chamado chamado) throws SQLException {
        if (chamado instanceof Incidente incidente) {
            ps.setObject(posicao, incidente.isSistemaIndisponivel() ? 1 : 0);
            ps.setString(posicao + 1, incidente.getAmbienteAfetado());
            ps.setObject(posicao + 2, null);
            ps.setObject(posicao + 3, null);
            ps.setObject(posicao + 4, null);
        } else if (chamado instanceof Requisicao requisicao) {
            ps.setObject(posicao, null);
            ps.setObject(posicao + 1, null);
            ps.setString(posicao + 2, requisicao.getJustificativaNegocio());
            ps.setObject(posicao + 3, requisicao.isPrecisaAprovacao() ? 1 : 0);
            ps.setObject(posicao + 4, requisicao.getAprovado() == null ? null : (requisicao.getAprovado() ? 1 : 0));
        }
    }

    /** Enum → texto ("ALTA"), ou null se estiver vazio. */
    private static String nome(Enum<?> valor) {
        return valor != null ? valor.name() : null;
    }

    /** LocalDateTime → texto no formato 2026-11-23T19:49:00, ou null. */
    private static String texto(LocalDateTime data) {
        return data != null ? data.toString() : null;
    }

    /** Texto do banco → LocalDateTime, ou null. */
    private static LocalDateTime data(String texto) {
        return texto != null ? LocalDateTime.parse(texto) : null;
    }

    /** Coluna 0/1/NULL → false/true/null. */
    private static Boolean booleanoOuNulo(ResultSet rs, String coluna) throws SQLException {
        int valor = rs.getInt(coluna);
        return rs.wasNull() ? null : valor == 1;
    }
}