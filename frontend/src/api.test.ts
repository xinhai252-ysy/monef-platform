import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const user = {
  email: "admin@monef.local",
  name: "Admin",
  role: "ADMIN",
  tenantId: "acme",
  demo: false,
};
const response = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
let fetchMock: ReturnType<typeof vi.fn>;
let events: EventTarget;
beforeEach(() => {
  vi.resetModules();
  fetchMock = vi.fn();
  events = new EventTarget();
  vi.stubGlobal("fetch", fetchMock);
  vi.stubGlobal("window", events);
});
afterEach(() => vi.unstubAllGlobals());

describe("session authentication", () => {
  it("normalizes email, preserves password, sends CSRF and refreshes it after login", async () => {
    fetchMock
      .mockResolvedValueOnce(response({ token: "before", headerName: "X-CSRF-TOKEN" }))
      .mockResolvedValueOnce(response(user))
      .mockResolvedValueOnce(response({ token: "after", headerName: "X-CSRF-TOKEN" }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    const { api } = await import("./api");
    await api.login(" Admin@Monef.Local ", " password with spaces ");
    await api.logout();
    expect(JSON.parse(fetchMock.mock.calls[1][1].body)).toEqual({
      email: "admin@monef.local",
      password: " password with spaces ",
    });
    expect(fetchMock.mock.calls[1][1]).toMatchObject({
      credentials: "include",
      headers: { "X-CSRF-TOKEN": "before" },
    });
    expect(fetchMock.mock.calls[2][0]).toBe("/api/auth/csrf");
    expect(fetchMock.mock.calls[3][1].headers["X-CSRF-TOKEN"]).toBe("after");
  });
  it("reports a wrong password without treating it as an expired active session", async () => {
    const expired = vi.fn();
    events.addEventListener("session-expired", expired);
    fetchMock
      .mockResolvedValueOnce(response({ token: "old", headerName: "X-CSRF-TOKEN" }))
      .mockResolvedValueOnce(response({ message: "邮箱或密码错误" }, 401))
      .mockResolvedValueOnce(response({ token: "fresh", headerName: "X-CSRF-TOKEN" }))
      .mockResolvedValueOnce(response(user));
    const { api } = await import("./api");
    await expect(api.login(user.email, "wrong")).rejects.toThrow("邮箱或密码错误");
    await api.login(user.email, "correct");
    expect(expired).not.toHaveBeenCalled();
    expect(fetchMock.mock.calls[2][0]).toBe("/api/auth/csrf");
  });
  it("expires the session even when an unauthorized response is not JSON", async () => {
    const expired = vi.fn();
    events.addEventListener("session-expired", expired);
    fetchMock.mockResolvedValueOnce(new Response("Unauthorized", { status: 401 }));
    const { api } = await import("./api");
    await expect(api.accounts()).rejects.toThrow("请求失败 (401)");
    expect(expired).toHaveBeenCalledOnce();
  });
  it("refreshes a rejected CSRF token on the next deliberate attempt without replaying a write", async () => {
    fetchMock
      .mockResolvedValueOnce(response({ token: "stale", headerName: "X-CSRF-TOKEN" }))
      .mockResolvedValueOnce(response({ message: "安全令牌已过期" }, 403))
      .mockResolvedValueOnce(response({ token: "fresh", headerName: "X-CSRF-TOKEN" }))
      .mockResolvedValueOnce(response(user));
    const { api } = await import("./api");
    await expect(api.login(user.email, "password")).rejects.toThrow("安全令牌已过期");
    expect(fetchMock).toHaveBeenCalledTimes(2);
    await api.login(user.email, "password");
    expect(fetchMock.mock.calls[3][1].headers["X-CSRF-TOKEN"]).toBe("fresh");
  });
  it("shows a useful connection error when the backend is unavailable", async () => {
    fetchMock.mockRejectedValueOnce(new TypeError("Failed to fetch"));
    const { api } = await import("./api");
    await expect(api.me()).rejects.toThrow("无法连接服务");
  });
});
