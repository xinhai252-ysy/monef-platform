<script setup lang="ts">
import { useWorkspace } from "../composables/workspaceContext";
import {
  ChevronRight,
  ArrowUpRight,
  ArrowDownLeft,
  Plus,
  Search,
  X,
  RefreshCw,
  Receipt,
  SlidersHorizontal,
} from "lucide-vue-next";
import { money } from "../format";

const {
  route,
  loading,
  month,
  dashboard,
  accounts,
  entries,
  page,
  search,
  kind,
  accountFilter,
  status,
  filtersOpen,
  selected,
  canWrite,
  shownEntries,
  changeRoute,
  load,
  openEntry,
  viewEntry,
} = useWorkspace();
</script>
<template>
  <section class="panel transactions-panel">
    <div class="panel-heading">
      <div>
        <span class="eyebrow">TRANSACTION ACTIVITY</span>
        <h2>
          {{ route === "dashboard" ? "最近交易" : "交易明细" }}
          <span class="count-badge">{{ entries?.totalElements ?? 0 }}</span>
        </h2>
      </div>
      <button v-if="route === 'dashboard'" class="text-button" @click="changeRoute('transactions')">
        查看全部
        <ArrowUpRight :size="15" />
      </button>
      <div v-else class="table-tools">
        <div class="search-field">
          <Search :size="16" />
          <input
            v-model="search"
            placeholder="搜索交易描述…"
            aria-label="搜索交易"
            maxlength="100"
          />
          <button v-if="search" aria-label="清空搜索" @click="search = ''">
            <X :size="14" />
          </button>
        </div>
        <button
          class="secondary"
          :class="{ selected: filtersOpen }"
          @click="filtersOpen = !filtersOpen"
        >
          <SlidersHorizontal :size="15" />
          筛选
        </button>
      </div>
    </div>
    <div v-if="route === 'transactions'" class="transaction-tabs">
      <button :class="{ active: kind === '' }" @click="kind = ''">全部交易</button>
      <button :class="{ active: kind === 'INCOME' }" @click="kind = 'INCOME'">收入</button>
      <button :class="{ active: kind === 'EXPENSE' }" @click="kind = 'EXPENSE'">支出</button>
      <span class="tab-end">{{ month }} · 原币金额</span>
    </div>
    <div v-if="filtersOpen && route === 'transactions'" class="filter-row">
      <label>
        账户
        <select v-model="accountFilter">
          <option value="">全部账户</option>
          <option v-for="a in accounts" :key="a.id" :value="a.id">
            {{ a.name }}
          </option>
        </select>
      </label>
      <label>
        状态
        <select v-model="status">
          <option value="">全部状态</option>
          <option value="POSTED">已入账</option>
          <option value="VOID">已作废</option>
        </select>
      </label>
      <button
        class="text-button"
        @click="
          accountFilter = '';
          status = '';
          search = '';
          kind = '';
        "
      >
        重置筛选
      </button>
    </div>
    <div class="table-scroll" :aria-busy="loading">
      <table class="transaction-table">
        <thead>
          <tr>
            <th>交易名称</th>
            <th>分类</th>
            <th>日期</th>
            <th>状态</th>
            <th class="align-right">金额</th>
            <th><span class="sr-only">详情</span></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="entry in shownEntries" :key="entry.id">
            <td>
              <button class="transaction-name" @click="viewEntry(entry)">
                <span
                  class="merchant-icon"
                  :class="entry.kind === 'INCOME' ? 'incoming' : 'outgoing'"
                >
                  <ArrowDownLeft v-if="entry.kind === 'INCOME'" :size="17" />
                  <ArrowUpRight v-else :size="17" />
                </span>
                <span>
                  <strong>{{ entry.description }}</strong>
                  <small>{{ entry.accountName }}</small>
                </span>
              </button>
            </td>
            <td>
              <span class="category-chip">{{ entry.category }}</span>
            </td>
            <td>{{ entry.bookedOn }}</td>
            <td>
              <span class="status-pill" :class="{ void: entry.status === 'VOID' }">
                <i></i>
                {{ entry.status === "POSTED" ? "已入账" : "已作废" }}
              </span>
            </td>
            <td class="align-right numeric" :class="{ income: entry.kind === 'INCOME' }">
              {{ entry.kind === "INCOME" ? "+" : "−" }}{{ money(entry.amount, entry.currency, 2) }}
              <small class="currency-label">{{ entry.currency }}</small>
            </td>
            <td>
              <button class="icon-button" aria-label="查看交易详情" @click="viewEntry(entry)">
                <ChevronRight :size="16" />
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-if="!shownEntries?.length" class="empty-state">
      <Receipt :size="30" />
      <h3>这里还没有交易</h3>
      <p>调整筛选条件，或记下第一笔收支。</p>
      <button v-if="canWrite" class="secondary" @click="openEntry">
        <Plus :size="15" />
        新增交易
      </button>
    </div>
    <div class="table-footer">
      <span>
        {{
          route === "dashboard"
            ? "最近 5 笔交易"
            : "共 " + (entries?.totalElements ?? 0) + " 笔交易"
        }}
        <span class="footer-dot">·</span>
        所有金额保留原币种
      </span>
      <div v-if="route === 'transactions'" class="pagination">
        <button :disabled="page === 0 || loading" @click="page--">上一页</button>
        <span>{{ page + 1 }} / {{ Math.max(1, entries?.totalPages ?? 1) }}</span>
        <button :disabled="page + 1 >= (entries?.totalPages ?? 0) || loading" @click="page++">
          下一页
        </button>
      </div>
      <button v-else class="icon-button" aria-label="刷新财务数据" @click="load">
        <RefreshCw :size="14" :class="{ spin: loading }" />
      </button>
    </div>
  </section>
</template>
