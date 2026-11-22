import { ref, onMounted, onUnmounted } from "vue";
import { api } from "../api";
import type { User } from "../types";
interface SessionOptions {
  onAuthenticated: () => Promise<void>;
  onSignedOut: () => void;
  onError: (message: string) => void;
}
export function useSession(options: SessionOptions) {
  const user = ref<User | null>(null);
  const initializing = ref(true);
  const loginEmail = ref(import.meta.env.DEV ? "demo@monef.local" : "");
  const loginPassword = ref(import.meta.env.DEV ? "MonefDemo!2026" : "");
  const loginBusy = ref(false);
  const loginError = ref("");
  function clearSession() {
    user.value = null;
    loginPassword.value = "";
    options.onSignedOut();
  }
  async function authenticate(action: () => Promise<User>) {
    if (loginBusy.value) return;
    loginBusy.value = true;
    loginError.value = "";
    try {
      user.value = await action();
      loginPassword.value = "";
      await options.onAuthenticated();
    } catch (error) {
      loginError.value = (error as Error).message;
    } finally {
      loginBusy.value = false;
    }
  }
  function login() {
    return authenticate(() => api.login(loginEmail.value, loginPassword.value));
  }
  function register(name: string, email: string, password: string) {
    return authenticate(() => api.register(name, email, password));
  }
  async function logout() {
    try {
      await api.logout();
      clearSession();
      loginError.value = "";
    } catch (error) {
      options.onError((error as Error).message);
    }
  }
  function sessionExpired() {
    clearSession();
    loginError.value = "登录已过期，请重新登录";
  }
  onMounted(async () => {
    window.addEventListener("session-expired", sessionExpired);
    try {
      user.value = await api.me();
      await options.onAuthenticated();
    } catch {
      clearSession();
    } finally {
      initializing.value = false;
    }
  });
  onUnmounted(() => window.removeEventListener("session-expired", sessionExpired));
  return {
    user,
    initializing,
    loginEmail,
    loginPassword,
    loginBusy,
    loginError,
    login,
    register,
    logout,
  };
}
