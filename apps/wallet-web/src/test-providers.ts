import { provideZonelessChangeDetection } from '@angular/core';

/**
 * Global test providers — forces zoneless change detection for all tests.
 * Configured via angular.json → test.options.providersFile.
 */
export default [provideZonelessChangeDetection()];
