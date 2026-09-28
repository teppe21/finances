import { defineConfig } from 'vitest/config';
import path from 'path';

export default defineConfig({
  test: {
    globals: true,
    environment: 'node',
    include: ['tests/**/*.test.ts'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html']
    }
  },
  resolve: {
    alias: {
      '@': path.resolve('./src'),
      '@core': path.resolve('./src/core'),
      '@database': path.resolve('./src/database'),
      '@services': path.resolve('./src/services'),
      '@store': path.resolve('./src/store'),
      '@theme': path.resolve('./src/theme'),
      '@i18n': path.resolve('./src/i18n'),
      '@utils': path.resolve('./src/utils'),
      '@components': path.resolve('./src/components')
    }
  }
});
