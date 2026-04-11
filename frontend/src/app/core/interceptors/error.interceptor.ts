import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

// Interceptor para tratar erros HTTP globalmente
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'Ocorreu um erro inesperado.';

      if (error.error instanceof ErrorEvent) {
        // Erro do lado do cliente
        errorMessage = `Erro: ${error.error.message}`;
      } else {
        // Erro do lado do servidor
        if (error.error?.message) {
          errorMessage = error.error.message;
        } else {
          switch (error.status) {
            case 0:
              errorMessage = 'Não foi possível conectar ao servidor. Verifique sua conexão.';
              break;
            case 400:
              errorMessage = 'Requisição inválida. Verifique os dados informados.';
              break;
            case 401:
              errorMessage = 'Não autorizado. Faça login novamente.';
              break;
            case 403:
              errorMessage = 'Acesso negado. Você não tem permissão para esta ação.';
              break;
            case 404:
              errorMessage = 'Recurso não encontrado.';
              break;
            case 409:
              errorMessage = 'Conflito de dados. O registro pode ter sido modificado.';
              break;
            case 422:
              errorMessage = 'Não foi possível processar a requisição.';
              break;
            case 500:
              errorMessage = 'Erro interno do servidor. Tente novamente mais tarde.';
              break;
            default:
              errorMessage = `Erro ${error.status}: ${error.statusText}`;
          }
        }
      }

      console.error('HTTP Error:', error);

      return throwError(() => ({
        ...error.error,
        userMessage: errorMessage
      }));
    })
  );
};
