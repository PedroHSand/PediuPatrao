package com.umc.pediupatrao.auditoria;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.StatusPedido;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AuditoriaService {

    private static final List<String> CAMPOS_SIGILOSOS = List.of("password", "senha", "token");
    private static final List<String> CAMPOS_MASCARADOS = List.of("telefone", "cep", "numero", "complemento");

    private static final Map<String, String> ROTULOS = Map.ofEntries(
            Map.entry("status", "Status"),
            Map.entry("saidaPor", "Saída registrada por"),
            Map.entry("saidaEm", "Saída em"),
            Map.entry("percentualDesconto", "Percentual de desconto"),
            Map.entry("valorTotal", "Valor total"),
            Map.entry("tipoAtendimento", "Tipo de atendimento"),
            Map.entry("clienteId", "Cliente (id)"),
            Map.entry("itens", "Itens"),
            Map.entry("nome", "Nome"),
            Map.entry("telefone", "Telefone"),
            Map.entry("cep", "CEP"),
            Map.entry("logradouro", "Logradouro"),
            Map.entry("numero", "Número"),
            Map.entry("complemento", "Complemento"),
            Map.entry("bairro", "Bairro"),
            Map.entry("cidade", "Cidade"),
            Map.entry("estado", "Estado"),
            Map.entry("endereco", "Endereço (antigo)"));

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public void registrar(AcaoAuditada operacao, String entidade, String entidadeId,
            List<CampoModificado> alteracoes, String justificativa) {
        Authentication sessao = SecurityContextHolder.getContext().getAuthentication();

        String papel = sessao.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

        List<CampoModificado> diferencasLiberadas = alteracoes.stream()
                .filter(alteracao -> !CAMPOS_SIGILOSOS.contains(alteracao.getCampo().toLowerCase()))
                .toList();

        EventoAuditoria registro = new EventoAuditoria(sessao.getName(), Perfil.valueOf(papel),
                LocalDateTime.now(), operacao, entidade, entidadeId, diferencasLiberadas, justificativa);

        auditoriaRepository.save(registro);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public List<ResumoAuditoria> consultar(String usuario, AcaoAuditada operacao, String entidade) {
        List<ResumoAuditoria> linhas = new ArrayList<>();

        for (EventoAuditoria registro : auditoriaRepository.findAllByOrderByDataHoraDesc()) {
            if (usuario != null && !usuario.isBlank() && !registro.getUsuario().equalsIgnoreCase(usuario.trim())) {
                continue;
            }
            if (operacao != null && registro.getOperacao() != operacao) {
                continue;
            }
            if (entidade != null && !entidade.isBlank() && !registro.getEntidade().equalsIgnoreCase(entidade.trim())) {
                continue;
            }

            List<CampoModificado> alteracoesVisiveis = new ArrayList<>();
            for (CampoModificado alteracao : registro.getAlteracoes()) {
                alteracoesVisiveis.add(humanizar(ocultarSeSensivel(alteracao)));
            }

            linhas.add(new ResumoAuditoria(registro.getId(), registro.getUsuario(), registro.getPerfil(),
                    registro.getDataHora(), registro.getOperacao(), registro.getEntidade(),
                    registro.getEntidadeId(), alteracoesVisiveis, registro.getJustificativa()));
        }
        return linhas;
    }

    private CampoModificado ocultarSeSensivel(CampoModificado alteracao) {
        if (!CAMPOS_MASCARADOS.contains(alteracao.getCampo().toLowerCase())) {
            return alteracao;
        }
        return new CampoModificado(alteracao.getCampo(), esconder(alteracao.getValorAnterior()),
                esconder(alteracao.getValorNovo()));
    }

    private String esconder(String valor) {
        if (valor == null || valor.isEmpty()) {
            return valor;
        }
        int visiveis = valor.length() > 4 ? 4 : 0;
        return "*".repeat(valor.length() - visiveis) + valor.substring(valor.length() - visiveis);
    }

    private CampoModificado humanizar(CampoModificado alteracao) {
        String campo = alteracao.getCampo();
        return new CampoModificado(ROTULOS.getOrDefault(campo, campo),
                formatarValor(campo, alteracao.getValorAnterior()),
                formatarValor(campo, alteracao.getValorNovo()));
    }

    private String formatarValor(String campo, String valor) {
        if (valor == null || valor.isEmpty()) {
            return valor;
        }
        try {
            switch (campo) {
                case "saidaEm":
                    return LocalDateTime.parse(valor).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                case "valorTotal":
                    return String.format(Locale.of("pt", "BR"), "R$ %,.2f", new BigDecimal(valor));
                case "percentualDesconto":
                    return new BigDecimal(valor).stripTrailingZeros().toPlainString().replace('.', ',') + "%";
                case "status":
                    return StatusPedido.valueOf(valor).getDescricao();
                case "tipoAtendimento":
                    return valor.charAt(0) + valor.substring(1).toLowerCase();
                default:
                    return valor;
            }
        } catch (RuntimeException e) {
            return valor;
        }
    }
}
