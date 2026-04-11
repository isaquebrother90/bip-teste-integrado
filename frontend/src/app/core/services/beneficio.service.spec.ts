import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { BeneficioService } from './beneficio.service';
import { Beneficio, PageResponse, TransferenciaResultado } from '../models/beneficio.model';
import { environment } from '../../../environments/environment';

describe('BeneficioService', () => {
  let service: BeneficioService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/v1/beneficios`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [BeneficioService]
    });

    service = TestBed.inject(BeneficioService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('listar', () => {
    it('should return paginated beneficios', () => {
      const mockResponse: PageResponse<Beneficio> = {
        content: [
          { id: 1, nome: 'Vale Alimentação', valor: 800, ativo: true, version: 0 }
        ],
        pageable: { pageNumber: 0, pageSize: 10, sort: { sorted: false, empty: true, unsorted: true } },
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0,
        first: true,
        last: true,
        empty: false
      };

      service.listar(0, 10, false).subscribe(response => {
        expect(response.content.length).toBe(1);
        expect(response.content[0].nome).toBe('Vale Alimentação');
      });

      const req = httpMock.expectOne(`${baseUrl}?page=0&size=10&apenasAtivos=false`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('buscarPorId', () => {
    it('should return a beneficio by id', () => {
      const mockBeneficio: Beneficio = {
        id: 1,
        nome: 'Vale Alimentação',
        valor: 800,
        ativo: true,
        version: 0
      };

      service.buscarPorId(1).subscribe(beneficio => {
        expect(beneficio.id).toBe(1);
        expect(beneficio.nome).toBe('Vale Alimentação');
      });

      const req = httpMock.expectOne(`${baseUrl}/1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockBeneficio);
    });
  });

  describe('criar', () => {
    it('should create a new beneficio', () => {
      const newBeneficio = { nome: 'Novo Benefício', valor: 500, ativo: true };
      const mockResponse: Beneficio = { id: 2, ...newBeneficio, version: 0 };

      service.criar(newBeneficio).subscribe(beneficio => {
        expect(beneficio.id).toBe(2);
        expect(beneficio.nome).toBe('Novo Benefício');
      });

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(newBeneficio);
      req.flush(mockResponse);
    });
  });

  describe('transferir', () => {
    it('should transfer value between beneficios', () => {
      const transferencia = { origemId: 1, destinoId: 2, valor: 100 };
      const mockResponse: TransferenciaResultado = {
        sucesso: true,
        mensagem: 'Transferência realizada com sucesso',
        valorTransferido: 100,
        saldoOrigem: 700,
        saldoDestino: 600,
        dataTransferencia: '2026-04-11T10:00:00'
      };

      service.transferir(transferencia).subscribe(result => {
        expect(result.sucesso).toBe(true);
        expect(result.saldoOrigem).toBe(700);
        expect(result.saldoDestino).toBe(600);
      });

      const req = httpMock.expectOne(`${baseUrl}/transferir`);
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('ativar/desativar', () => {
    it('should activate a beneficio', () => {
      const mockResponse: Beneficio = { id: 1, nome: 'Test', valor: 100, ativo: true, version: 1 };

      service.ativar(1).subscribe(beneficio => {
        expect(beneficio.ativo).toBe(true);
      });

      const req = httpMock.expectOne(`${baseUrl}/1/ativar`);
      expect(req.request.method).toBe('PATCH');
      req.flush(mockResponse);
    });

    it('should deactivate a beneficio', () => {
      const mockResponse: Beneficio = { id: 1, nome: 'Test', valor: 100, ativo: false, version: 1 };

      service.desativar(1).subscribe(beneficio => {
        expect(beneficio.ativo).toBe(false);
      });

      const req = httpMock.expectOne(`${baseUrl}/1/desativar`);
      expect(req.request.method).toBe('PATCH');
      req.flush(mockResponse);
    });
  });
});
