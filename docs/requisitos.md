# Dashboard Administrativo: requisitos

Painel para o responsável pela pizzaria acompanhar vendas, pedidos e tendências, alimentado por uma API própria em `/api/dashboard`.

## Requisitos funcionais

| ID | Requisito | Critério de aceite |
|---|---|---|
| RF01 | Indicadores gerais de vendas | Mostrar pedidos, faturamento, ticket médio, pedidos em preparo, pedidos que já saíram e produto mais vendido. Mostrar também os totais fixos de hoje, da semana e do mês. |
| RF02 | Análise de vendas (gráficos) | Faturamento por dia (linha), pedidos por hora do dia (barras), vendas por categoria (rosca) e entrega × retirada (rosca). |
| RF03 | Situação dos pedidos | Quantidade de pedidos por status, incluindo cancelados. |
| RF04 | Produtos mais vendidos | Ranking por quantidade, com faturamento de cada produto e limite configurável (1 a 20). |
| RF05 | Filtros de consulta | Período (hoje, semana, mês ou datas personalizadas), tipo de atendimento, status e categoria. Todos os indicadores, gráficos e o ranking respeitam os filtros. |
| RF06 | Integração com a API | O dashboard obtém todos os dados pelos endpoints, sem acessar o banco diretamente. |
| RF07 | Atualização e tratamento de dados | Atualização dos dados sem recarregar a página, com indicação de carregamento, de ausência de dados e de erro. Filtros inválidos devolvem mensagem clara. |

## Requisitos não funcionais

| ID | Requisito |
|---|---|
| RNF01 | **Segurança:** somente ADMIN e GERENTE acessam o dashboard, a API de métricas e a documentação da API. A regra é aplicada no backend (Spring Security) e as respostas não trazem dados pessoais de clientes. |
| RNF02 | **Desempenho:** a consulta ao MongoDB busca apenas o intervalo de datas pedido, e o período máximo de consulta é de 366 dias. |
| RNF03 | **Confiabilidade:** pedidos antigos sem itens, sem total ou com status desconhecido não quebram os cálculos. |
| RNF04 | **Usabilidade:** layout responsivo, no mesmo padrão visual do restante do sistema (Bootstrap 5). |
| RNF05 | **Manutenibilidade:** código em camadas (controller, service, repositório) e em pacote próprio, `dashboard`, como já é feito na auditoria. |
| RNF06 | **Testabilidade:** regras de cálculo e permissões cobertas por testes automatizados que não dependem do banco. |
| RNF07 | **Documentação:** endpoints documentados no Swagger (`/swagger-ui.html`) e neste diretório. |

## Regras de negócio das métricas

1. **Venda** é todo pedido com status diferente de `CANCELADO`. Só vendas entram em faturamento, ticket médio, gráficos e ranking.
2. **Faturamento** é a soma do `valorTotal` dos pedidos, que já está com o desconto aplicado.
3. **Ticket médio** é o faturamento dividido pelo número de vendas. Sem vendas, é zero.
4. **Em preparo** são os pedidos em `EM_PREPARACAO`.
5. **Já saíram** são os pedidos em `SAIU_PARA_ENTREGA`, `RETIRADO` ou `FINALIZADO`.
6. **Situação dos pedidos** é a única consulta que inclui os cancelados.
7. **Data do pedido** é a data de entrada (`entradaEm`). Pedidos sem essa data ficam fora das consultas.
8. **Fuso horário:** Brasília (`America/Sao_Paulo`). "Hoje" é o dia atual nesse fuso.
9. **Semana** vai da segunda-feira até hoje. **Mês** vai do dia 1 até hoje.
10. **Período personalizado** exige as duas datas, com início menor ou igual ao fim. Se for informado, vale mais que o período fixo.
11. **Categoria** é o tipo do produto cadastrado (hoje: Pizza, Esfiha e Bebida; o filtro da tela lista os tipos que existem no cadastro). Item de produto que não existe mais fica em "Outros".
12. **Valor por categoria e por produto** usa o subtotal do item com o desconto do pedido, para que a soma bata com o faturamento.
13. **Filtro de categoria** mantém apenas os pedidos que têm itens daquela categoria. Os gráficos de categoria e o ranking mostram só esses itens.
14. **Totais fixos** de hoje, semana e mês (no resumo) não são afetados pelos filtros.

## Endpoints previstos

Todos usam `GET`, aceitam os filtros `periodo`, `inicio`, `fim`, `tipoAtendimento`, `status` e `categoria`, e exigem perfil ADMIN ou GERENTE.

| Endpoint | Requisito |
|---|---|
| `/api/dashboard/categorias` | RF05 (opções do filtro de categoria) |
| `/api/dashboard/resumo` | RF01 |
| `/api/dashboard/vendas/por-dia` | RF02 |
| `/api/dashboard/vendas/por-hora` | RF02 |
| `/api/dashboard/vendas/por-categoria` | RF02 |
| `/api/dashboard/vendas/por-atendimento` | RF02 |
| `/api/dashboard/pedidos/situacao` | RF03 |
| `/api/dashboard/produtos/mais-vendidos` (parâmetro extra `limite`) | RF04 |

A documentação detalhada, com exemplos de requisição e resposta, fica em `docs/api-endpoints.md`.
