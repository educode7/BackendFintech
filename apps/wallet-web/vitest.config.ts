import { defineConfig, type Plugin } from 'vitest/config';
import tsconfigPaths from 'vite-tsconfig-paths';

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
  plugins: [tsconfigPaths({ root: __dirname }), stripZoneJsImport()],
  test: {
    globals: true,
  },
});
