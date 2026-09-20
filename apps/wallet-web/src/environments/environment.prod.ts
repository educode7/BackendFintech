export const environment = {
  production: true,
  apiGateway: '/api',
  paymentService: '',
  accountService: '',
  notificationService: '',
  oidc: {
    issuer: 'http://localhost:8180/realms/wallet',
    clientId: 'wallet-frontend',
    scope: 'openid profile email',
  },
};
