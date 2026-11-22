import { createRenderer, nextTick } from "vue";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "../api";
import { useSession } from "./useSession";

vi.mock("../api", () => ({
  api: { me: vi.fn(), login: vi.fn(), register: vi.fn(), logout: vi.fn() },
}));

const user = {
  email: "admin@monef.local",
  name: "Admin",
  role: "ADMIN" as const,
  tenantId: "acme",
  demo: false,
};

// A renderer mounts lifecycle hooks without relying on a browser or a simulated DOM.
const renderer = createRenderer<object, object>({
  createElement: () => ({}),
  createText: () => ({}),
  createComment: () => ({}),
  insert: () => {},
  remove: () => {},
  setText: () => {},
  setElementText: () => {},
  parentNode: () => null,
  nextSibling: () => null,
  patchProp: () => {},
});

const unmounts: Array<() => void> = [];
const settle = async () => {
  await new Promise<void>((resolve) => setTimeout(resolve, 0));
  await nextTick();
};

function mountSession() {
  let session!: ReturnType<typeof useSession>;
  const options = {
    onAuthenticated: vi.fn().mockResolvedValue(undefined),
    onSignedOut: vi.fn(),
    onError: vi.fn(),
  };
  const app = renderer.createApp({
    setup() {
      session = useSession(options);
      return () => null;
    },
  });
  app.mount({});
  unmounts.push(() => app.unmount());
  return { session, options };
}

beforeEach(() => {
  vi.resetAllMocks();
  vi.stubGlobal("window", new EventTarget());
  vi.mocked(api.me).mockRejectedValue(new Error("请先登录"));
});

afterEach(() => {
  unmounts.splice(0).forEach((unmount) => unmount());
  vi.unstubAllGlobals();
});

describe("session lifecycle", () => {
  it("opens the new workspace after registration and clears the password", async () => {
    const { session, options } = mountSession();
    await settle();
    vi.mocked(api.register).mockResolvedValue(user);
    session.loginPassword.value = "StrongPassword!23";
    await session.register("Admin", user.email, session.loginPassword.value);
    expect(api.register).toHaveBeenCalledWith("Admin", user.email, "StrongPassword!23");
    expect(session.user.value).toEqual(user);
    expect(session.loginPassword.value).toBe("");
    expect(options.onAuthenticated).toHaveBeenCalledOnce();
  });
  it("restores an existing session before loading business data", async () => {
    vi.mocked(api.me).mockResolvedValue(user);
    const { session, options } = mountSession();
    await settle();
    expect(session.user.value).toEqual(user);
    expect(session.initializing.value).toBe(false);
    expect(options.onAuthenticated).toHaveBeenCalledOnce();
  });

  it("shows a rejected login and allows a successful retry and logout", async () => {
    const { session, options } = mountSession();
    await settle();
    session.loginEmail.value = user.email;
    session.loginPassword.value = "incorrect";
    vi.mocked(api.login).mockRejectedValueOnce(new Error("邮箱或密码错误"));
    await session.login();
    expect(session.loginError.value).toBe("邮箱或密码错误");
    expect(session.user.value).toBeNull();
    vi.mocked(api.login).mockResolvedValueOnce(user);
    session.loginPassword.value = "valid-password";
    await session.login();
    expect(session.user.value).toEqual(user);
    expect(session.loginPassword.value).toBe("");
    expect(options.onAuthenticated).toHaveBeenCalledOnce();
    await session.logout();
    expect(session.user.value).toBeNull();
    expect(options.onSignedOut).toHaveBeenCalledTimes(2);
  });

  it("clears business state on expiration and removes its listener on unmount", async () => {
    vi.mocked(api.me).mockResolvedValue(user);
    const { session, options } = mountSession();
    await settle();
    window.dispatchEvent(new Event("session-expired"));
    expect(session.user.value).toBeNull();
    expect(session.loginError.value).toBe("登录已过期，请重新登录");
    expect(options.onSignedOut).toHaveBeenCalledOnce();
    unmounts.pop()!();
    window.dispatchEvent(new Event("session-expired"));
    expect(options.onSignedOut).toHaveBeenCalledOnce();
  });
});
