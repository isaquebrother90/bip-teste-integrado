# Sistema de Gerenciamento de Benefícios

Sistema fullstack para gerenciamento de benefícios corporativos, desenvolvido como solução para o desafio técnico da vaga de **Pessoa Desenvolvedora Fullstack Java + Angular** na BIP.

## Visão Geral

Este projeto implementa uma aplicação completa em camadas (DB, EJB, Backend, Frontend) para gerenciamento de benefícios, com funcionalidades de CRUD e transferência de valores entre benefícios.

Além de atender todos os requisitos do desafio, o projeto inclui **melhorias de nível sênior** que demonstram domínio de práticas reais do mercado financeiro — documentadas na seção [Além do Desafio](#além-do-desafio-melhorias-de-nível-sênior) abaixo.

### Tecnologias Utilizadas

#### Backend
- **Java 17** com **Spring Boot 3.2.5**
- **Spring Data JPA** / Hibernate 6
- **H2 Database** (desenvolvimento) / **PostgreSQL** (produção)
- **OpenAPI/Swagger** para documentação da API
- **JUnit 5** + **Mockito** para testes
- **MapStruct** para mapeamento DTO/Entity
- **Lombok** para redução de boilerplate
- **Caffeine** para cache em memória
- **Micrometer + Prometheus** para métricas

#### Frontend
- **Angular 18** com **TypeScript**
- Componentes **Standalone**
- **Reactive Forms** para formulários
- **RxJS** para programação reativa
- **SCSS** para estilização

#### DevOps
- **Docker** e **Docker Compose**
- **GitHub Actions** para CI/CD
- **JaCoCo** para cobertura de código

## Estrutura do Projeto

```
bip-teste-integrado/
├── backend-module/          # API REST Spring Boot
│   ├── src/main/java/
│   │   └── com/bip/beneficio/
│   │       ├── api/         # Controllers, DTOs, Mappers
│   │       ├── config/      # Configurações (Cache, Métricas, CORS, OpenAPI)
│   │       └── domain/      # Entities, Repositories, Services
│   └── src/test/java/       # Testes unitários, integração e concorrência
├── ejb-module/              # Módulo EJB (corrigido)
├── frontend/                # Aplicação Angular
│   └── src/app/
│       ├── core/            # Serviços, modelos e interceptors
│       └── features/        # Componentes de features
├── db/                      # Scripts SQL
├── docs/                    # Documentação técnica completa
├── .github/workflows/       # CI/CD Pipeline
└── docker-compose.yml       # Orquestração Docker
```

## Como Executar

### Pré-requisitos
- Java 17+
- Node.js 20+
- Maven 3.9+
- Docker e Docker Compose (opcional)

### Desenvolvimento Local

#### Backend

```bash
cd backend-module
mvn spring-boot:run
```

A API estará disponível em `http://localhost:8080/api`

Documentação Swagger: `http://localhost:8080/api/swagger-ui.html`

#### Frontend

```bash
cd frontend
npm install
npm start
```

A aplicação estará disponível em `http://localhost:4200`

### Com Docker

```bash
docker-compose up -d
```

- Frontend: `http://localhost:80`
- Backend API: `http://localhost:8080/api`
- PostgreSQL: `localhost:5432`

## API Endpoints

### Benefícios

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| GET | `/api/v1/beneficios` | Lista benefícios (paginado) |
| GET | `/api/v1/beneficios/{id}` | Busca por ID |
| GET | `/api/v1/beneficios/buscar?nome=` | Busca por nome |
| GET | `/api/v1/beneficios/metadata?nome=` | Busca metadados cacheados (sem saldo) |
| POST | `/api/v1/beneficios` | Cria benefício |
| PUT | `/api/v1/beneficios/{id}` | Atualiza benefício |
| DELETE | `/api/v1/beneficios/{id}?motivo=` | Remove benefício (soft delete) |
| PATCH | `/api/v1/beneficios/{id}/ativar` | Ativa benefício |
| PATCH | `/api/v1/beneficios/{id}/desativar` | Desativa benefício |
| PATCH | `/api/v1/beneficios/{id}/restaurar` | Restaura benefício removido |
| POST | `/api/v1/beneficios/transferir` | Transfere valor (legado) |
| GET | `/api/v1/beneficios/{id}/historico` | Histórico de transferências do benefício |

### Transferências (Auditoria)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/v1/transferencias` | Realiza transferência com rastreamento completo |
| GET | `/api/v1/transferencias` | Lista todas as transferências (paginado) |
| GET | `/api/v1/transferencias/{id}` | Busca transferência por ID |
| GET | `/api/v1/transferencias/correlacao/{id}` | Busca por chave de idempotência |

### Observabilidade (Actuator)

| Endpoint | Descrição |
|----------|-----------|
| `/api/actuator/health` | Status da aplicação |
| `/api/actuator/metrics` | Métricas técnicas e de negócio |
| `/api/actuator/prometheus` | Métricas no formato Prometheus |
| `/api/actuator/caches` | Estado dos caches em memória |

## Correção do Bug no EJB

O código original do `BeneficioEjbService` apresentava os seguintes problemas:

### Problemas Identificados

1. **Sem validação de saldo** - Permitia saldo negativo
2. **Sem locking** - Possível lost update em concorrência
3. **Sem validação de existência** - Não verificava se origem/destino existiam
4. **Permitia origem == destino** - Transferência para o mesmo benefício
5. **Sem tratamento transacional adequado** - Risco de inconsistência

### Soluções Implementadas

1. **Validação de saldo** via método `possuiSaldoSuficiente()` antes do débito
2. **Pessimistic Locking** via `LockModeType.PESSIMISTIC_WRITE` + **Optimistic Locking** via `@Version`
3. **Validação de existência** com lançamento de exceção apropriada
4. **Validação de origem != destino**
5. **Ordenação de locks** (menor ID primeiro) para evitar deadlocks
6. **Transação com isolamento SERIALIZABLE** para garantir consistência

## Testes

### Backend

```bash
cd backend-module
mvn test                    # Testes unitários
mvn verify                  # Testes de integração
mvn jacoco:report           # Relatório de cobertura
```

### Frontend

```bash
cd frontend
npm test                    # Testes com watch
npm run test:ci             # Testes CI (headless)
```

## Critérios de Avaliação Atendidos

| Critério | Peso | Status |
|----------|------|--------|
| Arquitetura em camadas | 20% | ✅ Implementado |
| Correção EJB | 20% | ✅ Corrigido |
| CRUD + Transferência | 15% | ✅ Implementado |
| Qualidade de código | 10% | ✅ SOLID, Clean Code |
| Testes | 15% | ✅ Unit + Integration + **Concorrência** |
| Documentação | 10% | ✅ Swagger + README + Docs técnicos |
| Frontend | 10% | ✅ Angular 18 |

---

## Melhorias além do desafio

As funcionalidades abaixo não foram solicitadas no desafio técnico. Foram adicionadas por serem práticas comuns em sistemas financeiros em produção.

### 1. Transferência como Domínio de Primeira Classe

**O que foi feito:** A transferência deixou de ser apenas uma operação com efeito colateral e passou a ser uma **entidade de domínio com ciclo de vida próprio**.

**Por que importa em sistemas financeiros:**
- Reguladores (Bacen, auditores) exigem rastreabilidade completa de cada movimentação
- Sem entidade dedicada, a pergunta "qual era o saldo exato no momento da transferência?" não tem resposta
- A máquina de estados (PENDENTE → PROCESSANDO → CONCLUIDA/FALHA/REVERTIDA) permite identificar transferências travadas ou com falha parcial

**O que foi implementado:**
- Entidade `Transferencia` com snapshot dos saldos **antes e depois** de cada operação
- Máquina de estados: `PENDENTE → PROCESSANDO → CONCLUIDA / FALHA / REVERTIDA`
- **Idempotency Key** (`correlacaoId` UUID): reenviar a mesma requisição retorna o resultado original sem reprocessar — essencial para sistemas com retry automático
- Endpoints de auditoria: `GET /v1/transferencias`, `GET /v1/beneficios/{id}/historico`
- O endpoint legado `POST /v1/beneficios/transferir` foi mantido por retrocompatibilidade, delegando internamente para o novo `TransferenciaService`

---

### 2. Soft Delete com Motivo de Desativação

**O que foi feito:** Registros de benefícios nunca são deletados fisicamente — são marcados com `deletadoEm` e `motivoDesativacao`.

**Por que importa em sistemas financeiros:**
- Um benefício que teve transferências **não pode desaparecer** do banco — quebraria a integridade histórica da auditoria
- LGPD exige distinção entre "anonimização" e "exclusão" — soft delete preserva dados para compliance sem expô-los ao usuário
- O campo `motivoDesativacao` cria rastreabilidade operacional ("Encerrado por reestruturação", "Duplicado do benefício #42")

**O que foi implementado:**
- Campos `deletadoEm` e `motivoDesativacao` na entidade `Beneficio`
- `@SQLRestriction("deletado_em IS NULL")` — filtra automaticamente todos os registros deletados em **qualquer** query JPA, sem risco de esquecer o filtro
- `DELETE /v1/beneficios/{id}?motivo=...` realiza soft delete
- `PATCH /v1/beneficios/{id}/restaurar` restaura registros deletados (via native query que ignora o `@SQLRestriction`)

---

### 3. Cache Seletivo — Sem Cachear Saldo

**O que foi feito:** Cache em memória com Caffeine, aplicado **somente a metadados** (nome, descrição, status ativo). O campo `valor` (saldo) **nunca é cacheado**.

**Por que importa em sistemas financeiros:**
- Cachear saldo é um erro clássico e grave: o cache pode mostrar R$1.000 enquanto o banco já tem R$0 — levando à aprovação de uma transferência inválida
- O endpoint `GET /v1/beneficios/metadata` devolve dados sem saldo, com TTL de 5 minutos — seguro para autocomplete, filtros e buscas
- O endpoint `GET /v1/beneficios/{id}` sempre consulta o banco diretamente — garantindo saldo sempre atualizado

**O que foi implementado:**
- `BeneficioMetadataDTO` — DTO explicitamente sem o campo `valor`, documentado com aviso no Swagger
- `CacheConfig` com Caffeine (TTL 5 min, máx 500 entradas, stats habilitadas)
- `@CacheEvict` em todos os métodos de escrita (criar, atualizar, remover, restaurar)

---

### 4. Testes de Concorrência com Invariantes de Negócio

**O que foi feito:** Testes que provam a corretude do sistema sob carga concorrente, validando invariantes financeiros fundamentais.

**Por que importa em sistemas financeiros:**
- Testes unitários com mocks não detectam race conditions — o mock nunca vai concorrer consigo mesmo
- A única forma de provar que o mecanismo de locking funciona é testá-lo com threads reais e verificar os invariantes de negócio

**O que foi implementado:**
- **Conservação de valor:** 10 threads executando 50 transferências simultâneas — a soma dos saldos deve ser idêntica antes e depois
- **Ausência de saldo negativo:** nenhum benefício pode terminar com saldo < 0 após qualquer combinação de transferências
- **Anti-deadlock:** 20 pares de transferências cruzadas (A→B e B→A simultaneamente) — todas devem terminar sem travar
- **Idempotência:** reenviar a mesma `correlacaoId` não duplica a transferência

---

### 5. Actuator + Métricas de Negócio

**O que foi feito:** Observabilidade com foco em métricas de negócio, não apenas técnicas.

**Por que importa em sistemas financeiros:**
- Métricas de infraestrutura (CPU, memória) não detectam anomalias de negócio — um spike de `transferencias.falhas` pode indicar um bug ou até tentativa de fraude
- Integração com Prometheus/Grafana é padrão em qualquer empresa que opera 24/7

**O que foi implementado:**
- `GET /actuator/health` — status da aplicação
- `GET /actuator/metrics/transferencias.total` — contador de transferências por status (`sucesso` / `falha`)
- `GET /actuator/metrics/transferencias.concluidas.total` — gauge com total histórico de transferências concluídas
- `GET /actuator/prometheus` — scrape endpoint para Prometheus/Grafana
- `GET /actuator/caches` — visibilidade do estado do cache em tempo real

---

### 6. Frontend: Loading Global + Toast Notifications

**O que foi feito:** Experiência de usuário adequada para um sistema financeiro — onde o usuário precisa saber claramente o resultado de cada operação.

**Por que importa:**
- Em sistemas financeiros, ambiguidade de UX é perigosa: o usuário não saber se uma transferência foi processada pode levá-lo a enviar novamente
- Confirmação explícita antes de operações destrutivas é requisito de usabilidade básico

**O que foi implementado:**
- **Barra de progresso global** no topo da página durante qualquer requisição HTTP (`LoadingInterceptor` + `LoadingService`)
- **Toast notifications** (bottom-right, auto-dismiss) substituindo mensagens inline — `ToastService` + `ToastComponent` disponíveis globalmente
- **Modal de confirmação** antes de deletar um benefício, exibindo o nome do registro

---

## Documentação Técnica

Para detalhes de implementação, decisões arquiteturais e explicações técnicas aprofundadas:

- [`docs/DOCUMENTACAO_COMPLETA_BIP.md`](docs/DOCUMENTACAO_COMPLETA_BIP.md) — Documentação técnica completa
- `http://localhost:8080/api/swagger-ui.html` — Documentação interativa da API

## Autor

Desenvolvido como solução para o desafio técnico da BIP.

## Licença

Este projeto é disponibilizado exclusivamente para fins de avaliação técnica.
