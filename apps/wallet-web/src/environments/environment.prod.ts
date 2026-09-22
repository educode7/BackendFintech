export const environment = {
  production: true,
  apiGateway: '/api',
  paymentService: '',
  accountService: '',
  notificationService: '',
  // Host-published OTLP/HTTP endpoint of otel-collector (docker-compose).
  // The repo contains no frontend reverse-proxy/Dockerfile from which to derive
  // another hostname, so this assumes the browser runs on the Docker host
  // (browser -> host:4318). Adjust per deployment topology if that changes.
  otelEndpoint: 'http://localhost:4318/v1/traces',
  oidc: {
    issuer: 'http://localhost:8180/realms/wallet',
    clientId: 'wallet-frontend',
    scope: 'openid profile email',
  },
};
