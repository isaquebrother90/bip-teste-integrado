import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ToastComponent } from './core/components/toast/toast.component';
import { LoadingService } from './core/services/loading.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, ToastComponent],
  template: `
    <!-- Barra de progresso global — aparece durante qualquer requisição HTTP -->
    @if (loadingService.isLoading()) {
      <div class="global-loading-bar"></div>
    }

    <app-toast />

    <div class="app-container">
      <header class="header">
        <div class="header-content">
          <h1 class="logo">
            <span class="material-icons">account_balance_wallet</span>
            Benefícios
          </h1>
          <nav class="nav">
            <a routerLink="/beneficios" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}">
              <span class="material-icons">list</span>
              Listar
            </a>
            <a routerLink="/beneficios/novo" routerLinkActive="active">
              <span class="material-icons">add_circle</span>
              Novo
            </a>
            <a routerLink="/transferencia" routerLinkActive="active">
              <span class="material-icons">swap_horiz</span>
              Transferir
            </a>
          </nav>
        </div>
      </header>

      <main class="main-content">
        <router-outlet />
      </main>

      <footer class="footer">
        <p>&copy; 2024 - Sistema de Gerenciamento de Benefícios | Desafio BIP</p>
      </footer>
    </div>
  `,
  styles: [`
    .global-loading-bar {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 3px;
      background: linear-gradient(90deg, var(--primary-color), #60a5fa, var(--primary-color));
      background-size: 200% 100%;
      animation: loadingBar 1.2s linear infinite;
      z-index: 9999;
    }

    @keyframes loadingBar {
      0%   { background-position: 200% 0; }
      100% { background-position: -200% 0; }
    }

    .app-container {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    }

    .header {
      background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-dark) 100%);
      color: white;
      box-shadow: var(--shadow-md);
      position: sticky;
      top: 0;
      z-index: 100;
    }

    .header-content {
      max-width: 1200px;
      margin: 0 auto;
      padding: var(--spacing-md) var(--spacing-lg);
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .logo {
      display: flex;
      align-items: center;
      gap: var(--spacing-sm);
      font-size: 1.5rem;
      margin: 0;

      .material-icons {
        font-size: 2rem;
      }
    }

    .nav {
      display: flex;
      gap: var(--spacing-md);

      a {
        display: flex;
        align-items: center;
        gap: var(--spacing-xs);
        color: rgba(255, 255, 255, 0.8);
        text-decoration: none;
        padding: var(--spacing-sm) var(--spacing-md);
        border-radius: var(--border-radius);
        transition: all 0.2s;

        &:hover {
          background-color: rgba(255, 255, 255, 0.1);
          color: white;
        }

        &.active {
          background-color: rgba(255, 255, 255, 0.2);
          color: white;
        }

        .material-icons {
          font-size: 1.25rem;
        }
      }
    }

    .main-content {
      flex: 1;
      max-width: 1200px;
      margin: 0 auto;
      padding: var(--spacing-lg);
      width: 100%;
    }

    .footer {
      background-color: var(--text-primary);
      color: var(--text-disabled);
      text-align: center;
      padding: var(--spacing-md);

      p {
        margin: 0;
        font-size: 0.875rem;
      }
    }

    @media (max-width: 768px) {
      .header-content {
        flex-direction: column;
        gap: var(--spacing-md);
      }

      .nav {
        flex-wrap: wrap;
        justify-content: center;
      }
    }
  `]
})
export class AppComponent {
  title = 'Benefícios';
  readonly loadingService = inject(LoadingService);
}
