import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BeneficioService } from '../../../../core/services/beneficio.service';
import { Beneficio, PageResponse } from '../../../../core/models/beneficio.model';

@Component({
  selector: 'app-beneficio-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, CurrencyPipe, DatePipe],
  template: `
    <div class="page-header">
      <div>
        <h2>Benefícios</h2>
        <p class="text-muted">Gerencie os benefícios cadastrados no sistema</p>
      </div>
      <a routerLink="/beneficios/novo" class="btn btn-primary">
        <span class="material-icons">add</span>
        Novo Benefício
      </a>
    </div>

    @if (mensagem()) {
      <div class="alert" [class.alert-success]="mensagemTipo() === 'success'" [class.alert-error]="mensagemTipo() === 'error'">
        {{ mensagem() }}
        <button class="close-btn" (click)="limparMensagem()">×</button>
      </div>
    }

    <div class="card">
      <div class="filters">
        <div class="search-box">
          <span class="material-icons">search</span>
          <input
            type="text"
            placeholder="Buscar por nome..."
            [(ngModel)]="termoBusca"
            (input)="buscar()"
          />
        </div>
        <label class="checkbox-label">
          <input
            type="checkbox"
            [(ngModel)]="apenasAtivos"
            (change)="carregar()"
          />
          Apenas ativos
        </label>
      </div>

      @if (carregando()) {
        <div class="loading">
          <span class="loading-spinner"></span>
          <span>Carregando...</span>
        </div>
      } @else if (beneficios().length === 0) {
        <div class="empty-state">
          <span class="material-icons">inbox</span>
          <p>Nenhum benefício encontrado</p>
        </div>
      } @else {
        <div class="table-container">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Nome</th>
                <th>Descrição</th>
                <th>Valor</th>
                <th>Status</th>
                <th>Atualizado em</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              @for (beneficio of beneficios(); track beneficio.id) {
                <tr>
                  <td>{{ beneficio.id }}</td>
                  <td><strong>{{ beneficio.nome }}</strong></td>
                  <td>{{ beneficio.descricao || '-' }}</td>
                  <td>{{ beneficio.valor | currency:'BRL' }}</td>
                  <td>
                    <span class="badge" [class.badge-success]="beneficio.ativo" [class.badge-danger]="!beneficio.ativo">
                      {{ beneficio.ativo ? 'Ativo' : 'Inativo' }}
                    </span>
                  </td>
                  <td>{{ beneficio.atualizadoEm | date:'dd/MM/yyyy HH:mm' }}</td>
                  <td class="actions">
                    <button class="btn btn-icon btn-secondary" title="Editar" [routerLink]="['/beneficios', beneficio.id, 'editar']">
                      <span class="material-icons">edit</span>
                    </button>
                    <button
                      class="btn btn-icon"
                      [class.btn-success]="!beneficio.ativo"
                      [class.btn-secondary]="beneficio.ativo"
                      [title]="beneficio.ativo ? 'Desativar' : 'Ativar'"
                      (click)="toggleStatus(beneficio)"
                    >
                      <span class="material-icons">{{ beneficio.ativo ? 'toggle_off' : 'toggle_on' }}</span>
                    </button>
                    <button class="btn btn-icon btn-danger" title="Excluir" (click)="confirmarExclusao(beneficio)">
                      <span class="material-icons">delete</span>
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>

        <div class="pagination">
          <button [disabled]="paginaAtual() === 0" (click)="irParaPagina(0)">
            <span class="material-icons">first_page</span>
          </button>
          <button [disabled]="paginaAtual() === 0" (click)="irParaPagina(paginaAtual() - 1)">
            <span class="material-icons">chevron_left</span>
          </button>
          <span class="page-info">
            Página {{ paginaAtual() + 1 }} de {{ totalPaginas() }}
            ({{ totalElementos() }} registros)
          </span>
          <button [disabled]="paginaAtual() >= totalPaginas() - 1" (click)="irParaPagina(paginaAtual() + 1)">
            <span class="material-icons">chevron_right</span>
          </button>
          <button [disabled]="paginaAtual() >= totalPaginas() - 1" (click)="irParaPagina(totalPaginas() - 1)">
            <span class="material-icons">last_page</span>
          </button>
        </div>
      }
    </div>

    @if (beneficioParaExcluir()) {
      <div class="modal-overlay" (click)="cancelarExclusao()">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h3>Confirmar Exclusão</h3>
            <button class="btn btn-icon" (click)="cancelarExclusao()">
              <span class="material-icons">close</span>
            </button>
          </div>
          <div class="modal-body">
            <p>Tem certeza que deseja excluir o benefício <strong>{{ beneficioParaExcluir()?.nome }}</strong>?</p>
            <p class="text-muted">Esta ação não pode ser desfeita.</p>
          </div>
          <div class="modal-footer">
            <button class="btn btn-secondary" (click)="cancelarExclusao()">Cancelar</button>
            <button class="btn btn-danger" (click)="excluir()">Excluir</button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: var(--spacing-lg);

      h2 {
        margin-bottom: var(--spacing-xs);
      }
    }

    .filters {
      display: flex;
      gap: var(--spacing-md);
      align-items: center;
      margin-bottom: var(--spacing-md);
      flex-wrap: wrap;
    }

    .search-box {
      display: flex;
      align-items: center;
      gap: var(--spacing-sm);
      flex: 1;
      min-width: 250px;
      max-width: 400px;
      background: var(--bg-secondary);
      border-radius: var(--border-radius);
      padding: var(--spacing-sm) var(--spacing-md);

      input {
        flex: 1;
        border: none;
        background: transparent;
        outline: none;
        font-size: 0.875rem;
      }

      .material-icons {
        color: var(--text-secondary);
      }
    }

    .checkbox-label {
      display: flex;
      align-items: center;
      gap: var(--spacing-xs);
      cursor: pointer;
      font-size: 0.875rem;

      input {
        width: 16px;
        height: 16px;
        cursor: pointer;
      }
    }

    .loading, .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: var(--spacing-xl);
      color: var(--text-secondary);

      .material-icons {
        font-size: 48px;
        margin-bottom: var(--spacing-md);
      }
    }

    .page-info {
      font-size: 0.875rem;
      color: var(--text-secondary);
    }

    .alert {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .close-btn {
        background: none;
        border: none;
        font-size: 1.25rem;
        cursor: pointer;
        opacity: 0.7;

        &:hover {
          opacity: 1;
        }
      }
    }
  `]
})
export class BeneficioListComponent implements OnInit {
  private readonly service = inject(BeneficioService);
  private readonly router = inject(Router);

  beneficios = signal<Beneficio[]>([]);
  carregando = signal(false);
  paginaAtual = signal(0);
  totalPaginas = signal(0);
  totalElementos = signal(0);

  mensagem = signal('');
  mensagemTipo = signal<'success' | 'error'>('success');

  beneficioParaExcluir = signal<Beneficio | null>(null);

  termoBusca = '';
  apenasAtivos = false;

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.service.listar(this.paginaAtual(), 10, this.apenasAtivos).subscribe({
      next: (response) => {
        this.beneficios.set(response.content);
        this.totalPaginas.set(response.totalPages);
        this.totalElementos.set(response.totalElements);
        this.carregando.set(false);
      },
      error: (err) => {
        this.mostrarMensagem(err.userMessage || 'Erro ao carregar benefícios', 'error');
        this.carregando.set(false);
      }
    });
  }

  buscar(): void {
    if (this.termoBusca.length > 2) {
      this.service.buscarPorNome(this.termoBusca).subscribe({
        next: (response) => {
          this.beneficios.set(response.content);
          this.totalPaginas.set(response.totalPages);
          this.totalElementos.set(response.totalElements);
        }
      });
    } else if (this.termoBusca.length === 0) {
      this.carregar();
    }
  }

  irParaPagina(pagina: number): void {
    this.paginaAtual.set(pagina);
    this.carregar();
  }

  toggleStatus(beneficio: Beneficio): void {
    const acao = beneficio.ativo ? this.service.desativar(beneficio.id) : this.service.ativar(beneficio.id);
    acao.subscribe({
      next: () => {
        this.mostrarMensagem(
          `Benefício ${beneficio.ativo ? 'desativado' : 'ativado'} com sucesso`,
          'success'
        );
        this.carregar();
      },
      error: (err) => {
        this.mostrarMensagem(err.userMessage || 'Erro ao alterar status', 'error');
      }
    });
  }

  confirmarExclusao(beneficio: Beneficio): void {
    this.beneficioParaExcluir.set(beneficio);
  }

  cancelarExclusao(): void {
    this.beneficioParaExcluir.set(null);
  }

  excluir(): void {
    const beneficio = this.beneficioParaExcluir();
    if (!beneficio) return;

    this.service.remover(beneficio.id).subscribe({
      next: () => {
        this.mostrarMensagem('Benefício excluído com sucesso', 'success');
        this.cancelarExclusao();
        this.carregar();
      },
      error: (err) => {
        this.mostrarMensagem(err.userMessage || 'Erro ao excluir benefício', 'error');
        this.cancelarExclusao();
      }
    });
  }

  mostrarMensagem(texto: string, tipo: 'success' | 'error'): void {
    this.mensagem.set(texto);
    this.mensagemTipo.set(tipo);
    setTimeout(() => this.limparMensagem(), 5000);
  }

  limparMensagem(): void {
    this.mensagem.set('');
  }
}
