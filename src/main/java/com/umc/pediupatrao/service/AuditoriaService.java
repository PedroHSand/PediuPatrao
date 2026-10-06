package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditoriaService {

    private static final List<String> CAMPOS_PROIBIDOS = List.of("password", "senha", "token");

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public void registrar(TipoOperacao operacao, String entidade, String entidadeId,
            List<AlteracaoCampo> alteracoes, String justificativa) {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();

        // a authority vem como ROLE_GERENTE, então tiro o prefixo para virar o enum
        String nomePerfil = autenticacao.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

        List<AlteracaoCampo> alteracoesSeguras = alteracoes.stream()
                .filter(alteracao -> !CAMPOS_PROIBIDOS.contains(alteracao.getCampo().toLowerCase()))
                .toList();

        RegistroAuditoria registro = new RegistroAuditoria(autenticacao.getName(), Perfil.valueOf(nomePerfil),
                LocalDateTime.now(), operacao, entidade, entidadeId, alteracoesSeguras, justificativa);

        auditoriaRepository.save(registro);
    }
}
