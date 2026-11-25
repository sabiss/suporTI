package com.suporti.suporti.controller;

import com.suporti.suporti.model.exception.ChamadoException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Tratador global de exceções para a camada Web (@ControllerAdvice).
 * Intercepta exceções de domínio do SuporTI e redireciona para a tela de origem
 * com mensagem amigável via RedirectAttributes (flash attribute).
 */
@ControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(ChamadoException.class)
    public String tratarErroDominio(ChamadoException ex,
                                    HttpServletRequest request,
                                    RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        return redirecionarOrigem(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String tratarArgumentoInvalido(IllegalArgumentException ex,
                                          HttpServletRequest request,
                                          RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        return redirecionarOrigem(request);
    }

    @ExceptionHandler(Exception.class)
    public String tratarErroGenerico(Exception ex,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("erro", "Ocorreu um erro inesperado: " + ex.getMessage());
        return redirecionarOrigem(request);
    }

    private String redirecionarOrigem(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;
        }
        return "redirect:/";
    }
}
