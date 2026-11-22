<script setup lang="ts">
import { useWorkspace } from "../../composables/workspaceContext";
import {
  Sparkles,
  Settings2,
  ChevronDown,
  ArrowUpRight,
  LogOut,
  Building2,
  Info,
} from "lucide-vue-next";

const { user, logout, nav, route, mobileNav, dashboard, modal, canWrite, changeRoute } =
  useWorkspace();
</script>
<template>
  <aside v-if="user" class="sidebar" :class="{ open: mobileNav }">
    <a href="#dashboard" class="brand" @click.prevent="changeRoute('dashboard')">
      <span class="brand-symbol"></span>
      MONEF
      <span class="brand-period">®</span>
    </a>
    <button class="workspace-card" @click="modal = 'workspace'">
      <span class="workspace-avatar"><Building2 :size="20" /></span>
      <span>
        <strong>{{ user.demo ? "Acme Studio" : "企业工作区" }}</strong>
        <small>{{ user.demo ? "示例工作区" : "团队工作区" }}</small>
      </span>
      <ChevronDown :size="14" />
    </button>
    <div class="nav-caption">WORKSPACE</div>
    <nav aria-label="主导航">
      <button
        v-for="item in nav"
        :key="item.id"
        :class="{ active: route === item.id }"
        @click="changeRoute(item.id)"
      >
        <component :is="item.icon" :size="19" />
        <span>{{ item.label }}</span>
        <span v-if="item.id === 'assistant'" class="nav-ai">AI</span>
        <span v-else-if="item.id === route" class="nav-marker"></span>
      </button>
    </nav>
    <div class="sidebar-insight">
      <div class="mini-orb"><Sparkles :size="17" /></div>
      <h3>让每个数字更有意义</h3>
      <p>
        发现趋势，理解变化。
        <br />
        你的财务助手已就位。
      </p>
      <button @click="changeRoute('assistant')">
        探索 Monef AI
        <ArrowUpRight :size="15" />
      </button>
    </div>
    <div class="sidebar-bottom">
      <button
        v-if="canWrite"
        class="nav-bottom"
        :class="{ active: route === 'audit' }"
        @click="changeRoute('audit')"
      >
        <Settings2 :size="18" />
        系统与审计
      </button>
      <button class="nav-bottom" @click="modal = 'help'">
        <Info :size="18" />
        数据口径说明
      </button>
      <div class="profile">
        <span class="avatar">{{ user.name.slice(0, 1) }}</span>
        <span>
          <strong>{{ user.name }}</strong>
          <small>{{ canWrite ? "工作区管理员" : "财务分析员" }}</small>
        </span>
        <button class="icon-button" aria-label="退出登录" @click="logout">
          <LogOut :size="16" />
        </button>
      </div>
    </div>
  </aside>
</template>
