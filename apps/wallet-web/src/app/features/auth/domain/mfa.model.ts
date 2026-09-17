export interface MfaSetupRequest {
  // POST /auth/mfa/setup — empty body, auth via Bearer token
}

export interface MfaSetupResponse {
  qr_code: string;
  secret: string;
  recovery_codes: string[];
  issuer: string;
  account_name: string;
}

export interface MfaVerifyRequest {
  code: string;
}

export interface MfaVerifyResponse {
  verified: boolean;
  backup_codes_remaining: number;
}

export interface MfaDisableRequest {
  code: string;
}

export interface MfaDisableResponse {
  // 204 No Content — no body
}
