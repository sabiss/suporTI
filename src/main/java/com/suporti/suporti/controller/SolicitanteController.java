package com.suporti.suporti.controller;

import com.suporti.suporti.model.Chamado;
import com.suporti.suporti.model.enums.Categoria;
import com.suporti.suporti.model.enums.Setor;
import com.suporti.suporti.model.enums.TipoChamado;
import com.suporti.suporti.model.exception.DadosChamadoInvalidosException;
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
     * Carrega os chamados para consulta e prepara os dados usados pelos filtros e pelo formulário.
     *
     * @param setor setor pelo qual filtrar; se ausente, lista chamados de todos os setores
     * @param model modelo usado para fornecer chamados, filtros e opções do formulário à view
     * @return nome da view da página do solicitante
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
     * Cria um chamado com os dados do formulário e redireciona à lista do solicitante
     * (padrão Post/Redirect/Get).
     *
     * @param tipo tipo do chamado: incidente ou requisição
     * @param titulo título resumido do chamado
     * @param descricao descrição detalhada do problema ou solicitação
     * @param solicitante nome de quem está abrindo o chamado
     * @param setor setor de origem do chamado
     * @param categoria categoria técnica do chamado
     * @param flagEspecifica indica indisponibilidade para incidente ou necessidade de aprovação para requisição
     * @param detalheEspecifico ambiente afetado do incidente ou justificativa da requisição; pode ser nulo
     * @param redirectAttributes atributos temporários usados para exibir a confirmação após o redirecionamento
     * @return redirecionamento para a lista de chamados do solicitante
     * @throws DadosChamadoInvalidosException se os dados obrigatórios forem inválidos
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
