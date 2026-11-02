package com.umc.pediupatrao.dashboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Indicadores e gráficos de vendas. Acesso: ADMIN e GERENTE.")
@ApiResponse(responseCode = "400", description = "Filtro inválido (datas, enum ou limite)")
@ApiResponse(responseCode = "403", description = "Perfil sem permissão")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/categorias")
    @Operation(summary = "Categorias disponíveis", description = "Tipos de produto cadastrados, usados no filtro de categoria.")
    public List<String> categorias() {
        return dashboardService.categorias();
    }

    @GetMapping("/resumo")
    @Operation(summary = "Indicadores gerais",
            description = "Pedidos, faturamento, ticket médio, pedidos em preparo, pedidos que já saíram, "
                    + "produto mais vendido e os totais fixos de hoje, semana e mês.")
    public ResumoDashboard resumo(@ParameterObject ParametrosFiltro filtro) {
        return dashboardService.resumo(filtro);
    }

    @GetMapping("/vendas/por-dia")
    @Operation(summary = "Faturamento por dia", description = "Uma entrada por dia do período, inclusive dias sem venda.")
    public List<PontoSerie> vendasPorDia(@ParameterObject ParametrosFiltro filtro) {
        return dashboardService.vendasPorDia(filtro);
    }

    @GetMapping("/vendas/por-hora")
    @Operation(summary = "Pedidos por hora do dia", description = "24 entradas, de 00h a 23h, somando o período filtrado.")
    public List<PontoSerie> vendasPorHora(@ParameterObject ParametrosFiltro filtro) {
        return dashboardService.vendasPorHora(filtro);
    }

    @GetMapping("/vendas/por-categoria")
    @Operation(summary = "Vendas por categoria de produto")
    public List<PontoSerie> vendasPorCategoria(@ParameterObject ParametrosFiltro filtro) {
        return dashboardService.vendasPorCategoria(filtro);
    }

    @GetMapping("/vendas/por-atendimento")
    @Operation(summary = "Entrega x retirada")
    public List<PontoSerie> vendasPorAtendimento(@ParameterObject ParametrosFiltro filtro) {
        return dashboardService.vendasPorAtendimento(filtro);
    }

    @GetMapping("/pedidos/situacao")
    @Operation(summary = "Quantidade de pedidos por status", description = "Inclui os cancelados.")
    public List<SituacaoStatus> situacao(@ParameterObject ParametrosFiltro filtro) {
        return dashboardService.situacaoDosPedidos(filtro);
    }

    @GetMapping("/produtos/mais-vendidos")
    @Operation(summary = "Ranking de produtos mais vendidos")
    public List<ItemRanking> maisVendidos(@ParameterObject ParametrosFiltro filtro,
            @Parameter(description = "Quantidade de itens do ranking, de 1 a 20")
            @RequestParam(defaultValue = "5") int limite) {
        return dashboardService.maisVendidos(filtro, limite);
    }
}
