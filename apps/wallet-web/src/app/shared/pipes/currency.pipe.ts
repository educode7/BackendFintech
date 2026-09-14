import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'currency', standalone: true })
export class CurrencyPipe implements PipeTransform {
  transform(value: string | number, currencyCode = 'USD'): string {
    const num = typeof value === 'string' ? parseFloat(value) : value;
    if (isNaN(num)) return value as string;
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currencyCode,
    }).format(num);
  }
}
