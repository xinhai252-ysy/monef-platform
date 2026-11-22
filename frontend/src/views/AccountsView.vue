<script setup lang="ts">
import { useWorkspace } from "../composables/workspaceContext";
import { Plus, Building2, Info } from "lucide-vue-next";
import { money } from "../format";

const { working, accounts, canWrite, openAccount, toggleAccount } = useWorkspace();
</script>
<template>
  <div class="accounts-grid">
    <article
      v-for="(a, i) in accounts"
      :key="a.id"
      class="account-card"
      :class="{ inactive: !a.active }"
    >
      <div class="account-top">
        <span class="bank-icon"><Building2 :size="23" /></span>
        <span class="account-currency">
          {{ a.currency }}
          <span class="live-dot" v-if="a.active"></span>
        </span>
      </div>
      <span class="account-label">{{ a.name }}</span>
      <h2>{{ money(a.balance, a.currency, 2) }}</h2>
      <span class="account-balance-label">账面余额</span>
      <div class="account-decoration" :class="'decoration-' + i"></div>
      <div class="account-bottom">
        <span>
          {{ a.institution }}
          <small>{{ a.active ? "账户使用中" : "已停用" }}</small>
        </span>
        <button
          v-if="canWrite"
          class="account-toggle"
          :disabled="working"
          @click="toggleAccount(a)"
        >
          {{ a.active ? "停用" : "启用" }}
        </button>
      </div>
    </article>
    <button v-if="canWrite" class="add-account" @click="openAccount">
      <span><Plus :size="25" /></span>
      <strong>添加资金账户</strong>
      <small>让所有资金汇聚于此</small>
    </button>
  </div>
  <div class="info-callout">
    <Info :size="18" />
    <p>余额 = 期初余额 + 已入账收入 − 已入账支出。不同币种独立展示；停用账户保留历史交易。</p>
  </div>
</template>
