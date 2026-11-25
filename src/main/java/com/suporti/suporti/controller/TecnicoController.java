package com.suporti.suporti.controller;

import com.suporti.suporti.model.Chamado;
import com.suporti.suporti.model.enums.Prioridade;
import com.suporti.suporti.model.exception.ChamadoNaoEncontradoException;
import com.suporti.suporti.model.exception.DadosChamadoInvalidosException;
import com.suporti.suporti.model.exception.TransicaoStatusInvalidaException;
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
     * Prepara e exibe o painel do técnico com as listas de triagem e de trabalho.
     *
     * @param aba aba que deve aparecer inicialmente; por padrão, {@code triagem}
     * @param model modelo usado para fornecer listas, contagens e prioridades à view
     * @return nome da view do painel do técnico
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
     * Define a prioridade do chamado durante a triagem e redireciona para a fila de trabalho.
     * Essa ação calcula o SLA, mas não inicia o atendimento.
     *
     * @param id identificador do chamado
     * @param prioridade prioridade selecionada pelo técnico
     * @param redirectAttributes atributos temporários para exibir uma mensagem de sucesso
     * @return redirecionamento para a aba da fila de trabalho
     * @throws ChamadoNaoEncontradoException se o chamado não existir
     * @throws DadosChamadoInvalidosException se a prioridade for nula
     * @throws TransicaoStatusInvalidaException se o chamado já estiver finalizado
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
     * Solicita ao service o início do atendimento e redireciona para a fila de trabalho.
     *
     * @param id identificador do chamado
     * @param redirectAttributes atributos temporários para exibir uma mensagem de sucesso
     * @return redirecionamento para a aba da fila de trabalho
     * @throws ChamadoNaoEncontradoException se o chamado não existir
     * @throws TransicaoStatusInvalidaException se a transição não for válida, incluindo aprovação
     *         pendente em uma requisição que a exige
     */
    @PostMapping("/chamados/{id}/iniciar")
    public String iniciar(@PathVariable("id") Long id,
                          RedirectAttributes redirectAttributes) {
        chamadoService.iniciarAtendimento(id);
        redirectAttributes.addFlashAttribute("sucesso", "Atendimento do chamado nº " + id + " iniciado!");
        return "redirect:/tecnico?aba=fila";
    }

    /**
     * Solicita a conclusão do chamado e redireciona para a fila de trabalho.
     *
     * @param id identificador do chamado
     * @param redirectAttributes atributos temporários para exibir uma mensagem de sucesso
     * @return redirecionamento para a aba da fila de trabalho
     * @throws ChamadoNaoEncontradoException se o chamado não existir
     * @throws TransicaoStatusInvalidaException se o chamado já estiver concluído ou cancelado
     */
    @PostMapping("/chamados/{id}/concluir")
    public String concluir(@PathVariable("id") Long id,
                           RedirectAttributes redirectAttributes) {
        chamadoService.concluir(id);
        redirectAttributes.addFlashAttribute("sucesso", "Chamado nº " + id + " concluído com sucesso!");
        return "redirect:/tecnico?aba=fila";
    }

    /**
     * Cancela o chamado com a justificativa informada e retorna à aba de origem.
     *
     * @param id identificador do chamado
     * @param motivo justificativa do cancelamento; não pode ser nula nem vazia
     * @param abaOrigem aba para a qual redirecionar após o cancelamento; por padrão, {@code fila}
     * @param redirectAttributes atributos temporários para exibir uma mensagem de sucesso
     * @return redirecionamento à aba indicada em {@code abaOrigem}
     * @throws ChamadoNaoEncontradoException se o chamado não existir
     * @throws DadosChamadoInvalidosException se o motivo for nulo ou vazio
     * @throws TransicaoStatusInvalidaException se o chamado já estiver finalizado
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
     * Aprova a requisição identificada e redireciona para a fila de trabalho.
     * A aprovação não inicia o atendimento.
     *
     * @param id identificador da requisição
     * @param redirectAttributes atributos temporários para exibir uma mensagem de sucesso
     * @return redirecionamento para a aba da fila de trabalho
     * @throws ChamadoNaoEncontradoException se o chamado não existir
     * @throws DadosChamadoInvalidosException se o chamado não for uma requisição
     * @throws TransicaoStatusInvalidaException se a requisição que exige aprovação já estiver finalizada
     */
    @PostMapping("/chamados/{id}/aprovar")
    public String aprovar(@PathVariable("id") Long id,
                          RedirectAttributes redirectAttributes) {
        chamadoService.aprovarRequisicao(id);
        redirectAttributes.addFlashAttribute("sucesso", "Requisição nº " + id + " aprovada com sucesso!");
        return "redirect:/tecnico?aba=fila";
    }
}
