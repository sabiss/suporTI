package com.suporti.suporti.controller;

import com.suporti.suporti.model.Chamado;
import com.suporti.suporti.model.enums.Prioridade;
import com.suporti.suporti.service.ChamadoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller do Painel do Técnico de TI (Tela 3) do SuporTI.
 * Gerencia as abas "Aguardando triagem" e "Fila de trabalho", além das transições de status.
 */
@Controller
@RequestMapping("/tecnico")
public class TecnicoController {

    private final ChamadoService chamadoService;

    public TecnicoController(ChamadoService chamadoService) {
        this.chamadoService = chamadoService;
    }

    /**
     * Exibe o painel do técnico na aba selecionada ('triagem' ou 'fila').
     */
    @GetMapping
    public String painel(@RequestParam(value = "aba", defaultValue = "triagem") String aba, Model model) {
        List<Chamado> aguardando = chamadoService.listarAguardandoTriagem();
        List<Chamado> fila = chamadoService.listarFilaDeTrabalho();

        model.addAttribute("abaAtiva", aba);
        model.addAttribute("chamadosAguardando", aguardando);
        model.addAttribute("chamadosFila", fila);
        model.addAttribute("qtdAguardando", aguardando.size());
        model.addAttribute("qtdFila", fila.size());
        model.addAttribute("prioridades", Prioridade.values());

        return "tecnico";
    }

    /**
     * Ação de triagem: define a prioridade do chamado (e calcula o prazo de SLA).
     */
    @PostMapping("/chamados/{id}/prioridade")
    public String definirPrioridade(@PathVariable("id") Long id,
                                    @RequestParam("prioridade") Prioridade prioridade,
                                    RedirectAttributes redirectAttributes) {
        chamadoService.classificarPrioridade(id, prioridade);
        redirectAttributes.addFlashAttribute("sucesso", "Prioridade do chamado nº " + id + " definida como " + prioridade.getDescricao() + "!");
        return "redirect:/tecnico?aba=fila";
    }

    /**
     * Inicia o atendimento do chamado, alterando status para EM_ANDAMENTO.
     */
    @PostMapping("/chamados/{id}/iniciar")
    public String iniciar(@PathVariable("id") Long id,
                          RedirectAttributes redirectAttributes) {
        chamadoService.iniciarAtendimento(id);
        redirectAttributes.addFlashAttribute("sucesso", "Atendimento do chamado nº " + id + " iniciado!");
        return "redirect:/tecnico?aba=fila";
    }

    /**
     * Conclui o atendimento do chamado, alterando status para CONCLUIDO.
     */
    @PostMapping("/chamados/{id}/concluir")
    public String concluir(@PathVariable("id") Long id,
                           RedirectAttributes redirectAttributes) {
        chamadoService.concluir(id);
        redirectAttributes.addFlashAttribute("sucesso", "Chamado nº " + id + " concluído com sucesso!");
        return "redirect:/tecnico?aba=fila";
    }

    /**
     * Cancela o chamado mediante justificativa obrigatória.
     */
    @PostMapping("/chamados/{id}/cancelar")
    public String cancelar(@PathVariable("id") Long id,
                           @RequestParam("motivo") String motivo,
                           @RequestParam(value = "abaOrigem", defaultValue = "fila") String abaOrigem,
                           RedirectAttributes redirectAttributes) {
        chamadoService.cancelar(id, motivo);
        redirectAttributes.addFlashAttribute("sucesso", "Chamado nº " + id + " cancelado.");
        return "redirect:/tecnico?aba=" + abaOrigem;
    }

    /**
     * Aprova uma requisição que exige aprovação prévia.
     */
    @PostMapping("/chamados/{id}/aprovar")
    public String aprovar(@PathVariable("id") Long id,
                          RedirectAttributes redirectAttributes) {
        chamadoService.aprovarRequisicao(id);
        redirectAttributes.addFlashAttribute("sucesso", "Requisição nº " + id + " aprovada com sucesso!");
        return "redirect:/tecnico?aba=fila";
    }
}
