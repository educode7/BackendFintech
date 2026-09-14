/**
 * RFC 9457 Problem+JSON error response.
 */
export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  code?: string;
  correlationId?: string;
}
