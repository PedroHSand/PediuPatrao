package com.umc.pediupatrao.auditoria;

import com.umc.pediupatrao.entity.Perfil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;

    private AuditoriaService auditoriaService;

    @BeforeEach
    void entrarComoGerencia() {
        auditoriaService = new AuditoriaService(auditoriaRepository);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "gerencia", "x", List.of(new SimpleGrantedAuthority("ROLE_GERENTE"))));
    }

    @AfterEach
    void sair() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registroGuardaUsuarioPerfilEHoraDoServidor() {
        auditoriaService.registrar(AcaoAuditada.CANCELAMENTO, "Pedido", "p1",
                List.of(new CampoModificado("status", "PRONTO", "CANCELADO")), "Cliente desistiu");

        ArgumentCaptor<EventoAuditoria> capturado = ArgumentCaptor.forClass(EventoAuditoria.class);
        verify(auditoriaRepository).save(capturado.capture());

        EventoAuditoria registro = capturado.getValue();
        assertEquals("gerencia", registro.getUsuario());
        assertEquals(Perfil.GERENTE, registro.getPerfil());
        assertEquals("Cliente desistiu", registro.getJustificativa());
        assertNotNull(registro.getDataHora());
    }

    @Test
    void camposDeSenhaENuncaSaoGravados() {
        auditoriaService.registrar(AcaoAuditada.ALTERACAO_CLIENTE, "Cliente", "c1",
                List.of(new CampoModificado("password", "antiga", "nova"),
                        new CampoModificado("token", "a", "b"),
                        new CampoModificado("nome", "Ana", "Ana Maria")),
                null);

        ArgumentCaptor<EventoAuditoria> capturado = ArgumentCaptor.forClass(EventoAuditoria.class);
        verify(auditoriaRepository).save(capturado.capture());

        List<CampoModificado> gravadas = capturado.getValue().getAlteracoes();
        assertEquals(1, gravadas.size());
        assertEquals("nome", gravadas.get(0).getCampo());
    }

    @Test
    void consultaMascaraDadosPessoaisETraduzOsCampos() {
        EventoAuditoria registro = new EventoAuditoria("balcao", Perfil.ATENDENTE, LocalDateTime.now(),
                AcaoAuditada.ALTERACAO_CLIENTE, "Cliente", "c1",
                List.of(new CampoModificado("telefone", "11911112222", "11987654321"),
                        new CampoModificado("logradouro", "Rua das Flores", "Av. Brasil")),
                null);
        when(auditoriaRepository.findAllByOrderByDataHoraDesc()).thenReturn(List.of(registro));

        List<ResumoAuditoria> linhas = auditoriaService.consultar(null, null, null);

        List<CampoModificado> alteracoes = linhas.get(0).getAlteracoes();
        assertEquals("Telefone", alteracoes.get(0).getCampo());
        assertEquals("*******4321", alteracoes.get(0).getValorNovo());
        assertEquals("Logradouro", alteracoes.get(1).getCampo());
        assertEquals("Av. Brasil", alteracoes.get(1).getValorNovo());
    }

    @Test
    void consultaTraduzValoresDePedido() {
        EventoAuditoria registro = new EventoAuditoria("gerencia", Perfil.GERENTE, LocalDateTime.now(),
                AcaoAuditada.DESCONTO, "Pedido", "p1",
                List.of(new CampoModificado("percentualDesconto", "0.00", "10.00"),
                        new CampoModificado("valorTotal", "50.00", "45.00")),
                null);
        when(auditoriaRepository.findAllByOrderByDataHoraDesc()).thenReturn(List.of(registro));

        List<CampoModificado> alteracoes = auditoriaService.consultar(null, null, null).get(0).getAlteracoes();

        assertEquals("Percentual de desconto", alteracoes.get(0).getCampo());
        assertEquals("10%", alteracoes.get(0).getValorNovo());
        assertEquals("Valor total", alteracoes.get(1).getCampo());
        assertEquals("R$ 45,00", alteracoes.get(1).getValorNovo());
    }

    @Test
    void consultaFiltraPorUsuarioEOperacao() {
        EventoAuditoria deBalcao = new EventoAuditoria("balcao", Perfil.ATENDENTE, LocalDateTime.now(),
                AcaoAuditada.CRIACAO_PEDIDO, "Pedido", "p1", List.of(), null);
        EventoAuditoria deGerencia = new EventoAuditoria("gerencia", Perfil.GERENTE, LocalDateTime.now(),
                AcaoAuditada.CANCELAMENTO, "Pedido", "p2", List.of(), "motivo");
        when(auditoriaRepository.findAllByOrderByDataHoraDesc()).thenReturn(List.of(deBalcao, deGerencia));

        assertEquals(1, auditoriaService.consultar("balcao", null, null).size());
        assertEquals(1, auditoriaService.consultar(null, AcaoAuditada.CANCELAMENTO, null).size());
        assertEquals(2, auditoriaService.consultar(null, null, "pedido").size());
    }
}
