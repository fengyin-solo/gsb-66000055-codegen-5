export const BASE_URL = 'http://localhost:8080/api';

interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: any;
}

export class ApiError extends Error {
  status: number;
  reasonCode?: string;

  constructor(status: number, message: string, reasonCode?: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.reasonCode = reasonCode;
  }
}

export async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> || {}),
  };

  const config: RequestInit = {
    ...options,
    headers,
  };

  if (options.body !== undefined && options.body !== null) {
    config.body = typeof options.body === 'string' ? options.body : JSON.stringify(options.body);
  }

  const response = await fetch(`${BASE_URL}${url}`, config);

  const text = await response.text();

  if (!response.ok) {
    let message = `HTTP error! status: ${response.status}`;
    let reasonCode: string | undefined;
    try {
      const body = JSON.parse(text);
      if (body && typeof body.message === 'string') {
        message = body.message;
      }
      if (body && typeof body.reasonCode === 'string') {
        reasonCode = body.reasonCode;
      }
    } catch {
      // 响应体不是 JSON，使用默认消息
    }
    throw new ApiError(response.status, message, reasonCode);
  }

  if (!text) {
    return undefined as T;
  }

  return JSON.parse(text) as T;
}
