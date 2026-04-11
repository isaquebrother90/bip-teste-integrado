import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container">
      @for (toast of toastService.toasts(); track toast.id) {
        <div class="toast" [class]="'toast-' + toast.tipo" (click)="toastService.remover(toast.id)">
          <span class="material-icons toast-icon">
            {{ iconePorTipo(toast.tipo) }}
          </span>
          <span class="toast-mensagem">{{ toast.mensagem }}</span>
          <button class="toast-close">×</button>
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      bottom: 24px;
      right: 24px;
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-width: 380px;
    }

    .toast {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 12px 16px;
      border-radius: 8px;
      color: white;
      font-size: 0.875rem;
      cursor: pointer;
      animation: slideIn 0.3s ease-out;
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);

      &-success { background: #16a34a; }
      &-error   { background: #dc2626; }
      &-warning { background: #d97706; }
      &-info    { background: #2563eb; }
    }

    .toast-mensagem { flex: 1; }
    .toast-icon { font-size: 18px; }
    .toast-close {
      background: none;
      border: none;
      color: white;
      font-size: 16px;
      cursor: pointer;
      opacity: 0.8;
      &:hover { opacity: 1; }
    }

    @keyframes slideIn {
      from { transform: translateX(100%); opacity: 0; }
      to   { transform: translateX(0);   opacity: 1; }
    }
  `]
})
export class ToastComponent {
  readonly toastService = inject(ToastService);

  iconePorTipo(tipo: string): string {
    const icones: Record<string, string> = {
      success: 'check_circle',
      error: 'error',
      warning: 'warning',
      info: 'info'
    };
    return icones[tipo] ?? 'info';
  }
}
