import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Beneficio,
  BeneficioCreate,
  BeneficioUpdate,
  PageResponse,
  Transferencia,
  TransferenciaResultado
} from '../models/beneficio.model';
import { environment } from '../../../environments/environment';

// Service para consumir a API de benefícios
@Injectable({
  providedIn: 'root'
})
export class BeneficioService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/v1/beneficios`;

  listar(
    page: number = 0,
    size: number = 10,
    apenasAtivos: boolean = false
  ): Observable<PageResponse<Beneficio>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('apenasAtivos', apenasAtivos.toString());

    return this.http.get<PageResponse<Beneficio>>(this.baseUrl, { params });
  }

  buscarPorId(id: number): Observable<Beneficio> {
    return this.http.get<Beneficio>(`${this.baseUrl}/${id}`);
  }

  buscarPorNome(nome: string, page: number = 0, size: number = 10): Observable<PageResponse<Beneficio>> {
    const params = new HttpParams()
      .set('nome', nome)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageResponse<Beneficio>>(`${this.baseUrl}/buscar`, { params });
  }

  criar(beneficio: BeneficioCreate): Observable<Beneficio> {
    return this.http.post<Beneficio>(this.baseUrl, beneficio);
  }

  atualizar(id: number, beneficio: BeneficioUpdate): Observable<Beneficio> {
    return this.http.put<Beneficio>(`${this.baseUrl}/${id}`, beneficio);
  }

  remover(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  ativar(id: number): Observable<Beneficio> {
    return this.http.patch<Beneficio>(`${this.baseUrl}/${id}/ativar`, {});
  }

  desativar(id: number): Observable<Beneficio> {
    return this.http.patch<Beneficio>(`${this.baseUrl}/${id}/desativar`, {});
  }

  transferir(transferencia: Transferencia): Observable<TransferenciaResultado> {
    return this.http.post<TransferenciaResultado>(`${this.baseUrl}/transferir`, transferencia);
  }
}
