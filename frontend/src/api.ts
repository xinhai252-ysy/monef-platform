import type {
  Account,
  AccountInput,
  ChatResult,
  Conversion,
  Dashboard,
  Entry,
  EntryInput,
  Page,
  Rate,
  User,
} from "./types";
export const previewMode = import.meta.env.VITE_PREVIEW_MODE === "true";
let csrf: { token: string; headerName: string } | null = null;
async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  if (previewMode) {
    const { demoRequest } = await import("./demo");
    return demoRequest<T>(path, options);
  }
  const method = options.method ?? "GET";
  if (method !== "GET" && !csrf) csrf = await request("/auth/csrf");
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  if (method !== "GET" && csrf) headers[csrf.headerName] = csrf.token;
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), path === "/ai/chat" ? 90000 : 15000);
  try {
    const res = await fetch("/api" + path, {
      ...options,
      headers,
      credentials: "include",
      signal: controller.signal,
    });
    if (res.status === 204) return undefined as T;
    if (res.status === 401) {
      csrf = null;
      if (path !== "/auth/login" && path !== "/auth/me")
        window.dispatchEvent(new Event("session-expired"));
    }
    if (res.status === 403) csrf = null;
    const data = await res.json().catch(() => null);
    if (!res.ok) throw new Error(data?.message ?? `请求失败 (${res.status})`);
    if (data === null) throw new Error("服务返回了无效数据，请确认后端已启动");
    return data as T;
  } catch (e) {
    if (e instanceof DOMException && e.name === "AbortError")
      throw new Error("请求超时，请稍后重试");
    if (e instanceof TypeError) throw new Error("无法连接服务，请确认后端已启动后重试");
    throw e;
  } finally {
    clearTimeout(timeout);
  }
}
function params(values: Record<string, string | number | undefined>) {
  return new URLSearchParams(
    Object.entries(values)
      .filter(([, v]) => v !== undefined && v !== "")
      .map(([k, v]) => [k, String(v)]),
  ).toString();
}
export const api = {
  me: () => request<User>("/auth/me"),
  login: async (email: string, password: string) => {
    const user = await request<User>("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email: email.trim().toLowerCase(), password }),
    });
    csrf = null;
    return user;
  },
  register: async (name: string, email: string, password: string) => {
    if (previewMode) throw new Error("交互演示不支持真实注册，请使用后端部署版本");
    const user = await request<User>("/auth/register", {
      method: "POST",
      body: JSON.stringify({ name: name.trim(), email: email.trim().toLowerCase(), password }),
    });
    csrf = null;
    return user;
  },
  logout: async () => {
    await request("/auth/logout", { method: "POST" });
    csrf = null;
  },
  accounts: () => request<Account[]>("/accounts"),
  createAccount: (data: AccountInput) =>
    request<Account>("/accounts", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  accountStatus: (id: string, active: boolean) =>
    request<Account>(`/accounts/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ active }),
    }),
  dashboard: (month: string) => request<Dashboard>(`/dashboard?month=${month}`),
  entries: (filter: Record<string, string | number | undefined>) =>
    request<Page<Entry>>("/transactions?" + params(filter)),
  createEntry: (data: EntryInput) =>
    request<Entry>("/transactions", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  voidEntry: (id: string) => request<Entry>(`/transactions/${id}/void`, { method: "POST" }),
  convert: (base: string, quote: string, amount: string, date?: string) =>
    request<Conversion>("/fx/convert?" + params({ base, quote, amount, date })),
  history: (base: string, quote: string, page = 0) =>
    request<Page<Rate>>("/fx/history?" + params({ base, quote, page })),
  chat: (message: string, month: string) =>
    request<ChatResult>("/ai/chat", {
      method: "POST",
      body: JSON.stringify({ message, month }),
    }),
  audit: (page = 0) =>
    request<
      Page<{
        id: string;
        actor: string;
        action: string;
        subjectId: string;
        detail: string;
        createdAt: string;
      }>
    >("/audit?page=" + page),
};
