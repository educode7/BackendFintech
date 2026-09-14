export const environment = {
  production: false,
  apiGateway: 'http://localhost:8080',
  paymentService: 'http://localhost:8081',
  accountService: 'http://localhost:8082',
  notificationService: 'http://localhost:8083',
  oidc: {
    issuer: 'http://localhost:8180/realms/wallet',
    clientId: 'wallet-frontend',
    scope: 'openid profile email',
  },
};
