# Solicitation API

API REST para um sistema de **Solicitações de Atendimento**, desenvolvida como desafio técnico para a SEA Tecnologia.

O cliente cadastra uma solicitação em 3 etapas (dados básicos, endereço com consulta de CEP no ViaCEP e confirmação), pode salvar o rascunho e continuar depois, e depois envia para análise. O analista avalia apenas as solicitações das UFs que cobre. As solicitações são indexadas no Elasticsearch para busca com filtros e paginação, e as ações críticas passam por auditoria via AOP.

## Stack

- Java 21 e Spring Boot 4
- PostgreSQL 15 (persistência transacional) com migrations Flyway
- Elasticsearch 9 (busca com filtros, ordenação e paginação)
- Spring Security com JWT (controle por role, dono e UF)
- Bean Validation e erros JSON padronizados
- AOP com `@Audit` e tabela de auditoria (`tb_audit_logs`)
- Micrometer + Prometheus (métricas) e OpenAPI/Swagger (documentação)
- Docker e Docker Compose
- JUnit 5, Mockito e Testcontainers

## Como rodar

Pré-requisito: Docker com Docker Compose.

1. Crie o arquivo `.env` a partir do modelo versionado (o `.env` real não é versionado) e troque a senha e o segredo:

   ```bash
   cp .env.example .env
   ```

   ```env
   SPRING_DATASOURCE_DB=solicitation_api
   SPRING_DATASOURCE_USERNAME=postgres
   SPRING_DATASOURCE_PASSWORD=troque-esta-senha
   JWT_SECRET=troque-por-um-segredo-longo-e-aleatorio
   ```

   Sem o `.env`, o Compose se recusa a subir e informa qual variável falta.

2. Suba tudo:

   ```bash
   docker compose up --build
   ```

   O Elasticsearch leva em torno de 1 a 2 minutos para ficar saudável. A aplicação só inicia depois que o Postgres e o Elasticsearch estiverem `healthy`.

3. Acesse:

   | Serviço | URL |
   |---|---|
   | API | http://localhost:8080 |
   | Swagger UI | http://localhost:8080/swagger-ui.html |
   | Actuator (health e métricas) | http://localhost:9001/actuator/health e `/actuator/prometheus` |
   | Prometheus | http://localhost:9090 (alvo em Status > Targets) |

Para parar: `docker compose down` (use `-v` para apagar também os volumes de dados).

**Portas publicadas no host:** a API em `8080` (todas as interfaces) e, somente em `127.0.0.1`, o Postgres em **`5433`**, o Elasticsearch em `9200`, o actuator em `9001` e o Prometheus em `9090`. O Postgres usa `5433` no host para não conflitar com um PostgreSQL local na `5432`; dentro do Docker a aplicação continua usando `db-postgres:5432`.

Há também uma coleção do Postman para testes manuais: `Solicitation-API.postman_collection.json`.

### Usuário ADMIN inicial

Na primeira inicialização, se não existir nenhum ADMIN, é criado um:

| E-mail | Senha |
|---|---|
| `admin@solicitation.com` | `admin123456` |

## Perfis de acesso

| Perfil | O que pode fazer |
|---|---|
| `CLIENT` | Se cadastra (`/auth/register`) e só acessa as próprias solicitações. É o **único** perfil que usa as rotas `/solicitations/**` (ANALYST e ADMIN recebem 403) |
| `ANALYST` | Avalia solicitações **somente das UFs do seu coverage**; não enxerga rascunhos |
| `ADMIN` | Cria usuários internos (ANALYST/ADMIN), define as UFs de cada analista e acessa as rotas de analista sem restrição de UF |

## Endpoints

Todas as rotas, exceto `/auth/**`, exigem o header `Authorization: Bearer <token>`.

### Autenticação

| Método | Rota | Descrição                                            |
|---|---|------------------------------------------------------|
| POST | `/auth/register` | Cria um usuário CLIENT e devolve o token             |
| POST | `/auth/login` | Autentica e devolve o token (50 minutos de validade) |

### Admin

| Método | Rota | Descrição |
|---|---|---|
| POST | `/admin/users` | Cria ANALYST ou ADMIN. O ANALYST precisa de ao menos uma UF em `states` |
| GET | `/admin/users/{id}` | Consulta um usuário (inclui `states` quando é ANALYST) |

### Cliente

| Método | Rota | Descrição |
|---|---|---|
| POST | `/solicitations` | Cria um rascunho (DRAFT, step 1) |
| GET | `/solicitations` | Lista as solicitações do cliente (paginado: `page`, `size`, `sort`) |
| GET | `/solicitations/{id}` | Consulta uma solicitação própria |
| PUT | `/solicitations/{id}/step1` | Dados básicos |
| PUT | `/solicitations/{id}/step2` | Endereço (CEP consultado no ViaCEP) |
| PUT | `/solicitations/{id}/step3` | Confirmação |
| POST | `/solicitations/{id}/submit` | Envia para análise (valida tudo; só o dono, só em DRAFT) |

### Analista

| Método | Rota | Descrição |
|---|---|---|
| GET | `/analyst/solicitations/{id}` | Consulta, se a UF estiver no coverage do analista |
| POST | `/analyst/solicitations/{id}/start` | `SUBMITTED` para `IN_REVIEW` |
| POST | `/analyst/solicitations/{id}/decide` | `APPROVE` ou `REJECT`, com comentário de 10 a 1000 caracteres |
| GET | `/analyst/solicitations/search` | Busca no Elasticsearch |

#### Busca

Parâmetros: `q` (título e descrição), `status`, `serviceType`, `priority`, `state`, `dateFrom`, `dateTo` (formato `yyyy-MM-dd`, inclusivo), `page` (padrão 0), `size` (padrão 20, máximo 100) e `sort` no formato `campo,ASC|DESC` (padrão `createdAt,DESC`).

`sort` só aceita os campos `createdAt`, `status`, `priority`, `serviceType` e `state`. Outro campo ou formato inválido devolve 400.

Rascunhos (`DRAFT`) **nunca** aparecem na busca de um ANALYST, mesmo que ele filtre por `status=DRAFT`.

Resposta: `{ "items": [...], "page": 0, "size": 20, "total": 1 }`.

Para um ANALYST, o filtro de UF é **sempre forçado ao coverage dele**: sem `state`, a busca é limitada às UFs que ele cobre; com `state` fora do coverage, a resposta é 403.

## Fluxo da solicitação

```
DRAFT ──submit──> SUBMITTED ──start──> IN_REVIEW ──decide──> APPROVED
                                                       └────> REJECTED
```

### Etapas e regras

| Etapa | Campos | Regras |
|---|---|---|
| 1. Dados básicos | `serviceType`, `title`, `description` | `serviceType`: INSTALLATION, MAINTENANCE ou INSPECTION. `title`: 3 a 80 caracteres. `description`: 20 a 1000 caracteres. Textos recebem `trim` antes da validação |
| 2. Endereço | `cep`, `number` | O CEP é consultado no ViaCEP, que preenche rua, bairro, cidade e UF. `number`: 1 a 20 caracteres (aceita `12A`). CEP inválido ou ViaCEP indisponível: a etapa não é concluída |
| 3. Confirmação | `priority`, `preferredDate`, `estimatedValue`, `termsAccepted` | `priority`: LOW, MEDIUM ou HIGH. `preferredDate` não pode estar no passado. `estimatedValue` ≥ 0 (≥ 100 quando a prioridade é HIGH). `termsAccepted` precisa ser `true`: com `false` o próprio `PUT /step3` responde 400 |

- Só é possível editar uma solicitação em `DRAFT`.
- `currentStep` guarda a etapa mais avançada já salva (1, 2 ou 3) e **nunca regride**: reeditar o step 1 depois de concluir o 3 mantém `currentStep = 3`.
- O `submit` valida todas as etapas e regras. Se algo faltar, a resposta (400) lista quais etapas ou campos estão incompletos.
- Toda criação e atualização (etapas, submit, start e decide) é indexada no Elasticsearch.

## Formato de erro

Os erros seguem um formato único:

```json
{
  "timestamp": "2026-11-05T12:00:00Z",
  "status": 400,
  "error": "Validation Error",
  "message": "title: size must be between 3 and 80",
  "path": "/solicitations/1/step1"
}
```

| Status | Quando |
|---|---|
| 400 | Validação, JSON malformado, parâmetro com tipo inválido (ex.: `/solicitations/abc`), `sort` inválido, regra de negócio, CEP inválido, submit incompleto |
| 401 | Sem token, token inválido ou credenciais inválidas |
| 403 | Sem permissão (role, dono ou UF fora do coverage) |
| 404 | Recurso não encontrado |
| 405 | Método HTTP não suportado na rota |
| 409 | E-mail já cadastrado ou violação de integridade |
| 415 | `Content-Type` não suportado |
| 500 | Erro inesperado (mensagem genérica, sem detalhes internos) |
| 503 | ViaCEP indisponível |

## Auditoria

As ações críticas são anotadas com `@Audit` e registradas por um aspecto na tabela `tb_audit_logs` (ação, usuário, perfil, duração, status `SUCCESS`/`ERROR` e mensagem de erro):

`CREATE_USER`, `SUBMIT_SOLICITATION`, `START_REVIEW` e `DECIDE`.

O registro é gravado em uma transação separada, então a falha de uma ação ainda gera o log de erro.

## Testes

```bash
./mvnw test
```

- **Unitários** (camada de service, com Mockito): `SolicitationServiceTest`, `AnalystServiceTest`, `AdminServiceTest` e `UserServiceTest`.
- **Integração** (`SolicitationControllerTest` e `AnalystControllerTest`): sobem PostgreSQL e Elasticsearch reais via Testcontainers e chamam os endpoints com `MockMvc`, passando pela pilha de segurança e pelo banco. O `SolicitationControllerTest` cobre o step 2 com o ViaCEP (mockado), o submit completo e incompleto e o acesso de um cliente à solicitação de outro. O `AnalystControllerTest` cobre a decisão do analista, o bloqueio por UF e por role e a busca no Elasticsearch restrita ao coverage.
- `ApplicationTests` verifica se o contexto da aplicação sobe.

Os testes de integração precisam do **Docker em execução**. A primeira execução baixa as imagens e o Elasticsearch pode levar alguns minutos para iniciar.

## Estrutura

```
src/main/java/com/martinsdev/solicitation/api
├── controller   # endpoints REST
├── service      # regras de negócio
├── repository   # JPA (PostgreSQL) e Elasticsearch
├── model        # entidades, documento de busca e enums
├── dto          # records de entrada e saída
└── infra
    ├── aop        # @Audit e aspecto de auditoria
    ├── client     # cliente do ViaCEP
    ├── exception  # exceções de domínio e handlers
    ├── handler    # tratamento global de erros
    ├── security   # JWT, filtro, configuração e seed do admin
    └── springdoc  # configuração do OpenAPI
```

## Observabilidade

O actuator roda em uma porta separada (`9001`) e expõe `health`, `info` e `prometheus`. O Prometheus do Compose coleta as métricas da aplicação em `app:9001`.

## Variáveis de ambiente

| Variável | Uso |
|---|---|
| `SPRING_DATASOURCE_DB`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Banco de dados |
| `JWT_SECRET` | Segredo de assinatura do token |
| `SPRING_ELASTICSEARCH_URIS` | URL do Elasticsearch (definida pelo Compose) |

Para rodar a aplicação fora do Docker, suba o Postgres e o Elasticsearch em `localhost` e execute `./mvnw spring-boot:run`. O `application.properties` espera o Postgres em `localhost:5432` e o Elasticsearch em `localhost:9200`; se usar o Postgres do Compose (porta `5433` no host), defina `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/solicitation_api`.
