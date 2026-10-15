package com.umc.pediupatrao.auditoria;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public List<ResumoAuditoria> consultar(@RequestParam(required = false) String usuario,
            @RequestParam(required = false) AcaoAuditada operacao,
            @RequestParam(required = false) String entidade) {
        return auditoriaService.consultar(usuario, operacao, entidade);
    }
}
