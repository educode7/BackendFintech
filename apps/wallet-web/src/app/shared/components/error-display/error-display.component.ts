import { Component, input } from '@angular/core';
import type { ProblemDetail } from '@models/error.model';

@Component({
  standalone: true,
  selector: 'app-error-display',
  template: `
    <div class="error-card" [class.warning]="statusClass() === 'warning'">
      <div class="error-icon">⚠️</div>
      <div class="error-content">
        <h3>{{ title() }}</h3>
        <p class="error-detail">{{ detail() }}</p>
        @if (correlationId()) {
          <p class="error-meta">Correlation ID: {{ correlationId() }}</p>
        }
      </div>
    </div>
  `,
  styles: [`
    .error-card {
      display: flex; gap: 1rem; padding: 1rem; margin: 1rem 0;
      background: #fef2f2; border: 1px solid #fecaca; border-radius: 0.5rem;
    }
    .error-card.warning { background: #fffbeb; border-color: #fed7aa; }
    .error-icon { font-size: 1.5rem; }
    .error-content h3 { margin: 0 0 0.25rem; color: #991b1b; font-weight: 600; }
    .error-card.warning .error-content h3 { color: #92400e; }
    .error-detail { margin: 0; color: #7f1d1d; }
    .error-meta { margin: 0.5rem 0 0; font-size: 0.75rem; color: #999; }
  `],
})
export class ErrorDisplayComponent {
  title = input('Error');
  detail = input('');
  correlationId = input<string>();
  statusClass = input<'error' | 'warning'>('error');
}
