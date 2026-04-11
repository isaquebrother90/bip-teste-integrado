import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BeneficioService } from '../../../../core/services/beneficio.service';
import { Beneficio, TransferenciaResultado } from '../../../../core/models/beneficio.model';

@Component({
  selector: 'app-transferencia',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, CurrencyPipe],
  template: `
    <div class="page-header">
      <div>
        <h2>Transferência de Valores</h2>
        <p class="text-muted">Transfira valores entre benefícios de forma segura</p>
      </div>
    </div>

    <div class="transfer-container">
      <div class="card">
        <div class="card-header">
          <h3>Nova Transferência</h3>
        </div>

        @if (erro()) {
          <div class="alert alert-error">
            {{ erro() }}
          </div>
        }

        @if (resultado()) {
          <div class="alert" [class.alert-success]="resultado()?.sucesso" [class.alert-error]="!resultado()?.sucesso">
            <div class="result-content">
              <span class="material-icons">{{ resultado()?.sucesso ? 'check_circle' : 'error' }}</span>
              <div>
                <strong>{{ resultado()?.mensagem }}</strong>
                @if (resultado()?.sucesso) {
                  <p>Valor transferido: {{ resultado()?.valorTransferido | currency:'BRL' }}</p>
                  <p>Saldo origem: {{ resultado()?.saldoOrigem | currency:'BRL' }}</p>
                  <p>Saldo destino: {{ resultado()?.saldoDestino | currency:'BRL' }}</p>
                }
              </div>
            </div>
          </div>
        }

        @if (carregandoBeneficios()) {
          <div class="loading">
            <span class="loading-spinner"></span>
            <span>Carregando benefícios...</span>
          </div>
        } @else {
          <form [formGroup]="form" (ngSubmit)="transferir()">
            <div class="transfer-fields">
              <div class="form-group">
                <label for="origemId">Origem (Débito) *</label>
                <select
                  id="origemId"
                  formControlName="origemId"
                  [class.invalid]="isInvalid('origemId')"
                  (change)="atualizarSaldoOrigem()"
                >
                  <option value="">Selecione o benefício de origem</option>
                  @for (beneficio of beneficiosAtivos(); track beneficio.id) {
                    <option [value]="beneficio.id">
                      {{ beneficio.nome }} - {{ beneficio.valor | currency:'BRL' }}
                    </option>
                  }
                </select>
                @if (isInvalid('origemId')) {
                  <span class="error-message">Selecione o benefício de origem</span>
                }
                @if (saldoOrigem() !== null) {
                  <span class="saldo-info">Saldo disponível: {{ saldoOrigem() | currency:'BRL' }}</span>
                }
              </div>

              <div class="transfer-arrow">
                <span class="material-icons">arrow_forward</span>
              </div>

              <div class="form-group">
                <label for="destinoId">Destino (Crédito) *</label>
                <select
                  id="destinoId"
                  formControlName="destinoId"
                  [class.invalid]="isInvalid('destinoId')"
                >
                  <option value="">Selecione o benefício de destino</option>
                  @for (beneficio of beneficiosAtivos(); track beneficio.id) {
                    <option
                      [value]="beneficio.id"
                      [disabled]="beneficio.id === form.get('origemId')?.value"
                    >
                      {{ beneficio.nome }} - {{ beneficio.valor | currency:'BRL' }}
                    </option>
                  }
                </select>
                @if (isInvalid('destinoId')) {
                  <span class="error-message">Selecione o benefício de destino</span>
                }
              </div>
            </div>

            <div class="form-group valor-group">
              <label for="valor">Valor da Transferência *</label>
              <div class="valor-input">
                <span class="currency-prefix">R$</span>
                <input
                  id="valor"
                  type="number"
                  step="0.01"
                  min="0.01"
                  formControlName="valor"
                  [class.invalid]="isInvalid('valor')"
                  placeholder="0,00"
                />
              </div>
              @if (isInvalid('valor')) {
                <span class="error-message">
                  @if (form.get('valor')?.errors?.['required']) {
                    Valor é obrigatório
                  } @else if (form.get('valor')?.errors?.['min']) {
                    Valor deve ser maior que zero
                  }
                </span>
              }
            </div>

            <div class="form-actions">
              <a routerLink="/beneficios" class="btn btn-secondary">
                <span class="material-icons">arrow_back</span>
                Voltar
              </a>
              <button
                type="submit"
                class="btn btn-primary"
                [disabled]="form.invalid || processando()"
              >
                @if (processando()) {
                  <span class="loading-spinner"></span>
                  Processando...
                } @else {
                  <span class="material-icons">swap_horiz</span>
                  Transferir
                }
              </button>
            </div>
          </form>
        }
      </div>

      <div class="card info-card">
        <div class="card-header">
          <h3>Informações</h3>
        </div>
        <div class="info-content">
          <div class="info-item">
            <span class="material-icons">security</span>
            <div>
              <strong>Transferência Segura</strong>
              <p>Todas as transferências são atômicas e protegidas contra inconsistências.</p>
            </div>
          </div>
          <div class="info-item">
            <span class="material-icons">account_balance</span>
            <div>
              <strong>Validação de Saldo</strong>
              <p>O sistema verifica automaticamente se há saldo suficiente.</p>
            </div>
          </div>
          <div class="info-item">
            <span class="material-icons">sync_lock</span>
            <div>
              <strong>Controle de Concorrência</strong>
              <p>Mecanismos de locking garantem integridade dos dados.</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-header {
      margin-bottom: var(--spacing-lg);

      h2 {
        margin-bottom: var(--spacing-xs);
      }
    }

    .transfer-container {
      display: grid;
      grid-template-columns: 2fr 1fr;
      gap: var(--spacing-lg);

      @media (max-width: 1024px) {
        grid-template-columns: 1fr;
      }
    }

    .transfer-fields {
      display: flex;
      align-items: flex-start;
      gap: var(--spacing-md);

      @media (max-width: 768px) {
        flex-direction: column;

        .transfer-arrow {
          transform: rotate(90deg);
        }
      }

      .form-group {
        flex: 1;
      }
    }

    .transfer-arrow {
      display: flex;
      align-items: center;
      justify-content: center;
      padding-top: 28px;

      .material-icons {
        font-size: 32px;
        color: var(--primary-color);
      }
    }

    .saldo-info {
      display: block;
      margin-top: var(--spacing-xs);
      font-size: 0.75rem;
      color: var(--success-color);
    }

    .valor-group {
      max-width: 300px;
    }

    .valor-input {
      display: flex;
      align-items: center;
      border: 1px solid var(--border-color);
      border-radius: var(--border-radius);
      overflow: hidden;

      &:focus-within {
        border-color: var(--primary-color);
        box-shadow: 0 0 0 3px rgba(25, 118, 210, 0.1);
      }

      .currency-prefix {
        padding: var(--spacing-sm) var(--spacing-md);
        background-color: var(--bg-secondary);
        color: var(--text-secondary);
        font-weight: 500;
      }

      input {
        border: none;
        border-radius: 0;
        flex: 1;

        &:focus {
          box-shadow: none;
        }
      }
    }

    .form-actions {
      display: flex;
      justify-content: flex-end;
      gap: var(--spacing-md);
      margin-top: var(--spacing-lg);
      padding-top: var(--spacing-lg);
      border-top: 1px solid var(--border-color);
    }

    .info-card {
      .info-content {
        display: flex;
        flex-direction: column;
        gap: var(--spacing-md);
      }

      .info-item {
        display: flex;
        gap: var(--spacing-md);
        padding: var(--spacing-md);
        background-color: var(--bg-secondary);
        border-radius: var(--border-radius);

        .material-icons {
          font-size: 24px;
          color: var(--primary-color);
        }

        strong {
          display: block;
          margin-bottom: var(--spacing-xs);
        }

        p {
          margin: 0;
          font-size: 0.875rem;
          color: var(--text-secondary);
        }
      }
    }

    .result-content {
      display: flex;
      gap: var(--spacing-md);
      align-items: flex-start;

      .material-icons {
        font-size: 24px;
      }

      p {
        margin: var(--spacing-xs) 0 0;
        font-size: 0.875rem;
      }
    }

    .loading {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: var(--spacing-sm);
      padding: var(--spacing-xl);
    }
  `]
})
export class TransferenciaComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(BeneficioService);

  form!: FormGroup;

  beneficiosAtivos = signal<Beneficio[]>([]);
  carregandoBeneficios = signal(false);
  processando = signal(false);
  erro = signal('');
  resultado = signal<TransferenciaResultado | null>(null);
  saldoOrigem = signal<number | null>(null);

  ngOnInit(): void {
    this.initForm();
    this.carregarBeneficios();
  }

  private initForm(): void {
    this.form = this.fb.group({
      origemId: ['', [Validators.required]],
      destinoId: ['', [Validators.required]],
      valor: [null, [Validators.required, Validators.min(0.01)]]
    });
  }

  private carregarBeneficios(): void {
    this.carregandoBeneficios.set(true);
    this.service.listar(0, 100, true).subscribe({
      next: (response) => {
        this.beneficiosAtivos.set(response.content);
        this.carregandoBeneficios.set(false);
      },
      error: (err) => {
        this.erro.set(err.userMessage || 'Erro ao carregar benefícios');
        this.carregandoBeneficios.set(false);
      }
    });
  }

  atualizarSaldoOrigem(): void {
    const origemId = this.form.get('origemId')?.value;
    if (origemId) {
      const beneficio = this.beneficiosAtivos().find(b => b.id === +origemId);
      this.saldoOrigem.set(beneficio?.valor ?? null);
    } else {
      this.saldoOrigem.set(null);
    }
  }

  isInvalid(field: string): boolean {
    const control = this.form.get(field);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  transferir(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.processando.set(true);
    this.erro.set('');
    this.resultado.set(null);

    const dados = {
      origemId: +this.form.value.origemId,
      destinoId: +this.form.value.destinoId,
      valor: +this.form.value.valor
    };

    this.service.transferir(dados).subscribe({
      next: (res) => {
        this.resultado.set(res);
        this.processando.set(false);
        this.form.reset();
        this.saldoOrigem.set(null);
        this.carregarBeneficios();
      },
      error: (err) => {
        this.erro.set(err.userMessage || err.message || 'Erro ao realizar transferência');
        this.processando.set(false);
      }
    });
  }
}
