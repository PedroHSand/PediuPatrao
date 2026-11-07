# API do Dashboard: endpoints

Base: `http://localhost:8080/api/dashboard`. Todos os endpoints são `GET` e devolvem JSON. Todos aceitam os filtros abaixo, exceto `/categorias`.
A documentação interativa (Swagger) fica em `http://localhost:8080/swagger-ui.html`.

## Autenticação e permissões

- A API usa a mesma sessão do sistema (login por formulário). Sem login, a resposta é um redirecionamento (302) para `/login`.
- Perfis permitidos: **ADMIN** e **GERENTE**. ATENDENTE recebe **403**.
- São endpoints só de leitura: outros métodos HTTP não são aceitos.

## Filtros (parâmetros de query, comuns a todos os endpoints)

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `periodo` | `HOJE`, `SEMANA` ou `MES` | não | Período fixo. Padrão: `HOJE`. Semana vai de segunda até hoje. Mês vai do dia 1 até hoje. |
| `inicio` | data `AAAA-MM-DD` | não | Início de um período personalizado. Exige `fim`. |
| `fim` | data `AAAA-MM-DD` | não | Fim de um período personalizado. Exige `inicio`. Se `inicio` e `fim` forem enviados, `periodo` é ignorado. |
| `tipoAtendimento` | `ENTREGA` ou `RETIRADA` | não | Filtra pelo tipo de atendimento. |
| `status` | `RECEBIDO`, `EM_PREPARACAO`, `PRONTO`, `SAIU_PARA_ENTREGA`, `RETIRADO`, `FINALIZADO` ou `CANCELADO` | não | Filtra pelo status atual do pedido. |
| `categoria` | texto | não | Tipo do produto, como os de `/categorias` (hoje Pizza, Esfiha e Bebida). Não diferencia maiúsculas. |

Regras: vendas são os pedidos não cancelados (só `pedidos/situacao` inclui os cancelados). O fuso é o de Brasília. O período máximo é de 366 dias. As regras completas estão em `docs/requisitos.md`.

## Erros

| Código | Quando | Corpo |
|---|---|---|
| 400 | Filtro inválido: data mal formatada, enum desconhecido, só uma das datas, início depois do fim, período maior que 366 dias ou limite fora de 1–20 | Texto com a mensagem |
| 302 | Sem login | Redireciona para `/login` |
| 403 | Perfil sem permissão | Vazio |

Exemplos de mensagens de erro (400). A data deve estar no formato `AAAA-MM-DD`, então `31/10/2026` é recusada:

| Requisição | Mensagem |
|---|---|
| `/resumo?inicio=2026-11-02&fim=2026-10-31` | `A data inicial não pode ser depois da data final.` |
| `/resumo?inicio=2026-10-31` | `Informe as duas datas, inicio e fim.` |
| `/resumo?inicio=31/10/2026&fim=02/11/2026` | `Valor inválido para o parâmetro 'inicio'.` |
| `/resumo?inicio=2026-10-31&fim=2026-11-02&status=XYZ` | `Valor inválido para o parâmetro 'status'.` |
| `/resumo?inicio=2025-10-31&fim=2026-11-02` | `O período máximo de consulta é de 366 dias.` |
| `/produtos/mais-vendidos?inicio=2026-10-31&fim=2026-11-02&limite=99` | `O limite deve ficar entre 1 e 20.` |

---

## GET /categorias

Tipos de produto cadastrados (RF05), em ordem alfabética e sem repetição. A tela usa essa lista para montar o filtro de categoria. Não recebe filtros.

**Requisição**
```
GET /api/dashboard/categorias
```

**Resposta 200**
```json
["Bebida", "Esfiha", "Pizza"]
```

---

## GET /resumo

Indicadores gerais (RF01). Os campos `hoje`, `semana` e `mes` são totais fixos e não mudam com os filtros.

**Requisição**
```
GET /api/dashboard/resumo?inicio=2026-10-31&fim=2026-11-02
```

**Resposta 200**
```json
{
  "inicio": "2026-10-31",
  "fim": "2026-11-02",
  "pedidos": 13,
  "faturamento": 1695.05,
  "ticketMedio": 130.39,
  "emPreparo": 3,
  "jaSairam": 8,
  "produtoMaisVendido": "Esfiha de Carne",
  "hoje":   { "pedidos": 13, "faturamento": 1695.05 },
  "semana": { "pedidos": 13, "faturamento": 1695.05 },
  "mes":    { "pedidos": 13, "faturamento": 1695.05 }
}
```

`produtoMaisVendido` é `null` quando não há vendas.

## GET /vendas/por-dia

Faturamento por dia (RF02, gráfico de linha). Devolve uma entrada para cada dia do período, inclusive dias sem venda.

**Requisição**
```
GET /api/dashboard/vendas/por-dia?inicio=2026-10-31&fim=2026-11-02
```

**Resposta 200**
```json
[
  { "rotulo": "2026-10-31", "pedidos": 0,  "faturamento": 0.00 },
  { "rotulo": "2026-11-01", "pedidos": 0,  "faturamento": 0.00 },
  { "rotulo": "2026-11-02", "pedidos": 13, "faturamento": 1695.05 }
]
```

## GET /vendas/por-hora

Pedidos por hora de entrada (RF02, gráfico de barras). A lista sempre tem 24 itens, um para cada hora do dia, de `00h` a `23h`, somando todos os dias do período. As horas sem pedido voltam com zero.

**Requisição**
```
GET /api/dashboard/vendas/por-hora?inicio=2026-10-31&fim=2026-11-02
```

**Resposta 200** (resumida: os itens com zero foram omitidos e marcados com `"..."`)
```json
[
  { "rotulo": "00h", "pedidos": 0,  "faturamento": 0.00 },
  "...",
  { "rotulo": "19h", "pedidos": 3,  "faturamento": 331.40 },
  { "rotulo": "20h", "pedidos": 10, "faturamento": 1363.65 },
  "...",
  { "rotulo": "23h", "pedidos": 0,  "faturamento": 0.00 }
]
```

## GET /vendas/por-categoria

Faturamento por categoria de produto (RF02, gráfico de rosca). Só aparecem categorias com vendas, da maior para a menor. O valor já considera o desconto do pedido. Um pedido com itens de mais de uma categoria é contado em cada uma delas.

**Requisição**
```
GET /api/dashboard/vendas/por-categoria?inicio=2026-10-31&fim=2026-11-02
```

**Resposta 200**
```json
[
  { "rotulo": "Pizza",  "pedidos": 12, "faturamento": 1383.12 },
  { "rotulo": "Esfiha", "pedidos": 5,  "faturamento": 170.05 },
  { "rotulo": "Bebida", "pedidos": 10, "faturamento": 141.88 }
]
```

## GET /vendas/por-atendimento

Entrega × retirada (RF02, gráfico de rosca). Sempre devolve as duas opções.

**Requisição**
```
GET /api/dashboard/vendas/por-atendimento?inicio=2026-10-31&fim=2026-11-02
```

**Resposta 200**
```json
[
  { "rotulo": "Entrega",  "pedidos": 9, "faturamento": 1230.15 },
  { "rotulo": "Retirada", "pedidos": 4, "faturamento": 464.90 }
]
```

## GET /pedidos/situacao

Quantidade de pedidos por status (RF03). Inclui os cancelados e sempre devolve os 7 status.

**Requisição**
```
GET /api/dashboard/pedidos/situacao?inicio=2026-10-31&fim=2026-11-02
```

**Resposta 200**
```json
[
  { "status": "RECEBIDO",          "descricao": "Recebido",          "quantidade": 1 },
  { "status": "EM_PREPARACAO",     "descricao": "Em preparação",     "quantidade": 3 },
  { "status": "PRONTO",            "descricao": "Pronto",            "quantidade": 1 },
  { "status": "SAIU_PARA_ENTREGA", "descricao": "Saiu para entrega", "quantidade": 0 },
  { "status": "RETIRADO",          "descricao": "Retirado",          "quantidade": 1 },
  { "status": "FINALIZADO",        "descricao": "Finalizado",        "quantidade": 7 },
  { "status": "CANCELADO",         "descricao": "Cancelado",         "quantidade": 2 }
]
```

## GET /produtos/mais-vendidos

Ranking de produtos por quantidade vendida (RF04). Em caso de empate na quantidade, vale o maior faturamento.

**Parâmetro extra**

| Parâmetro | Tipo | Padrão | Descrição |
|---|---|---|---|
| `limite` | inteiro de 1 a 20 | 5 | Quantidade de itens do ranking |

**Requisição**
```
GET /api/dashboard/produtos/mais-vendidos?inicio=2026-10-31&fim=2026-11-02&limite=3
```

**Resposta 200**
```json
[
  { "produto": "Esfiha de Carne",  "categoria": "Esfiha", "quantidade": 10, "faturamento": 59.15 },
  { "produto": "Coca-Cola 2L",     "categoria": "Bebida", "quantidade": 6,  "faturamento": 62.28 },
  { "produto": "Esfiha de Queijo", "categoria": "Esfiha", "quantidade": 6,  "faturamento": 33.15 }
]
```

## Combinando filtros

```
GET /api/dashboard/resumo?inicio=2026-10-31&fim=2026-11-02&tipoAtendimento=ENTREGA
```
```json
{
  "inicio": "2026-10-31",
  "fim": "2026-11-02",
  "pedidos": 9,
  "faturamento": 1230.15,
  "ticketMedio": 136.68,
  "emPreparo": 3,
  "jaSairam": 4,
  "produtoMaisVendido": "Esfiha de Carne",
  "hoje":   { "pedidos": 13, "faturamento": 1695.05 },
  "semana": { "pedidos": 13, "faturamento": 1695.05 },
  "mes":    { "pedidos": 13, "faturamento": 1695.05 }
}
```
