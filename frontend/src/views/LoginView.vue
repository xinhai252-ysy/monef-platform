<script setup lang="ts">
import { useWorkspace } from "../composables/workspaceContext";
import { ArrowUpRight, ShieldCheck, Info } from "lucide-vue-next";
import { ref } from "vue";
import { previewMode } from "../api";

const { loginEmail, loginPassword, loginBusy, loginError, login, register, devMode } =
  useWorkspace();
const registering = ref(false);
const name = ref("");
const confirmPassword = ref("");
const formError = ref("");

function switchMode() {
  registering.value = !registering.value;
  loginPassword.value = "";
  confirmPassword.value = "";
  loginError.value = "";
  formError.value = "";
}

async function submit() {
  formError.value = "";
  if (!registering.value) return login();
  if (loginPassword.value !== confirmPassword.value) {
    formError.value = "两次输入的密码不一致";
    return;
  }
  await register(name.value, loginEmail.value, loginPassword.value);
  if (!loginError.value) confirmPassword.value = "";
}
</script>
<template>
  <div class="login-screen">
    <div class="login-art">
      <a class="brand">
        <span class="brand-symbol"></span>
        MONEF
        <span class="brand-period">®</span>
      </a>
      <div class="login-copy">
        <span class="eyebrow">YOUR MONEY. IN FOCUS.</span>
        <h1>
          财务全貌，
          <br />
          一目了然
          <span>。</span>
        </h1>
        <p>
          从每一笔交易，到下一个商业决策。
          <br />
          让财务数据成为业务前进的力量。
        </p>
      </div>
      <div class="login-graphic">
        <div class="orbit orbit-one"></div>
        <div class="orbit orbit-two"></div>
        <span class="graphic-core"></span>
        <div class="floating-label">
          <span class="live-dot"></span>
          Clearer insights. Smarter decisions.
        </div>
      </div>
      <span class="login-foot">企业智能财务分析与资金管理平台</span>
    </div>
    <div class="login-form-wrap">
      <form class="login-form" @submit.prevent="submit">
        <span class="eyebrow">{{ registering ? "CREATE YOUR WORKSPACE" : "WELCOME BACK" }}</span>
        <h2>{{ registering ? "创建你的 Monef 账户" : "欢迎回到 Monef" }}</h2>
        <p>
          {{
            registering
              ? "注册后创建独立的空工作区，开始管理你的账本。"
              : "登录你的企业财务工作区。"
          }}
        </p>
        <label v-if="registering">
          姓名
          <input
            v-model="name"
            required
            maxlength="100"
            autocomplete="name"
            placeholder="你的姓名"
          />
        </label>
        <label>
          邮箱
          <input
            v-model="loginEmail"
            type="email"
            required
            maxlength="190"
            autocomplete="username"
            placeholder="you@company.com"
          />
        </label>
        <label>
          密码
          <input
            v-model="loginPassword"
            type="password"
            required
            :minlength="registering ? 12 : undefined"
            :maxlength="registering ? 72 : 200"
            :autocomplete="registering ? 'new-password' : 'current-password'"
            :placeholder="registering ? '至少 12 位字符' : '输入账户密码'"
          />
        </label>
        <label v-if="registering">
          确认密码
          <input
            v-model="confirmPassword"
            type="password"
            required
            autocomplete="new-password"
            placeholder="再次输入密码"
          />
        </label>
        <p v-if="loginError || formError" class="inline-error" role="alert">
          {{ formError || loginError }}
        </p>
        <button class="primary full" :disabled="loginBusy">
          {{
            loginBusy
              ? registering
                ? "正在创建…"
                : "正在登录…"
              : registering
                ? "注册并进入工作台"
                : "进入工作台"
          }}
          <ArrowUpRight :size="18" />
        </button>
        <p v-if="!previewMode" class="auth-switch">
          {{ registering ? "已有账户？" : "还没有账户？" }}
          <button type="button" class="text-button" :disabled="loginBusy" @click="switchMode">
            {{ registering ? "去登录" : "注册账户" }}
          </button>
        </p>
        <div v-if="devMode && !registering" class="demo-login-note">
          <Info :size="16" />
          <span>已填入本地演示账户。示例数据用于体验功能。</span>
        </div>
        <div class="secure-caption">
          <ShieldCheck :size="15" />
          会话认证 · 企业数据隔离
        </div>
      </form>
    </div>
  </div>
</template>
