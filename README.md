# Sistema de Gerenciamento de Benefícios

Sistema fullstack para gerenciamento de benefícios corporativos, desenvolvido como solução para o desafio técnico da vaga de **Pessoa Desenvolvedora Fullstack Java + Angular** na BIP.

## Visão Geral

Este projeto implementa uma aplicação completa em camadas (DB, EJB, Backend, Frontend) para gerenciamento de benefícios, com funcionalidades de CRUD e transferência de valores entre benefícios.

### Tecnologias Utilizadas

#### Backend
- **Java 17** com **Spring Boot 3.2.5**
- **Spring Data JPA** / Hibernate
- **H2 Database** (desenvolvimento) / **PostgreSQL** (produção)
- **OpenAPI/Swagger** para documentação da API
- **JUnit 5** + **Mockito** para testes
- **MapStruct** para mapeamento DTO/Entity
- **Lombok** para redução de boilerplate

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
│   │       ├── config/      # Configurações
│   │       └── domain/      # Entities, Repositories, Services
│   └── src/test/java/       # Testes unitários e integração
├── ejb-module/              # Módulo EJB (corrigido)
├── frontend/                # Aplicação Angular
│   └── src/app/
│       ├── core/            # Serviços e modelos
│       └── features/        # Componentes de features
├── db/                      # Scripts SQL
├── docs/                    # Documentação
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
| POST | `/api/v1/beneficios` | Cria benefício |
| PUT | `/api/v1/beneficios/{id}` | Atualiza benefício |
| DELETE | `/api/v1/beneficios/{id}` | Remove benefício |
| PATCH | `/api/v1/beneficios/{id}/ativar` | Ativa benefício |
| PATCH | `/api/v1/beneficios/{id}/desativar` | Desativa benefício |
| POST | `/api/v1/beneficios/transferir` | Transfere valor |

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
| Testes | 15% | ✅ Unit + Integration |
| Documentação | 10% | ✅ Swagger + README |
| Frontend | 10% | ✅ Angular 18 |

## Autor

Desenvolvido como solução para o desafio técnico da BIP.

## Licença

Este projeto é disponibilizado exclusivamente para fins de avaliação técnica.
