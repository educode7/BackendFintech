import { defineConfig, type Plugin } from 'vitest/config';
import { readFileSync } from 'fs';
import { resolve } from 'path';

/**
 * Angular 22 is zoneless, but the generated init-testbed.js still contains:
 *   if (typeof Zone !== "undefined") { await import("zone.js/testing"); }
 *
 * Vite resolves this dynamically even though it's conditional. This plugin
 * rewrites the init-testbed.js virtual file to remove the zone.js import
 * entirely, and maps any bare "zone.js" or "zone.js/testing" imports to
 * an empty module.
 */
function zoneJsStubPlugin(): Plugin {
  return {
    name: 'zone-js-stub',
    enforce: 'pre',
    transform(code, id) {
      // Strip zone.js/testing dynamic import from the generated init-testbed
      if (id.endsWith('init-testbed.js') || id.includes('init-testbed')) {
        const patched = code
          .replace(
            /if\s*\(typeof\s+Zone\s*!==\s*["']undefined["']\)\s*\{[\s\S]*?import\(["']zone\.js\/testing["']\)[\s\S]*?\}/,
            '// zone.js removed — Angular 22 is zoneless'
          );
        return { code: patched, map: null };
      }
      return null;
    },
    resolveId(id) {
      if (id === 'zone.js' || id === 'zone.js/testing') {
        return '\0zone-stub';
      }
      return null;
    },
    load(id) {
      if (id === '\0zone-stub') {
        return 'export {};';
      }
      return null;
    },
  };
}

export default defineConfig({
  plugins: [zoneJsStubPlugin()],
});
