# Documentação Técnica Completa - Sistema de Gerenciamento de Benefícios

## Índice

1. [Visão Geral](#visão-geral)
2. [Arquitetura](#arquitetura)
3. [Decisões Técnicas](#decisões-técnicas)
4. [Estrutura do Código](#estrutura-do-código)
5. [Padrões de Design](#padrões-de-design)
6. [Controle de Concorrência](#controle-de-concorrência)
7. [Validações e Regras de Negócio](#validações-e-regras-de-negócio)
8. [API REST](#api-rest)
9. [Frontend Angular](#frontend-angular)
10. [Banco de Dados](#banco-de-dados)
11. [Testes](#testes)
12. [Correção do Bug Original](#correção-do-bug-original)

---

## Visão Geral

Sistema fullstack para gerenciamento de benefícios corporativos, desenvolvido como solução para o desafio técnico da BIP. O sistema permite realizar operações CRUD completas sobre benefícios e executar transferências de valores entre benefícios de forma segura e consistente.

### Objetivos do Projeto

- Implementar arquitetura em camadas (DB, EJB, Backend, Frontend)
- Corrigir bugs críticos no código EJB original
- Garantir consistência de dados em operações concorrentes
- Fornecer API REST robusta e bem documentada
- Criar interface de usuário moderna e responsiva

### Stack Tecnológico

#### Backend
- Java 17
- Spring Boot 3.2.5
- Spring Data JPA
- H2 Database (dev) / PostgreSQL (prod)
- MapStruct (mapeamento DTO/Entity)
- Lombok (redução de boilerplate)
- OpenAPI/Swagger (documentação)
- JUnit 5 + Mockito (testes)

#### Frontend
- Angular 18
- TypeScript
- Standalone Components
- Reactive Forms
- RxJS
- SCSS

#### DevOps
- Docker / Docker Compose
- GitHub Actions (CI/CD)
- JaCoCo (cobertura de código)

---

## Arquitetura

### Arquitetura em Camadas

```
┌─────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER                    │
│              (Angular 18 - Frontend SPA)                │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP/REST
┌────────────────────┴────────────────────────────────────┐
│                     API LAYER                           │
│          (Controllers + DTOs + Mappers)                 │
│  - BeneficioController                                  │
│  - GlobalExceptionHandler                               │
│  - OpenAPI/Swagger Documentation                        │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│                   SERVICE LAYER                         │
│               (Business Logic)                          │
│  - BeneficioService / BeneficioServiceImpl              │
│  - Validações de negócio                                │
│  - Coordenação de transações                            │
│  - Gerenciamento de locks                               │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│                  REPOSITORY LAYER                       │
│              (Data Access)                              │
│  - BeneficioRepository (Spring Data JPA)                │
│  - Pessimistic/Optimistic Locking                       │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│                   DOMAIN LAYER                          │
│                  (Entities)                             │
│  - Beneficio (JPA Entity)                               │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────────────┐
│                DATABASE LAYER                           │
│       H2 (dev) / PostgreSQL (prod)                      │
└─────────────────────────────────────────────────────────┘
```

### Separação de Responsabilidades

#### Controllers (API Layer)
- Recebem requisições HTTP
- Validam entrada (@Valid)
- Delegam para Services
- Retornam ResponseEntity<DTO>
- Não contêm lógica de negócio

#### Services (Business Layer)
- **Contêm TODA a lógica de negócio**
- Coordenam transações (@Transactional)
- Gerenciam locks (pessimistic + optimistic)
- Validam regras de negócio
- Convertem entre Entity e DTO (via Mapper)

#### Repositories (Data Access Layer)
- Interface Spring Data JPA
- Queries customizadas (JPQL)
- Métodos com pessimistic lock
- Não contêm lógica de negócio

#### Entities (Domain Layer)
- Mapeamento JPA (@Entity)
- Apenas getters/setters (Lombok)
- Campos com annotations de validação
- **Sem lógica de negócio** (Anemic Domain Model)

---

## Decisões Técnicas

### 1. Anemic Domain Model

**Decisão**: Implementar Anemic Domain Model, onde a lógica de negócio está concentrada na camada de serviço.

**Justificativa**:
- Padrão mais comum em ambientes corporativos e bancários
- Facilita manutenção por equipes grandes
- Alinha com práticas Spring Boot convencionais
- Mais familiar para a maioria dos desenvolvedores
- Evita discussões técnicas desnecessárias

**Implementação**:
```java
// Entidade: apenas dados (POJO)
@Entity
public class Beneficio {
    private Long id;
    private String nome;
    private BigDecimal valor;
    private Boolean ativo;
    // getters/setters via Lombok
}

// Service: lógica de negócio
@Service
public class BeneficioServiceImpl {
    public void transferir(...) {
        validarMontante(valor);
        if (!possuiSaldoSuficiente(origem, valor)) {
            throw new InsufficientBalanceException(...);
        }
        debitar(origem, valor);
        creditar(destino, valor);
    }

    private void validarMontante(BigDecimal montante) { ... }
    private boolean possuiSaldoSuficiente(...) { ... }
    private void debitar(...) { ... }
    private void creditar(...) { ... }
}
```

### 2. Duplo Controle de Concorrência

**Decisão**: Utilizar Optimistic Locking (@Version) + Pessimistic Locking (SELECT FOR UPDATE).

**Justificativa**:
- **Optimistic**: Melhor performance em cenários de baixa contenção
- **Pessimistic**: Garante consistência em transferências (operações críticas)
- Defesa em profundidade contra race conditions

**Implementação**:
```java
// Optimistic Locking na entidade
@Entity
public class Beneficio {
    @Version
    private Long version;
}

// Pessimistic Locking no repository
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT b FROM Beneficio b WHERE b.id = :id")
Optional<Beneficio> findByIdWithLock(@Param("id") Long id);

// Uso no service
@Transactional(isolation = Isolation.SERIALIZABLE)
public TransferenciaResultadoDTO transferir(TransferenciaDTO dto) {
    Beneficio origem = repository.findByIdWithLock(dto.getOrigemId())
        .orElseThrow(...);
    Beneficio destino = repository.findByIdWithLock(dto.getDestinoId())
        .orElseThrow(...);
    // ...
}
```

### 3. Ordenação de Locks para Evitar Deadlock

**Decisão**: Adquirir locks sempre na mesma ordem (menor ID primeiro).

**Justificativa**:
- Previne deadlocks em transferências concorrentes entre os mesmos benefícios
- Padrão conhecido de prevenção de deadlock

**Implementação**:
```java
Long primeiroId = Math.min(dto.getOrigemId(), dto.getDestinoId());
Long segundoId = Math.max(dto.getOrigemId(), dto.getDestinoId());

Beneficio primeiro = repository.findByIdWithLock(primeiroId).orElseThrow(...);
Beneficio segundo = repository.findByIdWithLock(segundoId).orElseThrow(...);

Beneficio origem = dto.getOrigemId().equals(primeiroId) ? primeiro : segundo;
Beneficio destino = dto.getOrigemId().equals(primeiroId) ? segundo : primeiro;
```

### 4. Isolamento SERIALIZABLE em Transferências

**Decisão**: Usar isolamento SERIALIZABLE para operações de transferência.

**Justificativa**:
- Previne anomalias de leitura (phantom reads, non-repeatable reads)
- Garante consistência absoluta em operações financeiras
- Trade-off aceitável: performance vs. consistência (consistência ganha)

**Implementação**:
```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public TransferenciaResultadoDTO transferir(TransferenciaDTO dto) {
    // ...
}
```

### 5. MapStruct para Mapeamento DTO/Entity

**Decisão**: Utilizar MapStruct ao invés de mapeamento manual.

**Justificativa**:
- Geração de código em compile-time (performance)
- Segurança de tipos
- Menos código boilerplate
- Fácil manutenção

**Implementação**:
```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BeneficioMapper {
    BeneficioDTO toDTO(Beneficio entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    Beneficio toEntity(BeneficioCreateDTO dto);
}
```

### 6. Global Exception Handler

**Decisão**: Centralizar tratamento de exceções em @RestControllerAdvice.

**Justificativa**:
- Formato de resposta consistente (ApiErrorResponse)
- Evita duplicação de código
- Facilita logging e auditoria
- Melhora experiência do desenvolvedor

**Implementação**:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(...) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientBalance(...) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    // 12 handlers adicionais...
}
```

---

## Estrutura do Código

### Backend (Spring Boot)

```
backend-module/src/main/java/com/bip/beneficio/
├── api/
│   ├── controller/
│   │   └── BeneficioController.java          # REST endpoints
│   ├── dto/
│   │   ├── ApiErrorResponse.java             # Resposta de erro padronizada
│   │   ├── BeneficioDTO.java                 # DTO de resposta
│   │   ├── BeneficioCreateDTO.java           # DTO de criação
│   │   ├── BeneficioUpdateDTO.java           # DTO de atualização
│   │   ├── TransferenciaDTO.java             # DTO de transferência
│   │   └── TransferenciaResultadoDTO.java    # DTO resultado transferência
│   ├── handler/
│   │   └── GlobalExceptionHandler.java       # Tratamento global de erros
│   └── mapper/
│       └── BeneficioMapper.java              # MapStruct mapper
├── config/
│   ├── OpenApiConfig.java                    # Configuração Swagger
│   └── WebConfig.java                        # Configuração CORS
├── domain/
│   ├── entity/
│   │   └── Beneficio.java                    # Entidade JPA
│   ├── exception/
│   │   ├── BusinessException.java            # Exception base de negócio
│   │   ├── ResourceNotFoundException.java    # 404
│   │   ├── InsufficientBalanceException.java # Saldo insuficiente
│   │   ├── ConcurrencyException.java         # Conflito de concorrência
│   │   └── DuplicateResourceException.java   # Recurso duplicado
│   ├── repository/
│   │   └── BeneficioRepository.java          # Spring Data JPA
│   └── service/
│       ├── BeneficioService.java             # Interface do serviço
│       └── impl/
│           └── BeneficioServiceImpl.java     # Implementação do serviço
└── BeneficioApiApplication.java              # Classe principal
```

### Frontend (Angular)

```
frontend/src/app/
├── core/
│   ├── interceptors/
│   │   └── error.interceptor.ts              # Interceptor de erros HTTP
│   ├── models/
│   │   └── beneficio.model.ts                # Interfaces TypeScript
│   └── services/
│       └── beneficio.service.ts              # Service HTTP
├── features/
│   └── beneficios/
│       ├── beneficio-list/                   # Lista de benefícios
│       ├── beneficio-form/                   # Formulário CRUD
│       └── transferencia-form/               # Formulário transferência
└── app.component.ts                          # Componente raiz
```

---

## Padrões de Design

### 1. Repository Pattern
- Abstrai acesso a dados
- Implementado via Spring Data JPA
- Permite queries customizadas

### 2. DTO Pattern
- Separa modelo de domínio (Entity) do modelo de transferência (DTO)
- Evita exposição de detalhes internos
- Permite versioning de API

### 3. Mapper Pattern
- Converte entre Entity e DTO
- Implementado com MapStruct
- Reduz código boilerplate

### 4. Service Layer Pattern
- Encapsula lógica de negócio
- Coordena transações
- Orquestra múltiplas operações

### 5. Exception Handler Pattern
- Centraliza tratamento de exceções
- Garante formato consistente de resposta
- Facilita logging

### 6. Builder Pattern
- Construção de objetos complexos (DTOs)
- Implementado via Lombok @Builder
- Melhora legibilidade

---

## Controle de Concorrência

### Problema Original (Bug do EJB)

```java
// ❌ CÓDIGO BUGADO ORIGINAL
public void transferir(Long origemId, Long destinoId, BigDecimal valor) {
    Beneficio origem = em.find(Beneficio.class, origemId);
    Beneficio destino = em.find(Beneficio.class, destinoId);

    // SEM validação de saldo!
    origem.setValor(origem.getValor().subtract(valor));
    destino.setValor(destino.getValor().add(valor));

    em.merge(origem);  // SEM lock - possível lost update!
    em.merge(destino);
}
```

**Problemas**:
1. ❌ Sem validação de saldo (saldo negativo possível)
2. ❌ Sem locking (lost updates em concorrência)
3. ❌ Sem validação de existência
4. ❌ Permite origem == destino
5. ❌ Sem ordenação de locks (deadlock possível)

### Solução Implementada

```java
// ✅ CÓDIGO CORRIGIDO
@Transactional(isolation = Isolation.SERIALIZABLE)
public TransferenciaResultadoDTO transferir(TransferenciaDTO dto) {
    // ✅ 1. Validação origem != destino
    if (dto.getOrigemId().equals(dto.getDestinoId())) {
        throw new BusinessException("Origem e destino não podem ser iguais");
    }

    // ✅ 2. Ordenação de locks (menor ID primeiro)
    Long primeiroId = Math.min(dto.getOrigemId(), dto.getDestinoId());
    Long segundoId = Math.max(dto.getOrigemId(), dto.getDestinoId());

    // ✅ 3. Pessimistic lock (SELECT ... FOR UPDATE)
    Beneficio primeiro = repository.findByIdWithLock(primeiroId)
        .orElseThrow(() -> new ResourceNotFoundException(...));
    Beneficio segundo = repository.findByIdWithLock(segundoId)
        .orElseThrow(() -> new ResourceNotFoundException(...));

    Beneficio origem = dto.getOrigemId().equals(primeiroId) ? primeiro : segundo;
    Beneficio destino = dto.getOrigemId().equals(primeiroId) ? segundo : primeiro;

    // ✅ 4. Validação de montante
    validarMontante(dto.getValor());

    // ✅ 5. Validação de saldo
    if (!possuiSaldoSuficiente(origem, dto.getValor())) {
        throw new InsufficientBalanceException(...);
    }

    // ✅ 6. Operações com validações
    debitar(origem, dto.getValor());
    creditar(destino, dto.getValor());

    // ✅ 7. Persistência dentro da transação
    repository.save(origem);
    repository.save(destino);

    return TransferenciaResultadoDTO.builder()...;
}
```

### Tipos de Lock Utilizados

#### Optimistic Locking (@Version)

```java
@Entity
public class Beneficio {
    @Version
    private Long version;
}
```

**Funcionamento**:
1. JPA adiciona `WHERE version = :version` em UPDATEs
2. Se version mudou, `ObjectOptimisticLockingFailureException`
3. Frontend deve recarregar dados e tentar novamente

**Quando usar**:
- Operações normais (CRUD)
- Baixa contenção esperada
- Melhor performance

#### Pessimistic Locking (SELECT FOR UPDATE)

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT b FROM Beneficio b WHERE b.id = :id")
Optional<Beneficio> findByIdWithLock(@Param("id") Long id);
```

**Funcionamento**:
1. Executa `SELECT ... FOR UPDATE`
2. Bloqueia linha até fim da transação
3. Outras transações esperam

**Quando usar**:
- Operações críticas (transferências)
- Alta contenção esperada
- Consistência é prioridade

### Níveis de Isolamento

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
```

**Isolamento SERIALIZABLE**:
- Mais alto nível de isolamento
- Previne: dirty reads, non-repeatable reads, phantom reads
- Garante execução como se fosse serial
- Trade-off: menor concorrência, maior consistência

---

## Validações e Regras de Negócio

### Validações de Entrada (Bean Validation)

```java
@Data
public class BeneficioCreateDTO {
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    private String nome;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.00", message = "Valor não pode ser negativo")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal valor;
}
```

### Validações de Negócio (Service Layer)

```java
private void validarMontante(BigDecimal montante) {
    if (montante == null) {
        throw new IllegalArgumentException("Montante não pode ser nulo");
    }
    if (montante.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("Montante deve ser maior que zero");
    }
}

private boolean possuiSaldoSuficiente(Beneficio beneficio, BigDecimal montante) {
    return beneficio.getValor() != null &&
           montante != null &&
           beneficio.getValor().compareTo(montante) >= 0;
}
```

### Regras de Negócio Implementadas

1. **Nome único**: Não pode existir dois benefícios com o mesmo nome
2. **Valor não negativo**: Valor deve ser >= 0
3. **Montante positivo**: Transferências devem ter valor > 0
4. **Saldo suficiente**: Débito só se houver saldo
5. **Origem != Destino**: Não pode transferir para si mesmo
6. **Benefício existe**: Validação de existência antes de operações

---

## API REST

### Padrão de Endpoints

```
Base URL: http://localhost:8080/api/v1/beneficios
```

### CRUD Completo

#### Listar Benefícios
```http
GET /api/v1/beneficios?page=0&size=10&apenasAtivos=false
```

**Response**:
```json
{
  "content": [
    {
      "id": 1,
      "nome": "Vale Alimentação",
      "descricao": "Benefício para alimentação",
      "valor": 800.00,
      "ativo": true,
      "version": 0,
      "criadoEm": "2024-04-11T10:00:00",
      "atualizadoEm": "2024-04-11T10:00:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0
}
```

#### Buscar por ID
```http
GET /api/v1/beneficios/1
```

#### Buscar por Nome
```http
GET /api/v1/beneficios/buscar?nome=alimentacao&page=0&size=10
```

#### Criar Benefício
```http
POST /api/v1/beneficios
Content-Type: application/json

{
  "nome": "Vale Alimentação",
  "descricao": "Benefício para alimentação diária",
  "valor": 800.00,
  "ativo": true
}
```

#### Atualizar Benefício
```http
PUT /api/v1/beneficios/1
Content-Type: application/json

{
  "nome": "Vale Alimentação",
  "descricao": "Benefício para alimentação diária",
  "valor": 900.00,
  "ativo": true,
  "version": 0
}
```

#### Remover Benefício
```http
DELETE /api/v1/beneficios/1
```

#### Ativar Benefício
```http
PATCH /api/v1/beneficios/1/ativar
```

#### Desativar Benefício
```http
PATCH /api/v1/beneficios/1/desativar
```

### Transferência

```http
POST /api/v1/beneficios/transferir
Content-Type: application/json

{
  "origemId": 1,
  "destinoId": 2,
  "valor": 100.00
}
```

**Response Success**:
```json
{
  "sucesso": true,
  "mensagem": "Transferência realizada com sucesso",
  "valorTransferido": 100.00,
  "saldoOrigem": 700.00,
  "saldoDestino": 1100.00,
  "dataTransferencia": "2024-04-11T10:30:00"
}
```

**Response Error (Saldo Insuficiente)**:
```json
{
  "timestamp": "2024-04-11T10:30:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "errorCode": "INSUFFICIENT_BALANCE",
  "message": "Saldo insuficiente no benefício 1. Saldo atual: 50.00, Valor solicitado: 100.00",
  "path": "/api/v1/beneficios/transferir"
}
```

### Tratamento de Erros

| HTTP Status | Error Code | Descrição |
|------------|------------|-----------|
| 400 | VALIDATION_ERROR | Validação de campos |
| 400 | INVALID_JSON | JSON malformado |
| 400 | MISSING_PARAMETER | Parâmetro faltando |
| 400 | TYPE_MISMATCH | Tipo de parâmetro errado |
| 404 | RESOURCE_NOT_FOUND | Recurso não encontrado |
| 404 | ENDPOINT_NOT_FOUND | Endpoint não existe |
| 405 | METHOD_NOT_ALLOWED | Método HTTP incorreto |
| 409 | CONCURRENCY_CONFLICT | Conflito de concorrência |
| 409 | DUPLICATE_RESOURCE | Recurso duplicado |
| 409 | DATA_INTEGRITY_ERROR | Violação de integridade |
| 415 | UNSUPPORTED_MEDIA_TYPE | Content-Type incorreto |
| 422 | INSUFFICIENT_BALANCE | Saldo insuficiente |
| 422 | BUSINESS_ERROR | Erro de regra de negócio |
| 500 | INTERNAL_ERROR | Erro interno |

---

## Frontend Angular

### Arquitetura

- **Standalone Components**: Sem módulos, arquitetura moderna
- **Reactive Forms**: Validação reativa
- **RxJS**: Programação reativa, observables
- **Service Pattern**: BeneficioService centraliza chamadas HTTP
- **Error Interceptor**: Tratamento global de erros

### Componentes Principais

#### BeneficioListComponent
- Lista paginada de benefícios
- Filtros (ativo/inativo, busca por nome)
- Ações: criar, editar, remover, ativar/desativar

#### BeneficioFormComponent
- Formulário de criação/edição
- Validações reativas
- Tratamento de erros

#### TransferenciaFormComponent
- Seleção de origem e destino
- Validação de valor
- Feedback visual de sucesso/erro

### Service Layer

```typescript
@Injectable({
  providedIn: 'root'
})
export class BeneficioService {
  private readonly baseUrl = `${environment.apiUrl}/v1/beneficios`;

  listar(page: number, size: number, apenasAtivos: boolean): Observable<PageResponse<Beneficio>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('apenasAtivos', apenasAtivos.toString());
    return this.http.get<PageResponse<Beneficio>>(this.baseUrl, { params });
  }

  transferir(transferencia: Transferencia): Observable<TransferenciaResultado> {
    return this.http.post<TransferenciaResultado>(`${this.baseUrl}/transferir`, transferencia);
  }
}
```

### Error Interceptor

```typescript
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'Ocorreu um erro inesperado.';

      if (error.error?.message) {
        errorMessage = error.error.message;
      } else {
        switch (error.status) {
          case 404:
            errorMessage = 'Recurso não encontrado.';
            break;
          case 409:
            errorMessage = 'Conflito de dados. O registro pode ter sido modificado.';
            break;
          // ...
        }
      }

      return throwError(() => ({ ...error.error, userMessage: errorMessage }));
    })
  );
};
```

---

## Banco de Dados

### Schema

```sql
CREATE TABLE beneficio (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(255),
    valor DECIMAL(15,2) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT true,
    version BIGINT,
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL
);

CREATE INDEX idx_beneficio_nome ON beneficio(nome);
CREATE INDEX idx_beneficio_ativo ON beneficio(ativo);
```

### Configurações

#### H2 (Desenvolvimento)
```properties
spring.datasource.url=jdbc:h2:mem:beneficiodb
spring.datasource.driverClassName=org.h2.Driver
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.h2.console.enabled=true
```

#### PostgreSQL (Produção)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/beneficiodb
spring.datasource.username=beneficio_user
spring.datasource.password=beneficio_pass
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

---

## Testes

### Backend - Testes Unitários

```java
@ExtendWith(MockitoExtension.class)
class BeneficioServiceImplTest {

    @Mock
    private BeneficioRepository repository;

    @Mock
    private BeneficioMapper mapper;

    @InjectMocks
    private BeneficioServiceImpl service;

    @Test
    void deveTransferirComSucesso() {
        // Given
        Beneficio origem = Beneficio.builder()
            .id(1L).valor(new BigDecimal("1000")).build();
        Beneficio destino = Beneficio.builder()
            .id(2L).valor(new BigDecimal("500")).build();

        when(repository.findByIdWithLock(1L)).thenReturn(Optional.of(origem));
        when(repository.findByIdWithLock(2L)).thenReturn(Optional.of(destino));

        // When
        TransferenciaDTO dto = new TransferenciaDTO(1L, 2L, new BigDecimal("100"));
        TransferenciaResultadoDTO resultado = service.transferir(dto);

        // Then
        assertThat(resultado.isSucesso()).isTrue();
        assertThat(resultado.getSaldoOrigem()).isEqualTo(new BigDecimal("900"));
        assertThat(resultado.getSaldoDestino()).isEqualTo(new BigDecimal("600"));
    }

    @Test
    void deveLancarExcecaoQuandoSaldoInsuficiente() {
        // Given
        Beneficio origem = Beneficio.builder()
            .id(1L).valor(new BigDecimal("50")).build();
        Beneficio destino = Beneficio.builder()
            .id(2L).valor(new BigDecimal("500")).build();

        when(repository.findByIdWithLock(1L)).thenReturn(Optional.of(origem));
        when(repository.findByIdWithLock(2L)).thenReturn(Optional.of(destino));

        // When / Then
        TransferenciaDTO dto = new TransferenciaDTO(1L, 2L, new BigDecimal("100"));
        assertThatThrownBy(() -> service.transferir(dto))
            .isInstanceOf(InsufficientBalanceException.class);
    }
}
```

### Cobertura de Código

Meta: **>80%** de cobertura

```bash
mvn clean test jacoco:report
```

Relatório: `target/site/jacoco/index.html`

---

## Correção do Bug Original

### Análise do Código EJB Original

```java
@Stateless
public class BeneficioEjbService {

    @PersistenceContext
    private EntityManager em;

    // ❌ CÓDIGO PROBLEMÁTICO
    public void transferirValor(Long origemId, Long destinoId, BigDecimal valor) {
        Beneficio origem = em.find(Beneficio.class, origemId);
        Beneficio destino = em.find(Beneficio.class, destinoId);

        // PROBLEMA 1: Sem validação de saldo
        origem.setValor(origem.getValor().subtract(valor));

        // PROBLEMA 2: Sem validação de existência
        destino.setValor(destino.getValor().add(valor));

        // PROBLEMA 3: Sem locking (lost updates possíveis)
        em.merge(origem);
        em.merge(destino);

        // PROBLEMA 4: Permite origem == destino
        // PROBLEMA 5: Sem ordenação de locks (deadlock possível)
    }
}
```

### Problemas Identificados

| # | Problema | Impacto | Severidade |
|---|----------|---------|------------|
| 1 | Sem validação de saldo | Saldo negativo possível | **CRÍTICO** |
| 2 | Sem controle de concorrência | Lost updates em operações simultâneas | **CRÍTICO** |
| 3 | Sem validação de existência | NullPointerException se ID inválido | **ALTO** |
| 4 | Permite origem == destino | Transferência inválida aceita | **MÉDIO** |
| 5 | Sem ordenação de locks | Deadlocks em operações concorrentes | **ALTO** |
| 6 | Sem isolamento transacional | Race conditions possíveis | **ALTO** |

### Soluções Implementadas

#### 1. Validação de Saldo

```java
// Antes: ❌
origem.setValor(origem.getValor().subtract(valor));

// Depois: ✅
private boolean possuiSaldoSuficiente(Beneficio beneficio, BigDecimal montante) {
    return beneficio.getValor() != null &&
           montante != null &&
           beneficio.getValor().compareTo(montante) >= 0;
}

if (!possuiSaldoSuficiente(origem, valor)) {
    throw new InsufficientBalanceException(...);
}
```

#### 2. Controle de Concorrência (Optimistic + Pessimistic)

```java
// Optimistic Lock
@Entity
public class Beneficio {
    @Version
    private Long version;
}

// Pessimistic Lock
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT b FROM Beneficio b WHERE b.id = :id")
Optional<Beneficio> findByIdWithLock(@Param("id") Long id);
```

#### 3. Validação de Existência

```java
// Antes: ❌
Beneficio origem = em.find(Beneficio.class, origemId);
// origem pode ser null!

// Depois: ✅
Beneficio origem = repository.findByIdWithLock(origemId)
    .orElseThrow(() -> new ResourceNotFoundException("Benefício", "id", origemId));
```

#### 4. Validação Origem != Destino

```java
if (dto.getOrigemId().equals(dto.getDestinoId())) {
    throw new BusinessException("Origem e destino não podem ser iguais");
}
```

#### 5. Ordenação de Locks (Prevenção de Deadlock)

```java
Long primeiroId = Math.min(dto.getOrigemId(), dto.getDestinoId());
Long segundoId = Math.max(dto.getOrigemId(), dto.getDestinoId());

Beneficio primeiro = repository.findByIdWithLock(primeiroId).orElseThrow(...);
Beneficio segundo = repository.findByIdWithLock(segundoId).orElseThrow(...);
```

#### 6. Isolamento Transacional SERIALIZABLE

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public TransferenciaResultadoDTO transferir(TransferenciaDTO dto) {
    // ...
}
```

### Comparação Antes/Depois

| Aspecto | Antes (EJB Bugado) | Depois (Spring Boot Corrigido) |
|---------|-------------------|--------------------------------|
| Validação de saldo | ❌ Não | ✅ Sim |
| Locking | ❌ Nenhum | ✅ Pessimistic + Optimistic |
| Validação de existência | ❌ Não | ✅ Sim, com exception |
| Origem != Destino | ❌ Não | ✅ Sim |
| Ordenação de locks | ❌ Não | ✅ Sim (menor ID primeiro) |
| Isolamento transacional | ❌ Padrão | ✅ SERIALIZABLE |
| Tratamento de erro | ❌ Genérico | ✅ Específico (InsufficientBalanceException, etc) |
| Logging | ❌ Não | ✅ Sim (SLF4J) |
| Testes | ❌ Não | ✅ Sim (Unit + Integration) |

---

## Explicações Técnicas Adicionais

### Por que Anemic Domain Model?

Embora o **Rich Domain Model** (DDD) seja teoricamente superior, optei por **Anemic Domain Model** pelos seguintes motivos práticos:

1. **Padrão de Mercado**: Mais comum em ambientes corporativos e bancários (80-90% dos casos)
2. **Familiaridade**: A maioria dos desenvolvedores está acostumada com este padrão
3. **Spring Boot Convention**: Alinha com práticas convencionais do framework
4. **Manutenibilidade**: Mais fácil para equipes grandes e com turnover
5. **Evita Discussões**: Foco na solução do problema, não no padrão arquitetural

**Trade-off aceito**: Menor encapsulamento em troca de maior adoção e facilidade de manutenção.

### Por que Pessimistic + Optimistic Lock?

**Defesa em Profundidade**:

- **Optimistic (@Version)**: Primeira linha de defesa
  - Melhor performance em cenários normais
  - Detecta conflitos após o fato
  - Requer reload + retry pelo cliente

- **Pessimistic (SELECT FOR UPDATE)**: Segunda linha de defesa
  - Previne conflitos desde o início
  - Garante consistência em operações críticas (transferências)
  - Maior custo de performance, mas aceitável para operações financeiras

### Por que Isolamento SERIALIZABLE?

**Consistência > Performance** em operações financeiras:

- Previne anomalias de leitura (phantom reads, non-repeatable reads, dirty reads)
- Garante que transferências sejam executadas como se fossem seriais
- Trade-off aceitável: menor concorrência vs. maior consistência

**Alternativa considerada**: `REPEATABLE_READ` (menor isolamento)
- Rejeitada porque ainda permite phantom reads em PostgreSQL
- Em operações financeiras, consistência absoluta é prioridade

### Ordenação de Locks: Por quê?

**Problema**: Deadlock em transferências concorrentes

**Cenário problemático**:
```
T1: transferir(A → B)  →  Lock(A), Lock(B)
T2: transferir(B → A)  →  Lock(B), Lock(A)

Deadlock! T1 espera B, T2 espera A
```

**Solução**: Sempre adquirir locks na mesma ordem
```
T1: transferir(A → B)  →  Lock(min(A,B)), Lock(max(A,B))  →  Lock(A), Lock(B)
T2: transferir(B → A)  →  Lock(min(B,A)), Lock(max(B,A))  →  Lock(A), Lock(B)

T1 adquire A primeiro, T2 espera. Quando T1 libera, T2 executa. Sem deadlock!
```

### MapStruct vs Mapeamento Manual?

**Por que MapStruct**:

1. **Performance**: Geração em compile-time (não reflection)
2. **Type Safety**: Erros em tempo de compilação
3. **Manutenibilidade**: Menos código para manter
4. **Consistência**: Regras de mapeamento centralizadas

**Comparação**:

```java
// Mapeamento Manual (verbose, propenso a erros)
public BeneficioDTO toDTO(Beneficio entity) {
    BeneficioDTO dto = new BeneficioDTO();
    dto.setId(entity.getId());
    dto.setNome(entity.getNome());
    dto.setDescricao(entity.getDescricao());
    dto.setValor(entity.getValor());
    dto.setAtivo(entity.getAtivo());
    dto.setVersion(entity.getVersion());
    dto.setCriadoEm(entity.getCriadoEm());
    dto.setAtualizadoEm(entity.getAtualizadoEm());
    return dto;
}

// MapStruct (conciso, gerado automaticamente)
@Mapper(componentModel = "spring")
public interface BeneficioMapper {
    BeneficioDTO toDTO(Beneficio entity);
}
```

### Global Exception Handler: Vantagens

**Centralização**:
```java
// Sem handler: código duplicado em cada controller
@PostMapping
public ResponseEntity<?> criar(@Valid BeneficioCreateDTO dto) {
    try {
        return ResponseEntity.ok(service.criar(dto));
    } catch (DuplicateResourceException e) {
        return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of("error", "Erro interno"));
    }
}

// Com handler: delegação automática
@PostMapping
public ResponseEntity<BeneficioDTO> criar(@Valid BeneficioCreateDTO dto) {
    return ResponseEntity.ok(service.criar(dto));
}

// Handler centralizado
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(...) {
        // Formato consistente, logging, auditoria
    }
}
```

**Benefícios**:
1. ✅ Formato de resposta consistente (ApiErrorResponse)
2. ✅ Logging centralizado
3. ✅ Código controller mais limpo
4. ✅ Fácil adicionar novos handlers
5. ✅ Melhor experiência do desenvolvedor

---

## Conclusão

Este projeto implementa uma solução robusta e completa para gerenciamento de benefícios, corrigindo todos os problemas identificados no código EJB original e adicionando funcionalidades modernas.

**Principais Destaques**:

- ✅ Arquitetura em camadas bem definida
- ✅ Controle de concorrência robusto (Pessimistic + Optimistic)
- ✅ Validações abrangentes (Bean Validation + Business Logic)
- ✅ API REST bem documentada (OpenAPI/Swagger)
- ✅ Frontend moderno (Angular 18)
- ✅ Testes automatizados (Unit + Integration)
- ✅ Tratamento de erros consistente
- ✅ Código limpo e bem documentado

**Decisões Técnicas Justificadas**:

- Anemic Domain Model: Padrão de mercado, facilita manutenção
- Duplo Locking: Defesa em profundidade
- Isolamento SERIALIZABLE: Consistência em operações financeiras
- Ordenação de Locks: Prevenção de deadlocks
- MapStruct: Performance e type safety
- Global Exception Handler: Centralização e consistência

**Diferenciais**:

- Correção completa do bug original (6 problemas identificados e resolvidos)
- Documentação técnica detalhada
- Código production-ready
- Padrões de mercado (Spring Boot, Angular, Docker)

---

**Desenvolvido como solução para o desafio técnico da BIP**

Data: Abril 2024
