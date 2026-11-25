package com.suporti.suporti.controller;

import com.suporti.suporti.model.Chamado;
import com.suporti.suporti.model.enums.Categoria;
import com.suporti.suporti.model.enums.Setor;
import com.suporti.suporti.model.enums.TipoChamado;
import com.suporti.suporti.service.ChamadoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller do Modo Solicitante (Tela 2) do SuporTI.
 * Permite listar chamados com filtro por setor e cadastrar novos chamados (Incidentes ou Requisições).
 */
@Controller
@RequestMapping("/solicitante")
public class SolicitanteController {

    private final ChamadoService chamadoService;

    public SolicitanteController(ChamadoService chamadoService) {
        this.chamadoService = chamadoService;
    }

    /**
     * Exibe a lista de chamados para consulta do solicitante.
     * Opcionalmente filtra pelo setor escolhido.
     */
    @GetMapping
    public String listar(@RequestParam(value = "setor", required = false) Setor setor, Model model) {
        List<Chamado> chamados = chamadoService.listarPorSetor(setor);

        model.addAttribute("chamados", chamados);
        model.addAttribute("setorSelecionado", setor);
        model.addAttribute("setores", Setor.values());
        model.addAttribute("categorias", Categoria.values());
        model.addAttribute("tipos", TipoChamado.values());

        return "solicitante";
    }

    /**
     * Processa a abertura de um novo chamado pelo solicitante.
     * Aplica o padrão Post/Redirect/Get para evitar submissões duplicadas.
     */
    @PostMapping("/chamados")
    public String abrir(@RequestParam("tipo") TipoChamado tipo,
                        @RequestParam("titulo") String titulo,
                        @RequestParam("descricao") String descricao,
                        @RequestParam("solicitante") String solicitante,
                        @RequestParam("setor") Setor setor,
                        @RequestParam("categoria") Categoria categoria,
                        @RequestParam(value = "flagEspecifica", defaultValue = "false") boolean flagEspecifica,
                        @RequestParam(value = "detalheEspecifico", required = false) String detalheEspecifico,
                        RedirectAttributes redirectAttributes) {

        Chamado novo = chamadoService.abrir(tipo, titulo, descricao, solicitante,
                setor, categoria, flagEspecifica, detalheEspecifico);

        redirectAttributes.addFlashAttribute("sucesso", "Chamado nº " + novo.getId() + " aberto com sucesso!");
        return "redirect:/solicitante";
    }
}
