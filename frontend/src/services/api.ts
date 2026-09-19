export const BASE_URL = 'http://localhost:8080/api';

interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: any;
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

  if (!response.ok) {
    let message = `HTTP error! status: ${response.status}`;
    try {
      const text = await response.text();
      if (text) {
        const data = JSON.parse(text);
        if (data && typeof data.message === 'string' && data.message) {
          message = data.message;
        }
      }
    } catch {
      // 忽略解析失败，使用默认错误信息
    }
    throw new Error(message);
  }

  const text = await response.text();
  if (!text) {
    return undefined as T;
  }

  return JSON.parse(text) as T;
}
