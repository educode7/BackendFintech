/**
 * Global test providers — forces zoneless change detection for all tests.
 * Configured via angular.json → test.options.providersFile.
 */
import { provideZonelessChangeDetection } from '@angular/core';

export default [provideZonelessChangeDetection()];
