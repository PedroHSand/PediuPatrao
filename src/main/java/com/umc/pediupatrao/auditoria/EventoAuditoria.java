package com.umc.pediupatrao.auditoria;

import com.umc.pediupatrao.entity.Perfil;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "auditoria")
public class EventoAuditoria {

    @Id
    private String id;
    private final String usuario;
    private final Perfil perfil;
    private final LocalDateTime dataHora;
    private final AcaoAuditada operacao;
    private final String entidade;
    private final String entidadeId;
    private final List<CampoModificado> alteracoes;
    private final String justificativa;

    public EventoAuditoria(String usuario, Perfil perfil, LocalDateTime dataHora, AcaoAuditada operacao,
            String entidade, String entidadeId, List<CampoModificado> alteracoes, String justificativa) {
        this.usuario = usuario;
        this.perfil = perfil;
        this.dataHora = dataHora;
        this.operacao = operacao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.alteracoes = alteracoes;
        this.justificativa = justificativa;
    }

    public String getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public AcaoAuditada getOperacao() {
        return operacao;
    }

    public String getEntidade() {
        return entidade;
    }

    public String getEntidadeId() {
        return entidadeId;
    }

    public List<CampoModificado> getAlteracoes() {
        return alteracoes;
    }

    public String getJustificativa() {
        return justificativa;
    }
}
