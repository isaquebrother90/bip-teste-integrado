# Log de Sessões — bip-teste-integrado

Arquivo de progresso entre sessões. Atualizado após cada mudança significativa.

---

## Sessão 1 — 2026-04-11

### O que foi desenvolvido
- Solução completa do desafio técnico BIP (fullstack Java + Angular)
- Backend: Spring Boot 3.2.5, CRUD completo de benefícios, transferência com pessimistic + optimistic locking, SERIALIZABLE isolation
- Correção do EJB original: validação de saldo, locks ordenados por ID (anti-deadlock), impede origem == destino
- Frontend: Angular 18 standalone, reactive forms, CRUD + tela de transferência, interceptor de erros
- Infraestrutura: Docker Compose, GitHub Actions CI/CD, JaCoCo cobertura

### Estado atual
- Todos os critérios do desafio técnico atendidos (ver README.md)
- Testes: BeneficioServiceTest (422 linhas), BeneficioControllerIntegrationTest (504 linhas)

### Sessão 2 — 2026-04-11

### O que foi implementado
Todas as 6 melhorias de nível sênior implementadas:

1. **Auditoria de Transferências** ✅
   - Entidade `Transferencia` com máquina de estados (PENDENTE→PROCESSANDO→CONCLUIDA/FALHA/REVERTIDA)
   - Idempotency key (`correlacaoId` UUID) — reenvios retornam resultado original sem reprocessar
   - Snapshot de saldos antes/depois (campo obrigatório para auditoria regulatória)
   - Endpoints: `POST /v1/transferencias`, `GET /v1/transferencias`, `GET /v1/transferencias/{id}`, `GET /v1/transferencias/correlacao/{id}`, `GET /v1/beneficios/{id}/historico`
   - `BeneficioService.transferir()` marcado como `@Deprecated` — delega para `TransferenciaService`

2. **Soft Delete** ✅
   - Campos `deletadoEm` e `motivoDesativacao` na entidade `Beneficio`
   - `@SQLRestriction("deletado_em IS NULL")` — filtro automático em todas as queries JPA
   - `DELETE /v1/beneficios/{id}?motivo=...` faz soft delete
   - `PATCH /v1/beneficios/{id}/restaurar` restaura registros deletados
   - Native query `findByIdIncluindoDeletados` para contornar `@SQLRestriction` no restore

3. **Cache seletivo (Caffeine)** ✅
   - Dependências adicionadas ao pom.xml: `spring-boot-starter-cache`, `caffeine`
   - `BeneficioMetadataDTO` — DTO sem campo `valor` para uso em endpoints cacheados
   - `GET /v1/beneficios/metadata` — busca cacheada (TTL 5min), sem saldo
   - `@CacheEvict` nos métodos criar/atualizar/remover/restaurar

4. **Testes de Concorrência** ✅
   - `TransferenciaConcorrenciaTest` com 3 testes:
     - Conservação de valor (10 threads, 50 transferências simultâneas)
     - Anti-deadlock (20 pares de transferências cruzadas A↔B)
     - Idempotência (mesmo `correlacaoId` não duplica transferência)

5. **Actuator + Métricas** ✅
   - `micrometer-registry-prometheus` adicionado ao pom.xml
   - `MetricasConfig`: gauge `transferencias.concluidas.total` e `transferencias.falhas.total`
   - Counter `transferencias.total{status=sucesso|falha}` no `TransferenciaServiceImpl`
   - `application.yml` expõe `caches` endpoint do Actuator

6. **Frontend UX** ✅
   - `LoadingService` + `LoadingInterceptor` — barra de progresso global durante requisições HTTP
   - `ToastService` + `ToastComponent` — notificações snackbar (bottom-right, auto-dismiss)
   - `app.component.ts` — barra de loading animada no topo + `<app-toast>` global
   - `beneficio-list` migrado para toast (removidos `mensagem/mensagemTipo` signals inline)

### Estado atual
- Backend: todos os arquivos escritos, aguarda compilação com Java 17 (ambiente local tem Java 11)
- Frontend: build Angular concluído sem erros TypeScript ✅
- CI/CD irá compilar e testar o backend automaticamente no push

### Decisões arquiteturais registradas
- `@SQLRestriction` (Hibernate 6) vs `@Where` (Hibernate 5) — projeto usa Spring Boot 3.x/Hibernate 6, logo `@SQLRestriction` é correto
- Cache do campo `valor` foi explicitamente descartado — `BeneficioMetadataDTO` criado para isso
- `TransferenciaServiceImpl` injeta `BeneficioRepository` diretamente (não `BeneficioService`) para evitar dependência circular
- Isolamento SERIALIZABLE mantido nas transferências — correto para o desafio técnico

---

## Sessão 3 — 2026-04-11

### O que foi feito
- Atualizado `docs/DOCUMENTACAO_COMPLETA_BIP.md`:
  - Adicionada seção completa **"Decisões de Nível Sênior: Além do Desafio"** (linhas ~1267–1585)
  - Sete subseções técnicas com justificativas baseadas em práticas financeiras reais:
    1. Por que Transferência é uma Entidade de Domínio, não um Log?
    2. Por que Idempotency Key (correlacaoId) é Obrigatória?
    3. Por que Soft Delete com `@SQLRestriction`?
    4. Por que Cache Seletivo — e Por que o `valor` Nunca é Cacheado?
    5. Por que Testes de Concorrência Validam Invariantes de Negócio?
    6. Por que Métricas de Negócio e não Apenas Métricas Técnicas?
    7. Por que Loading Global e Toast Notifications no Frontend?
  - Seção **Conclusão** reescrita: cobre critérios do desafio + melhorias sênior + tabela de decisões técnicas justificadas

### Pendências
- **Java 17**: Disco cheio (exit code 112). Liberar espaço, depois: `winget install Microsoft.OpenJDK.17 --silent --accept-package-agreements --accept-source-agreements`
- **Maven PATH**: Adicionado ao `~/.bashrc`, requer novo terminal ou `source ~/.bashrc`
- **Verificação backend**: Após Java 17, rodar `cd backend-module && mvn compile` para confirmar todos os arquivos compilam
