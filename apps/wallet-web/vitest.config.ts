import { defineConfig, type Plugin } from 'vitest/config';
import path from 'path';

/**
 * Angular 22 is zoneless. The generated init-testbed.js contains:
 *   if (typeof Zone !== "undefined") { await import("zone.js/testing"); }
 *
 * Vite resolves dynamic imports statically even inside conditionals.
 * This plugin strips that import at transform time.
 */
function stripZoneJsImport(): Plugin {
  return {
    name: 'strip-zone-js',
    enforce: 'pre',
    transform(code, id) {
      if (id.endsWith('init-testbed.js') || id.includes('init-testbed')) {
        const patched = code.replace(
          /if\s*\(typeof\s+Zone\s*!==\s*["']undefined["']\)\s*\{[\s\S]*?import\(["']zone\.js\/testing["']\)[\s\S]*?\}/,
          '// zone.js removed — Angular 22 is zoneless'
        );
        return { code: patched, map: null };
      }
      return null;
    },
  };
}

export default defineConfig({
  plugins: [stripZoneJsImport()],
  resolve: {
    alias: {
      '@core': path.resolve(__dirname, './src/app/core'),
      '@features': path.resolve(__dirname, './src/app/features'),
      '@shared': path.resolve(__dirname, './src/app/shared'),
      '@env': path.resolve(__dirname, './src/environments'),
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test-setup.ts'],
  },
});
