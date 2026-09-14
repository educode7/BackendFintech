import { Component, input } from '@angular/core';

@Component({
  standalone: true,
  selector: 'app-loading-spinner',
  template: `
    <div class="spinner-container" [class.inline]="inline()">
      <div class="spinner"></div>
      @if (message()) {
        <p class="spinner-message">{{ message() }}</p>
      }
    </div>
  `,
  styles: [`
    .spinner-container { display: flex; flex-direction: column; align-items: center; padding: 2rem; }
    .spinner-container.inline { padding: 0.5rem; }
    .spinner {
      width: 40px; height: 40px;
      border: 3px solid #e0e0e0; border-top-color: #3b82f6;
      border-radius: 50%; animation: spin 0.8s linear infinite;
    }
    .spinner-message { margin-top: 1rem; color: #666; font-size: 0.875rem; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class LoadingSpinnerComponent {
  message = input<string>();
  inline = input(false);
}
