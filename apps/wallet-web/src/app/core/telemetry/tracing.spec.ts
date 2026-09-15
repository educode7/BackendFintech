import { initializeTracing } from './tracing';

describe('tracing', () => {
  it('should export initializeTracing function', () => {
    expect(typeof initializeTracing).toBe('function');
  });

  it('should call initializeTracing without throwing', () => {
    expect(() => initializeTracing()).not.toThrow();
  });
});
