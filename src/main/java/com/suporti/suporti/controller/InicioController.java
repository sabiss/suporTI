package com.suporti.suporti.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller da Tela Inicial (Tela 1) do SuporTI.
 * Permite a escolha direta entre os modos Solicitante e Técnico de TI.
 */
@Controller
public class InicioController {

    @GetMapping("/")
    public String inicio() {
        return "index";
    }
}
