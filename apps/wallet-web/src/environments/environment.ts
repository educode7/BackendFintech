export const environment = {
  production: false,
  apiGateway: 'http://localhost:8080',
  paymentService: 'http://localhost:8081',
  accountService: 'http://localhost:8082',
  notificationService: 'http://localhost:8083',
  // Host-published OTLP/HTTP endpoint of otel-collector (docker-compose).
  otelEndpoint: 'http://localhost:4318/v1/traces',
  oidc: {
    issuer: 'http://localhost:8180/realms/wallet',
    clientId: 'wallet-frontend',
    scope: 'openid profile email',
  },
};
