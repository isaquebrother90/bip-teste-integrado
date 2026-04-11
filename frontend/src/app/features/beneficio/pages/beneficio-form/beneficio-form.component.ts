import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { BeneficioService } from '../../../../core/services/beneficio.service';
import { Beneficio } from '../../../../core/models/beneficio.model';

@Component({
  selector: 'app-beneficio-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="page-header">
      <div>
        <h2>{{ isEdicao() ? 'Editar' : 'Novo' }} Benefício</h2>
        <p class="text-muted">
          {{ isEdicao() ? 'Atualize os dados do benefício' : 'Preencha os dados para criar um novo benefício' }}
        </p>
      </div>
    </div>

    @if (erro()) {
      <div class="alert alert-error">
        {{ erro() }}
      </div>
    }

    <div class="card">
      @if (carregando()) {
        <div class="loading">
          <span class="loading-spinner"></span>
          <span>Carregando...</span>
        </div>
      } @else {
        <form [formGroup]="form" (ngSubmit)="salvar()">
          <div class="form-row">
            <div class="form-group flex-2">
              <label for="nome">Nome *</label>
              <input
                id="nome"
                type="text"
                formControlName="nome"
                [class.invalid]="isInvalid('nome')"
                placeholder="Digite o nome do benefício"
              />
              @if (isInvalid('nome')) {
                <span class="error-message">
                  @if (form.get('nome')?.errors?.['required']) {
                    Nome é obrigatório
                  } @else if (form.get('nome')?.errors?.['minlength']) {
                    Nome deve ter pelo menos 2 caracteres
                  } @else if (form.get('nome')?.errors?.['maxlength']) {
                    Nome deve ter no máximo 100 caracteres
                  }
                </span>
              }
            </div>

            <div class="form-group flex-1">
              <label for="valor">Valor *</label>
              <input
                id="valor"
                type="number"
                step="0.01"
                min="0"
                formControlName="valor"
                [class.invalid]="isInvalid('valor')"
                placeholder="0.00"
              />
              @if (isInvalid('valor')) {
                <span class="error-message">
                  @if (form.get('valor')?.errors?.['required']) {
                    Valor é obrigatório
                  } @else if (form.get('valor')?.errors?.['min']) {
                    Valor não pode ser negativo
                  }
                </span>
              }
            </div>
          </div>

          <div class="form-group">
            <label for="descricao">Descrição</label>
            <textarea
              id="descricao"
              formControlName="descricao"
              rows="3"
              [class.invalid]="isInvalid('descricao')"
              placeholder="Descrição opcional do benefício"
            ></textarea>
            @if (isInvalid('descricao')) {
              <span class="error-message">Descrição deve ter no máximo 255 caracteres</span>
            }
          </div>

          @if (isEdicao()) {
            <div class="form-group">
              <label class="checkbox-label">
                <input type="checkbox" formControlName="ativo" />
                <span>Benefício ativo</span>
              </label>
            </div>
          }

          <div class="form-actions">
            <a routerLink="/beneficios" class="btn btn-secondary">
              <span class="material-icons">arrow_back</span>
              Cancelar
            </a>
            <button type="submit" class="btn btn-primary" [disabled]="form.invalid || salvando()">
              @if (salvando()) {
                <span class="loading-spinner"></span>
                Salvando...
              } @else {
                <span class="material-icons">save</span>
                {{ isEdicao() ? 'Atualizar' : 'Criar' }}
              }
            </button>
          </div>
        </form>
      }
    </div>
  `,
  styles: [`
    .page-header {
      margin-bottom: var(--spacing-lg);

      h2 {
        margin-bottom: var(--spacing-xs);
      }
    }

    .form-row {
      display: flex;
      gap: var(--spacing-md);

      @media (max-width: 768px) {
        flex-direction: column;
      }
    }

    .flex-1 {
      flex: 1;
    }

    .flex-2 {
      flex: 2;
    }

    textarea {
      resize: vertical;
      min-height: 80px;
    }

    .checkbox-label {
      display: flex;
      align-items: center;
      gap: var(--spacing-sm);
      cursor: pointer;

      input {
        width: 18px;
        height: 18px;
        cursor: pointer;
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

    .loading {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: var(--spacing-sm);
      padding: var(--spacing-xl);
    }
  `]
})
export class BeneficioFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(BeneficioService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  form!: FormGroup;
  beneficioId: number | null = null;

  isEdicao = signal(false);
  carregando = signal(false);
  salvando = signal(false);
  erro = signal('');

  ngOnInit(): void {
    this.initForm();

    const id = this.route.snapshot.params['id'];
    if (id) {
      this.beneficioId = +id;
      this.isEdicao.set(true);
      this.carregarBeneficio();
    }
  }

  private initForm(): void {
    this.form = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      descricao: ['', [Validators.maxLength(255)]],
      valor: [0, [Validators.required, Validators.min(0)]],
      ativo: [true],
      version: [0]
    });
  }

  private carregarBeneficio(): void {
    if (!this.beneficioId) return;

    this.carregando.set(true);
    this.service.buscarPorId(this.beneficioId).subscribe({
      next: (beneficio) => {
        this.form.patchValue({
          nome: beneficio.nome,
          descricao: beneficio.descricao,
          valor: beneficio.valor,
          ativo: beneficio.ativo,
          version: beneficio.version
        });
        this.carregando.set(false);
      },
      error: (err) => {
        this.erro.set(err.userMessage || 'Erro ao carregar benefício');
        this.carregando.set(false);
      }
    });
  }

  isInvalid(field: string): boolean {
    const control = this.form.get(field);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.salvando.set(true);
    this.erro.set('');

    const dados = this.form.value;

    const operacao = this.isEdicao()
      ? this.service.atualizar(this.beneficioId!, dados)
      : this.service.criar(dados);

    operacao.subscribe({
      next: () => {
        this.router.navigate(['/beneficios'], {
          state: { message: `Benefício ${this.isEdicao() ? 'atualizado' : 'criado'} com sucesso` }
        });
      },
      error: (err) => {
        this.erro.set(err.userMessage || 'Erro ao salvar benefício');
        this.salvando.set(false);
      }
    });
  }
}
