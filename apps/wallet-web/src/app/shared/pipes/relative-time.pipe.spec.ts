import { RelativeTimePipe } from './relative-time.pipe';

describe('RelativeTimePipe', () => {
  let pipe: RelativeTimePipe;

  beforeEach(() => {
    pipe = new RelativeTimePipe();
  });

  it('should create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('should return "just now" for recent timestamps', () => {
    const now = new Date();
    const result = pipe.transform(now);
    expect(result).toBe('just now');
  });

  it('should return minutes ago', () => {
    const fiveMinAgo = new Date(Date.now() - 5 * 60 * 1000);
    const result = pipe.transform(fiveMinAgo);
    expect(result).toBe('5m ago');
  });

  it('should return hours ago', () => {
    const twoHrAgo = new Date(Date.now() - 2 * 60 * 60 * 1000);
    const result = pipe.transform(twoHrAgo);
    expect(result).toBe('2h ago');
  });

  it('should return days ago', () => {
    const threeDaysAgo = new Date(Date.now() - 3 * 24 * 60 * 60 * 1000);
    const result = pipe.transform(threeDaysAgo);
    expect(result).toBe('3d ago');
  });

  it('should handle ISO string input', () => {
    const now = new Date();
    const result = pipe.transform(now.toISOString());
    expect(result).toBe('just now');
  });
});
