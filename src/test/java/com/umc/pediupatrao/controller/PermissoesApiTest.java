package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.auditoria.AuditoriaController;
import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.auditoria.AuditoriaService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import com.umc.pediupatrao.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({PedidoController.class, AuditoriaController.class, UsuarioController.class})
@Import(SecurityConfig.class)
class PermissoesApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PedidoService pedidoService;

    @MockitoBean
    private AuditoriaService auditoriaService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    // ---------- cancelamento e desconto: só GERENTE ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminNaoCancelaPedido() throws Exception {
        mockMvc.perform(put("/api/pedidos/p1/cancelar").param("justificativa", "x").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoCancelaPedido() throws Exception {
        mockMvc.perform(put("/api/pedidos/p1/cancelar").param("justificativa", "x").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteCancelaPedido() throws Exception {
        mockMvc.perform(put("/api/pedidos/p1/cancelar").param("justificativa", "x").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminNaoAplicaDesconto() throws Exception {
        mockMvc.perform(put("/api/pedidos/p1/desconto").param("percentual", "10").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoAplicaDesconto() throws Exception {
        mockMvc.perform(put("/api/pedidos/p1/desconto").param("percentual", "10").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteAplicaDesconto() throws Exception {
        mockMvc.perform(put("/api/pedidos/p1/desconto").param("percentual", "10").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void percentualQueNaoEhNumeroDevolveErroDeRequisicao() throws Exception {
        mockMvc.perform(put("/api/pedidos/p1/desconto").param("percentual", "abc").with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ---------- registro de pedidos: GERENTE e ATENDENTE, nunca ADMIN ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminNaoRegistraPedido() throws Exception {
        mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content("{}").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteRegistraPedido() throws Exception {
        mockMvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content("{}").with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminConsultaPedidos() throws Exception {
        mockMvc.perform(get("/api/pedidos")).andExpect(status().isOk());
    }

    // ---------- auditoria: só ADMIN e GERENTE, e só leitura ----------

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoConsultaAuditoria() throws Exception {
        mockMvc.perform(get("/api/auditoria")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteConsultaAuditoria() throws Exception {
        mockMvc.perform(get("/api/auditoria")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminConsultaAuditoria() throws Exception {
        mockMvc.perform(get("/api/auditoria")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void naoExisteRotaParaAlterarOuExcluirAuditoria() throws Exception {
        mockMvc.perform(delete("/api/auditoria/x").with(csrf())).andExpect(status().is4xxClientError());
        mockMvc.perform(put("/api/auditoria/x").with(csrf())).andExpect(status().is4xxClientError());
        mockMvc.perform(post("/api/auditoria").with(csrf())).andExpect(status().is4xxClientError());
    }

    // ---------- usuários: só ADMIN ----------

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteNaoListaUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoCriaUsuario() throws Exception {
        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("{}").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminListaUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios")).andExpect(status().isOk());
    }

    // ---------- sem login e sem CSRF ----------

    @Test
    void semLoginRedirecionaParaOLogin() throws Exception {
        mockMvc.perform(get("/api/auditoria")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/api/usuarios")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void operacaoSemTokenCsrfEhRecusada() throws Exception {
        mockMvc.perform(put("/api/pedidos/p1/cancelar").param("justificativa", "x"))
                .andExpect(status().isForbidden());
    }
}
