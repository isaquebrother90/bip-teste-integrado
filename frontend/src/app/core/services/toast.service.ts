import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  mensagem: string;
  tipo: 'success' | 'error' | 'warning' | 'info';
  duracao: number;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toasts = signal<Toast[]>([]);
  private nextId = 0;

  success(mensagem: string, duracao = 4000): void {
    this.adicionar(mensagem, 'success', duracao);
  }

  error(mensagem: string, duracao = 6000): void {
    this.adicionar(mensagem, 'error', duracao);
  }

  warning(mensagem: string, duracao = 5000): void {
    this.adicionar(mensagem, 'warning', duracao);
  }

  info(mensagem: string, duracao = 4000): void {
    this.adicionar(mensagem, 'info', duracao);
  }

  remover(id: number): void {
    this.toasts.update(lista => lista.filter(t => t.id !== id));
  }

  private adicionar(mensagem: string, tipo: Toast['tipo'], duracao: number): void {
    const id = this.nextId++;
    this.toasts.update(lista => [...lista, { id, mensagem, tipo, duracao }]);
    setTimeout(() => this.remover(id), duracao);
  }
}
