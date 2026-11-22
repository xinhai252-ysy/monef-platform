<script setup lang="ts">
import { useWorkspace } from "../composables/workspaceContext";
import { Sparkles, ShieldCheck, RefreshCw, Receipt } from "lucide-vue-next";

const { auditPage, auditData, loadAudit } = useWorkspace();
</script>
<template>
  <div class="system-cards">
    <div class="panel">
      <ShieldCheck :size="24" />
      <h3>企业隔离</h3>
      <p>租户由登录身份决定，所有账本查询均在当前企业范围内。</p>
    </div>
    <div class="panel">
      <Receipt :size="24" />
      <h3>流水可追溯</h3>
      <p>交易仅允许作废，保留原始金额、汇率快照和操作记录。</p>
    </div>
    <div class="panel">
      <Sparkles :size="24" />
      <h3>受限 AI 工具</h3>
      <p>只读查询，固定企业与月份，不执行 SQL 或资金操作。</p>
    </div>
  </div>
  <section class="panel">
    <div class="panel-heading">
      <div>
        <span class="eyebrow">ACTIVITY LOG</span>
        <h2>操作审计</h2>
      </div>
      <button class="icon-button" aria-label="刷新审计" @click="loadAudit">
        <RefreshCw :size="16" />
      </button>
    </div>
    <div class="table-scroll">
      <table>
        <thead>
          <tr>
            <th>时间</th>
            <th>操作人</th>
            <th>事件</th>
            <th>详情</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in auditData?.content" :key="a.id">
            <td>{{ new Date(a.createdAt).toLocaleString("zh-CN") }}</td>
            <td>{{ a.actor }}</td>
            <td>{{ a.action }}</td>
            <td>{{ a.detail }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p v-if="!auditData?.content.length" class="empty">
      首次创建账户、记账或分析后，这里会显示操作记录。
    </p>
    <div class="table-footer">
      <span>共 {{ auditData?.totalElements ?? 0 }} 条记录</span>
      <div class="pagination">
        <button
          :disabled="auditPage === 0"
          @click="
            auditPage--;
            loadAudit();
          "
        >
          上一页
        </button>
        <span>{{ auditPage + 1 }} / {{ Math.max(1, auditData?.totalPages ?? 1) }}</span>
        <button
          :disabled="auditPage + 1 >= (auditData?.totalPages ?? 0)"
          @click="
            auditPage++;
            loadAudit();
          "
        >
          下一页
        </button>
      </div>
    </div>
  </section>
</template>
