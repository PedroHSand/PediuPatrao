package com.umc.pediupatrao.dashboard;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.controller.ErrosDaApi;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, ErrosDaApi.class})
class DashboardApiTest {

    private static final String[] ROTAS = {
        "/api/dashboard/categorias",
        "/api/dashboard/resumo",
        "/api/dashboard/vendas/por-dia",
        "/api/dashboard/vendas/por-hora",
        "/api/dashboard/vendas/por-categoria",
        "/api/dashboard/vendas/por-atendimento",
        "/api/dashboard/pedidos/situacao",
        "/api/dashboard/produtos/mais-vendidos"
    };

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    // ---------- permissões ----------

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteAcessaTodosOsEndpoints() throws Exception {
        for (String rota : ROTAS) {
            mockMvc.perform(get(rota)).andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminAcessaTodosOsEndpoints() throws Exception {
        for (String rota : ROTAS) {
            mockMvc.perform(get(rota)).andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoAcessaNenhumEndpoint() throws Exception {
        for (String rota : ROTAS) {
            mockMvc.perform(get(rota)).andExpect(status().isForbidden());
        }
    }

    @Test
    void semLoginRedirecionaParaOLogin() throws Exception {
        for (String rota : ROTAS) {
            mockMvc.perform(get(rota)).andExpect(status().is3xxRedirection());
        }
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoAcessaADocumentacaoDaApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isForbidden());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void dashboardSoAceitaLeitura() throws Exception {
        mockMvc.perform(post("/api/dashboard/resumo")).andExpect(status().is4xxClientError());
    }

    // ---------- erros de filtro ----------

    @Test
    @WithMockUser(roles = "GERENTE")
    void dataEmFormatoInvalidoDevolve400() throws Exception {
        mockMvc.perform(get("/api/dashboard/resumo").param("inicio", "ontem").param("fim", "hoje"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Valor inválido para o parâmetro 'inicio'."));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void statusDesconhecidoDevolve400() throws Exception {
        mockMvc.perform(get("/api/dashboard/resumo").param("status", "NAO_EXISTE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void tipoDeAtendimentoDesconhecidoDevolve400() throws Exception {
        mockMvc.perform(get("/api/dashboard/vendas/por-dia").param("tipoAtendimento", "VOANDO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void limiteQueNaoEhNumeroDevolve400() throws Exception {
        mockMvc.perform(get("/api/dashboard/produtos/mais-vendidos").param("limite", "muitos"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void regraDeFiltroDoServicoViraErro400ComMensagem() throws Exception {
        when(dashboardService.resumo(any())).thenThrow(new FiltroInvalidoException("Informe as duas datas, inicio e fim."));

        mockMvc.perform(get("/api/dashboard/resumo").param("inicio", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Informe as duas datas, inicio e fim."));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void limiteInvalidoDoRankingViraErro400() throws Exception {
        when(dashboardService.maisVendidos(any(), anyInt()))
                .thenThrow(new FiltroInvalidoException("O limite deve ficar entre 1 e 20."));

        mockMvc.perform(get("/api/dashboard/produtos/mais-vendidos").param("limite", "99"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void listasVaziasSaoDevolvidasComoArrayVazio() throws Exception {
        when(dashboardService.vendasPorDia(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/vendas/por-dia"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
