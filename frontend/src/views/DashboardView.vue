<script setup lang="ts">
import { useWorkspace } from "../composables/workspaceContext";
import { ArrowUpRight, ArrowUp, ArrowDown, BadgeCheck } from "lucide-vue-next";
import { money, percent } from "../format";

import TrendChart from "../components/TrendChart.vue";
import ExchangeCard from "../components/ExchangeCard.vue";
import AiPanel from "../components/AiPanel.vue";
const { month, dashboard, categories, title, totalExpenses, metrics, spark, changeRoute } =
  useWorkspace();
</script>
<template>
  <template v-if="dashboard">
    <section class="kpi-grid" aria-label="本月财务指标">
      <article
        v-for="metric in metrics"
        :key="metric.label"
        class="kpi-card"
        :class="{ highlight: metric.highlight }"
      >
        <div class="kpi-top">
          <span class="kpi-icon"><component :is="metric.icon" :size="17" /></span>
          <span>{{ metric.label }}</span>
          <ArrowUpRight :size="14" class="muted" />
        </div>
        <div class="kpi-value">{{ metric.value }}</div>
        <div class="kpi-bottom">
          <div v-if="metric.change !== null">
            <span class="change-badge" :class="{ negative: !metric.good }">
              <ArrowUp v-if="Number(metric.change) >= 0" :size="11" />
              <ArrowDown v-else :size="11" />
              {{ percent(metric.change) }}
            </span>
            <small>较上月</small>
          </div>
          <small v-else>
            {{ metric.label === "收支结余率" ? "（收入 − 支出）/ 收入" : "上月无可比数据" }}
          </small>
          <svg v-if="metric.series.length" class="sparkline" viewBox="0 0 90 32" aria-hidden="true">
            <path
              :d="spark(metric.series)"
              fill="none"
              :stroke="metric.highlight ? '#cec3ff' : '#85818f'"
              stroke-width="1.8"
              stroke-linecap="round"
            />
          </svg>
          <span v-else class="kpi-health"><BadgeCheck :size="19" /></span>
        </div>
      </article>
    </section>
    <div class="dashboard-grid">
      <div class="dashboard-main">
        <TrendChart :data="dashboard" />
        <section class="panel expense-panel">
          <div class="panel-heading">
            <div>
              <span class="eyebrow">WHERE YOUR MONEY GOES</span>
              <h2>支出构成</h2>
            </div>
            <span class="panel-total">
              {{ money(dashboard.expense.value) }}
              <small>本月总支出</small>
            </span>
          </div>
          <div class="expense-visual" v-if="dashboard.categories.length">
            <div
              v-for="(c, i) in dashboard.categories"
              :key="c.category"
              :style="{
                flex: Number(c.amount),
                background: ['#8a76f5', '#5f53a8', '#41414b', '#a6f2b0', '#76737f', '#55545e'][
                  i % 6
                ],
              }"
              :title="`${c.category}: ${money(c.amount)}`"
            ></div>
          </div>
          <div class="category-grid">
            <div v-for="(c, i) in dashboard.categories" :key="c.category" class="category-item">
              <i
                class="dot"
                :style="{
                  background: ['#8a76f5', '#5f53a8', '#41414b', '#a6f2b0', '#76737f', '#55545e'][
                    i % 6
                  ],
                }"
              ></i>
              <span>
                {{ c.category }}
                <small>{{ c.count }} 笔交易</small>
              </span>
              <strong>
                {{ money(c.amount) }}
                <small>
                  {{ totalExpenses ? ((Number(c.amount) / totalExpenses) * 100).toFixed(1) : 0 }}%
                </small>
              </strong>
            </div>
          </div>
          <p v-if="!dashboard.categories.length" class="empty">本月暂无已入账支出。</p>
        </section>
      </div>
      <div class="dashboard-side">
        <ExchangeCard />
        <AiPanel :month="month" @open="changeRoute('assistant')" />
      </div>
    </div>
  </template>
</template>
