import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { initializeTracing } from './app/core/telemetry/tracing';

// Initialize OpenTelemetry tracing before Angular bootstrap
initializeTracing()
  .then(() => bootstrapApplication(App, appConfig))
  .catch((err) => console.error(err));
