package com.umc.pediupatrao.auditoria;

import org.springframework.data.repository.Repository;

import java.util.List;

public interface AuditoriaRepository extends Repository<EventoAuditoria, String> {

    EventoAuditoria save(EventoAuditoria registro);

    List<EventoAuditoria> findAllByOrderByDataHoraDesc();
}
