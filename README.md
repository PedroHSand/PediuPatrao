# PediuPatrão

Sistema web de pedidos para uma pizzaria, feito com **Java 21, Spring Boot 3.4, Spring Security, Thymeleaf e MongoDB Atlas**.

Esta versão evolui o projeto base com **controle de acesso por perfil (RBAC), gestão do fluxo de pedidos, cancelamento e desconto restritos ao gerente e auditoria persistente**.

## Como rodar

### Requisitos
- Java 21
- Uma conta no MongoDB Atlas, com o banco `pizzaria`
- O IP da sua máquina liberado em **Atlas → Security → Network Access**

### Passos
1. Copie `secrets.example.properties` para `secrets.properties` na raiz do projeto e preencha a string de conexão do seu cluster:
   ```properties
   MONGODB_URI=mongodb+srv://USUARIO:SENHA@SEU-CLUSTER.mongodb.net/pizzaria?retryWrites=true&w=majority
   ```
   O `secrets.properties` está no `.gitignore` e não é versionado.
2. Rode o projeto:
   ```powershell
   .\mvnw spring-boot:run
   ```
   Ou execute `PizzariaPedidosApplication` pela IDE.
3. Acesse <http://localhost:8080>.

### Usuários de teste
Na primeira execução, se não existirem, estes usuários são criados automaticamente. A senha de todos é `teste123`.

| Usuário | Perfil |
|---|---|
| `admin` | ADMIN |
| `gerencia` | GERENTE |
| `balcao` | ATENDENTE |

### Testes automatizados
```powershell
.\mvnw test
```
Não precisam do Atlas: usam mocks e `@WebMvcTest`.

## Perfis e permissões

| Funcionalidade | ADMIN | GERENTE | ATENDENTE |
|---|:-:|:-:|:-:|
| Usuários e perfis | ✅ | ❌ | ❌ |
| Consultar clientes | ❌ | ✅ | ✅ |
| Cadastrar, alterar e excluir clientes | ❌ | ❌ | ✅ |
| Consultar pedidos | ✅ | ✅ | ✅ |
| Registrar pedido e avançar status | ❌ | ✅ | ✅ |
| Aplicar desconto e cancelar pedido | ❌ | ✅ | ❌ |
| Consultar auditoria | ✅ | ✅ | ❌ |
| Produtos | ver | gerenciar | ver |

A autorização é aplicada **no backend**, em duas camadas: regras de URL no `SecurityConfig` e `@PreAuthorize` nos services. Ocultar botões na tela é só conveniência.

## Requisitos implementados

### RF01 — Usuários e permissões
- Enum `Perfil` (`ADMIN`, `GERENTE`, `ATENDENTE`) gravado em cada usuário, e o perfil do banco é o que vale no login.
- Somente o ADMIN cria, edita, exclui usuários e atribui perfis, nas telas e na API.
- Proteção contra autoelevação: ninguém altera o próprio perfil nem exclui o próprio usuário, e não é possível salvar usuário sem perfil.
- Desconto e cancelamento são do GERENTE, e de mais ninguém, nem do ADMIN.
- A senha é "só de escrita": nunca volta em resposta de API.
- CSRF ligado, com formulários usando `th:action` e os `fetch` enviando o token.

### RF02 — Pedido
- O pedido registra itens com produto, nome, quantidade e preço unitário, tipo de atendimento entrega ou retirada, percentual de desconto, valor total, responsável pelo registro e data/hora de entrada.
- O preço do item é **copiado do produto no momento do pedido**.
- O total é **calculado no servidor**. A requisição usa um DTO sem campos de valor, e por isso qualquer `valorTotal` ou `registradoPor` enviado é ignorado.
- Validações: cliente existente, pelo menos um item, quantidade ≥ 1, produto existente e ativo.
- Valores monetários em `BigDecimal`.

### RF03 — Entrada, saída e transições
- Fluxo: `RECEBIDO → EM_PREPARACAO → PRONTO →` `SAIU_PARA_ENTREGA` para entrega ou `RETIRADO` para retirada `→ FINALIZADO`.
- Transições inválidas, como pular etapa, voltar ou finalizar pedido em preparação, são recusadas.
- Pedido `CANCELADO` ou `FINALIZADO` não muda mais, e `CANCELADO` só é alcançado pela operação de cancelamento.
- A entrada registra usuário e data/hora, enquanto a **saída**, em `SAIU_PARA_ENTREGA` ou `RETIRADO`, registra obrigatoriamente data/hora e responsável.

### RF04 — Cancelamento e desconto exclusivos do GERENTE
- **Cancelamento:** somente antes da saída e com justificativa obrigatória, preservando o pedido e seus dados.
- **Desconto:** percentual de 0 a 20%, somente antes da saída e nunca em pedido cancelado. Um desconto novo substitui o anterior, e o total é recalculado a partir dos itens.
- As duas operações são auditadas com valores anteriores e novos.

### RF05 — Auditoria
- Persistida na coleção `auditoria` do MongoDB.
- Auditados: criação, alteração e exclusão de clientes, campo a campo e só o que mudou, além da criação de pedido, da mudança de status, do desconto e do cancelamento.
- Cada registro guarda usuário, perfil, data/hora, operação, entidade e id, campos com valor anterior e novo, e justificativa quando há.
- O repositório da auditoria só tem inserção e busca, e por isso não existe rota para alterar ou excluir registros.
- Consulta somente para ADMIN e GERENTE: tela `/auditoria`, com filtros, e `GET /api/auditoria`.
- Senha, token e campos semelhantes nunca são gravados. Na consulta, telefone, CEP, número e complemento aparecem mascarados.

### Qualidade
- Regras de negócio nos services, com mensagens de erro claras nas telas e respostas `409` para regra de negócio, `403` para falta de permissão e `400` para parâmetro inválido na API.
- Tentativas de acesso negado são registradas em log com usuário, método e caminho, sem dados sensíveis.
- 49 testes automatizados: transições de status, cálculo de total e desconto, cancelamento, auditoria e permissões das APIs por perfil.

## Principais alterações em relação ao projeto base
- **Segurança:** `Perfil`, `UsuarioDetailsService` com o perfil do banco, `SecurityConfig` com regras por perfil e `@EnableMethodSecurity`, `/api/usuarios` deixou de ser público, CSRF religado.
- **Pedidos:** `ItemDoPedido`, `ModoAtendimento`, `StatusPedido`, `NovoPedidoForm`, novos campos em `Pedido`, tela de novo pedido e lista reformulada com itens, total, responsável, saída e ações por perfil.
- **Auditoria:** `EventoAuditoria`, `CampoModificado`, `AcaoAuditada`, `AuditoriaRepository`, `AuditoriaService` e a tela e API de consulta.
- **Clientes:** a lista deixou de montar HTML com `innerHTML`, o que abria risco de XSS, e a exclusão envia o token CSRF.
- **Testes e configuração:** testes automatizados, `secrets.example.properties` e dependências de teste no `pom.xml`, sem alterar versões.

## Decisões de projeto
- **ADMIN e clientes:** o ADMIN administra usuários e acessos e não consulta clientes, seguindo a lista de responsabilidades do enunciado e reduzindo a exposição de dados pessoais.
- **Exclusão de cliente:** não está no enunciado, então foi atribuída ao ATENDENTE, junto com cadastro e alteração.
- **Produtos:** todos veem, mas só o GERENTE cria, edita e exclui.
- **Entrega × retirada:** o enunciado cita "saiu para entrega / retirado" sem ligar ao tipo, então aqui pedido de entrega vai para `SAIU_PARA_ENTREGA` e pedido de retirada vai para `RETIRADO`.
- **Campos antigos do pedido:** `tipoPizza`, `sabor` e `quantidade` foram mantidos por compatibilidade com pedidos já gravados, enquanto os pedidos novos são descritos pelos itens.
- **Mascaramento:** o dado completo fica gravado na auditoria, e só a exibição, na tela e na API, o oculta.

## Pontos herdados do projeto base, não alterados
- Links de **Funcionários, Entregadores e Horário de Funcionamento** na sidebar apontam para rotas que não existem.
- A rota `/teste` não tem template, e `/admin` é a página de demonstração do tema AdminLTE.

## Estrutura
```
src/main/java/com/umc/pediupatrao
├── auditoria/    EventoAuditoria, CampoModificado, AcaoAuditada, ResumoAuditoria,
│                 AuditoriaRepository, AuditoriaService, AuditoriaController
├── config/       SecurityConfig, DataInitializer, MongoInitConfig
├── controller/   HomeController das telas, controllers REST de /api, ErrosDaApi, AtributosGlobais
├── dto/          NovoPedidoForm, NovoItemForm
├── entity/       Usuario, Perfil, Cliente, Produto, Pedido, ItemDoPedido, StatusPedido, ModoAtendimento
├── exception/    NegocioException
├── repository/   repositórios do MongoDB
└── service/      regras de negócio de clientes, produtos, pedidos e usuários
src/test/java     testes automatizados
```
