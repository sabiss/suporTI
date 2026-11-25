package com.suporti.suporti.controller;

import com.suporti.suporti.model.Chamado;
import com.suporti.suporti.model.enums.Categoria;
import com.suporti.suporti.model.enums.Prioridade;
import com.suporti.suporti.model.enums.Setor;
import com.suporti.suporti.model.enums.TipoChamado;
import com.suporti.suporti.service.ChamadoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/teste-web-suporti.db")
class WebControllersTest {

    private MockMvc mockMvc;

    @Autowired
    private org.springframework.web.context.WebApplicationContext webApplicationContext;

    @Autowired
    private ChamadoService chamadoService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setup() {
        jdbc.update("DELETE FROM chamados");
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("GET / deve retornar 200 e view index")
    void deveCarregarTelaInicial() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    @DisplayName("GET /solicitante deve carregar atributos do model e view solicitante")
    void deveCarregarTelaSolicitante() throws Exception {
        mockMvc.perform(get("/solicitante"))
                .andExpect(status().isOk())
                .andExpect(view().name("solicitante"))
                .andExpect(model().attributeExists("chamados", "setores", "categorias", "tipos"));
    }

    @Test
    @DisplayName("POST /solicitante/chamados deve abrir novo chamado e redirecionar com flash de sucesso")
    void deveAbrirChamadoViaPost() throws Exception {
        mockMvc.perform(post("/solicitante/chamados")
                        .param("tipo", "INCIDENTE")
                        .param("titulo", "Problema no Monitor")
                        .param("descricao", "Monitor piscando constantemente")
                        .param("solicitante", "Ana")
                        .param("setor", "FINANCEIRO")
                        .param("categoria", "HARDWARE")
                        .param("flagEspecifica", "false")
                        .param("detalheEspecifico", "Sala 101"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/solicitante"))
                .andExpect(flash().attributeExists("sucesso"));
    }

    @Test
    @DisplayName("GET /tecnico deve carregar o painel com as abas de triagem e fila")
    void deveCarregarPainelTecnico() throws Exception {
        mockMvc.perform(get("/tecnico").param("aba", "triagem"))
                .andExpect(status().isOk())
                .andExpect(view().name("tecnico"))
                .andExpect(model().attributeExists("chamadosAguardando", "chamadosFila", "qtdAguardando", "qtdFila", "prioridades"));
    }

    @Test
    @DisplayName("POST /tecnico/chamados/{id}/prioridade deve triar o chamado e redirecionar para a fila")
    void deveTriarChamado() throws Exception {
        Chamado chamado = chamadoService.abrir(TipoChamado.INCIDENTE, "Queda de link",
                "Sem conexao", "Carlos", Setor.TI, Categoria.REDE, true, "Servidor");

        mockMvc.perform(post("/tecnico/chamados/{id}/prioridade", chamado.getId())
                        .param("prioridade", "ALTA"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tecnico?aba=fila"))
                .andExpect(flash().attributeExists("sucesso"));
    }

    @Test
    @DisplayName("POST /tecnico/chamados/{id}/iniciar e concluir deve realizar o fluxo de atendimento")
    void deveIniciarEConcluirAtendimento() throws Exception {
        Chamado chamado = chamadoService.abrir(TipoChamado.INCIDENTE, "Teclado quebrado",
                "Teclas falhando", "Marcos", Setor.RH, Categoria.HARDWARE, false, "Geral");
        chamadoService.classificarPrioridade(chamado.getId(), Prioridade.MEDIA);

        mockMvc.perform(post("/tecnico/chamados/{id}/iniciar", chamado.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tecnico?aba=fila"))
                .andExpect(flash().attributeExists("sucesso"));

        mockMvc.perform(post("/tecnico/chamados/{id}/concluir", chamado.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tecnico?aba=fila"))
                .andExpect(flash().attributeExists("sucesso"));
    }
}
