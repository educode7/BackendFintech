import { CurrencyPipe } from './currency.pipe';

describe('CurrencyPipe', () => {
  let pipe: CurrencyPipe;

  beforeEach(() => {
    pipe = new CurrencyPipe();
  });

  it('should create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('should format USD amount', () => {
    const result = pipe.transform('1234.56', 'USD');
    expect(result).toContain('1,234.56');
    expect(result).toContain('$');
  });

  it('should format EUR amount', () => {
    const result = pipe.transform('1000', 'EUR');
    expect(result).toContain('1,000');
  });

  it('should handle zero', () => {
    const result = pipe.transform('0', 'USD');
    expect(result).toContain('0.00');
  });

  it('should handle invalid input', () => {
    const result = pipe.transform('invalid', 'USD');
    expect(result).toBe('invalid');
  });
});
