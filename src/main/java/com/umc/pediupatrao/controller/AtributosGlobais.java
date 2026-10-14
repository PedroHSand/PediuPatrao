package com.umc.pediupatrao.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AtributosGlobais {

    @ModelAttribute("podeVerAuditoria")
    public boolean podeVerAuditoria(Authentication sessao) {
        if (sessao == null) {
            return false;
        }
        return sessao.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_GERENTE"));
    }
}
