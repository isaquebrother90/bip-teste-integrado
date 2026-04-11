# CLAUDE.md — bip-teste-integrado

## Regra de Log de Sessão (OBRIGATÓRIO)

**Após qualquer mudança significativa** (nova feature, correção, decisão arquitetural, refactor), atualize `docs/SESSOES.md` com:
- Data da sessão
- O que foi feito
- O que ficou pendente
- Decisões arquiteturais tomadas e o motivo

Isso permite retomar o trabalho em sessões futuras sem precisar re-analisar o código do zero.

## Contexto do Projeto

Sistema de gerenciamento de benefícios corporativos — desafio técnico BIP (Fullstack Java + Angular).

- **Backend**: Spring Boot 3.2.5 · Java 17 · JPA/Hibernate · H2 (dev) · PostgreSQL (prod)
- **Frontend**: Angular 18 standalone · Reactive Forms · RxJS
- **Infra**: Docker Compose · GitHub Actions CI/CD · JaCoCo

Antes de começar qualquer sessão de trabalho, leia `docs/SESSOES.md` para entender o estado atual do projeto.

## Comandos do Projeto

```bash
# Backend
cd backend-module && mvn spring-boot:run
cd backend-module && mvn test
cd backend-module && mvn verify

# Frontend
cd frontend && npm start
cd frontend && npm run test:ci
```

## Regras de Negócio Críticas

- Transferências: sempre adquirir locks na ordem crescente de ID (anti-deadlock)
- Nunca permitir saldo negativo
- Nunca cachear o campo `valor` (saldo) — risco de inconsistência financeira
- Entidade `Transferencia` é domínio de primeira classe, não apenas log
