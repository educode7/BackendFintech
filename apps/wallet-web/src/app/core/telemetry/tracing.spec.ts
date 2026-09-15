import { initializeTracing } from './tracing';

describe('tracing', () => {
  it('should call initializeTracing without throwing', async () => {
    await expect(initializeTracing()).resolves.not.toThrow();
  });
});
