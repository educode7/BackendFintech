import { Component, input } from '@angular/core';

@Component({
  standalone: true,
  selector: 'app-page-header',
  template: `
    <header class="page-header">
      <h1>{{ title() }}</h1>
      @if (subtitle()) {
        <p class="subtitle">{{ subtitle() }}</p>
      }
    </header>
  `,
  styles: [`
    .page-header { margin-bottom: 2rem; }
    h1 { font-size: 1.75rem; font-weight: 600; color: #1a1a2e; }
    .subtitle { color: #666; margin-top: 0.25rem; }
  `],
})
export class PageHeaderComponent {
  title = input.required<string>();
  subtitle = input<string>();
}
