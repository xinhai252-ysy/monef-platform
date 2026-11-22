<script setup lang="ts">
import { useFinanceWorkspace } from "./composables/useFinanceWorkspace";
import { provideWorkspace } from "./composables/workspaceContext";
import { previewMode } from "./api";
import { currentMonth } from "./format";
import {
  ChevronDown,
  ChevronRight,
  Plus,
  Download,
  CalendarDays,
  Info,
  Check,
  Menu,
} from "lucide-vue-next";
import LoginView from "./views/LoginView.vue";
import WorkspaceDialogs from "./components/layout/WorkspaceDialogs.vue";
import WorkspaceSidebar from "./components/layout/WorkspaceSidebar.vue";
import DashboardView from "./views/DashboardView.vue";
import TransactionsView from "./views/TransactionsView.vue";
import AccountsView from "./views/AccountsView.vue";
import AuditView from "./views/AuditView.vue";
import ExchangeCard from "./components/ExchangeCard.vue";
import AiPanel from "./components/AiPanel.vue";
const workspace = useFinanceWorkspace();
provideWorkspace(workspace);
const {
  user,
  initializing,
  route,
  mobileNav,
  loading,
  error,
  toast,
  working,
  month,
  dashboard,
  accounts,
  page,
  status,
  refreshKey,
  modal,
  canWrite,
  title,
  load,
  openEntry,
  openAccount,
  exportCsv,
} = workspace;
</script>
<template>
  <div v-if="initializing" class="boot-screen">
    <span class="brand-symbol"></span>
    <p>正在进入财务工作台…</p>
  </div>
  <LoginView v-else-if="!user" />
  <div v-else class="app-shell">
    <div v-if="mobileNav" class="sidebar-shade" @click="mobileNav = false"></div>
    <WorkspaceSidebar />
    <div class="main-shell">
      <header class="topbar">
        <div class="breadcrumb">
          <button class="mobile-menu icon-button" aria-label="展开导航" @click="mobileNav = true">
            <Menu :size="20" />
          </button>
          <span>工作台</span>
          <ChevronRight :size="13" />
          <strong>{{ title }}</strong>
        </div>
        <div class="topbar-right">
          <span class="environment-tag">
            <span class="live-dot"></span>
            {{ previewMode ? "交互演示" : user.demo ? "本地演示" : "企业工作区" }}
          </span>
          <span class="topbar-divider"></span>
          <span class="topbar-currency">
            USD
            <ChevronDown :size="12" />
          </span>
          <button class="avatar small" aria-label="账户信息" @click="modal = 'workspace'">
            {{ user.name.slice(0, 1) }}
          </button>
        </div>
      </header>
      <main>
        <div class="page-heading">
          <div>
            <span class="eyebrow">
              {{
                route === "dashboard"
                  ? "YOUR BUSINESS, AT A GLANCE."
                  : route === "assistant"
                    ? "INTELLIGENCE, GROUNDED IN YOUR DATA."
                    : "EVERY DETAIL MATTERS."
              }}
            </span>
            <h1>
              {{ title }}
              <span v-if="route === 'dashboard'" class="title-dot">.</span>
            </h1>
            <p>
              {{
                route === "dashboard"
                  ? "欢迎回来，" + user.name + "。每一笔流动，都心中有数。"
                  : route === "transactions"
                    ? "追踪每一笔收支，让业务的来龙去脉清晰可见。"
                    : route === "accounts"
                      ? "统一查看账户与资金余额，掌握企业资金分布。"
                      : route === "exchange"
                        ? "跨越币种，保持清晰。查询、换算与追溯汇率。"
                        : route === "assistant"
                          ? "从财务问题出发，让账本提供有依据的答案。"
                          : "关键操作可追溯，企业数据有边界。"
              }}
            </p>
          </div>
          <div class="page-actions">
            <label v-if="!['accounts', 'exchange', 'audit'].includes(route)" class="month-control">
              <CalendarDays :size="16" />
              <input
                type="month"
                v-model="month"
                min="2000-01"
                :max="currentMonth()"
                aria-label="统计月份"
              />
            </label>
            <button
              v-if="['dashboard', 'transactions'].includes(route)"
              class="secondary"
              aria-label="导出交易 CSV"
              :disabled="working"
              @click="exportCsv"
            >
              <Download :size="16" />
              <span>导出</span>
            </button>
            <button
              v-if="canWrite && ['dashboard', 'transactions'].includes(route)"
              class="primary"
              @click="openEntry"
            >
              <Plus :size="17" />
              新增交易
            </button>
            <button v-if="canWrite && route === 'accounts'" class="primary" @click="openAccount">
              <Plus :size="17" />
              新增账户
            </button>
          </div>
        </div>
        <div v-if="previewMode" class="preview-banner">
          <Info :size="14" />
          <span>交互演示使用示例数据，修改仅保存在当前浏览器。企业部署版本使用服务端数据。</span>
        </div>
        <div v-if="error" class="error-banner" role="alert">
          <Info :size="18" />
          <span>{{ error }}</span>
          <button @click="load">重新加载</button>
        </div>
        <div v-if="loading && !dashboard" class="skeleton-grid">
          <div v-for="n in 4" :key="n" class="skeleton"></div>
        </div>
        <DashboardView v-if="route === 'dashboard' && dashboard" />
        <TransactionsView v-if="['dashboard', 'transactions'].includes(route) && dashboard" />
        <AccountsView v-if="route === 'accounts'" />
        <div v-if="route === 'exchange'" class="exchange-page">
          <ExchangeCard expanded />
        </div>
        <AiPanel v-if="route === 'assistant'" :month="month" :key="refreshKey" expanded />
        <AuditView v-if="route === 'audit'" />
        <footer class="page-footer">
          <span>
            <span class="brand-symbol tiny"></span>
            Monef
            <span>让资金管理，更进一步。</span>
          </span>
          <span>
            {{
              dashboard
                ? "更新于 " +
                  new Date(dashboard.generatedAt).toLocaleTimeString("zh-CN", {
                    hour: "2-digit",
                    minute: "2-digit",
                  })
                : "企业智能财务工作台"
            }}
            <span class="footer-dot">·</span>
            <button @click="modal = 'help'">数据口径</button>
          </span>
        </footer>
      </main>
    </div>
  </div>
  <WorkspaceDialogs />
  <Transition name="toast">
    <div v-if="toast" class="toast-message" role="status">
      <Check :size="16" />
      {{ toast }}
    </div>
  </Transition>
</template>
