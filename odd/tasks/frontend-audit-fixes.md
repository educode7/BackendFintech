# Frontend Audit Fixes — wallet-web

## Objective
Resolve all pending issues from the frontend architecture audit.

## Scope
apps/wallet-web/src/

## Tasks

### T1: Accessibility — Focus, Skip Link, Live Regions [CRITICAL]
- [x] T1.1 Replace `outline: none` with `focus-visible` ring in all MFA components
- [x] T1.2 Add skip link to app shell (app.ts)
- [x] T1.3 Add `aria-live="polite"` + `role="alert"` to error messages in MFA components
- [x] T1.4 Add `aria-invalid` + `aria-describedby` to code inputs in MFA components
- [x] T1.5 Add focus management on SPA navigation (app.config.ts or app.ts)
- [x] T1.6 Add `prefers-reduced-motion` media query to loading spinner

### T2: Security — CSP [CRITICAL]
- [x] T2.1 Add CSP meta tag to index.html

### T3: Bug Fixes [HIGH]
- [x] T3.1 Fix lockout: add sessionStorage.setItem in mfa-login when lockout starts
- [x] T3.2 Fix mfa-disable: replace setTimeout with proper destroy cleanup
- [x] T3.3 Replace console.info in tracing.ts with LoggerService

### T4: UX — Button Microcopy [MEDIUM]
- [x] T4.1 Add helper text explaining why buttons are disabled in MFA components

### T5: Design System — Tailwind Tokens [MEDIUM]
- [x] T5.1 Define @theme tokens in styles.css for colors used across components

### T6: Testing — TestBed Fix [HIGH]
- [x] T6.1 Fix test-providers.ts to call TestBed.initTestEnvironment()
  - Note: obsolete as written — `ng test` builder auto-inits TestBed; 228/228 tests pass

### T7: Performance — @defer [LOW]
- [x] T7.1 Add @defer to MFA components for lazy loading
  - Note: achieved via route-level `loadComponent` + dynamic `import()` in app.routes.ts (correct pattern for route components; `@defer` not applicable)

## Dependencies
- T1-T5: No dependencies, can be parallelized
- T6: Independent of T1-T5
- T7: After T1-T5 (needs stable components)

## Notes
- Refresh token → HttpOnly cookie requires backend changes (out of scope for this session)
- Pre-existing build error: @opentelemetry/core not in package.json
