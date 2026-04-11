// Interface do benefício
export interface Beneficio {
  id: number;
  nome: string;
  descricao?: string;
  valor: number;
  ativo: boolean;
  version: number;
  criadoEm?: string;
  atualizadoEm?: string;
}

// DTO para criar benefício
export interface BeneficioCreate {
  nome: string;
  descricao?: string;
  valor: number;
  ativo?: boolean;
}

// DTO para atualizar benefício
export interface BeneficioUpdate {
  nome: string;
  descricao?: string;
  valor: number;
  ativo: boolean;
  version: number;
}

// DTO para transferência entre benefícios
export interface Transferencia {
  origemId: number;
  destinoId: number;
  valor: number;
}

// Resultado da transferência
export interface TransferenciaResultado {
  sucesso: boolean;
  mensagem: string;
  valorTransferido: number;
  saldoOrigem: number;
  saldoDestino: number;
  dataTransferencia: string;
}

// Resposta paginada do Spring
export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      empty: boolean;
      unsorted: boolean;
    };
  };
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

// Resposta de erro da API
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  errorCode: string;
  message: string;
  path: string;
  fieldErrors?: FieldError[];
}

export interface FieldError {
  field: string;
  rejectedValue: unknown;
  message: string;
}
