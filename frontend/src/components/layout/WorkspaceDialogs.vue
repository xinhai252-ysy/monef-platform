<script setup lang="ts">
import { useWorkspace } from "../../composables/workspaceContext";
import { ArrowUpRight, ArrowDownLeft, X, Check, ExternalLink } from "lucide-vue-next";
import { money, today } from "../../format";
import { previewMode } from "../../api";

const {
  user,
  error,
  working,
  kind,
  status,
  selected,
  modal,
  dialog,
  modalError,
  form,
  accountForm,
  categories,
  canWrite,
  activeAccounts,
  closeModal,
  saveEntry,
  saveAccount,
  voidEntry,
} = useWorkspace();
</script>
<template>
  <dialog
    ref="dialog"
    @cancel.prevent="closeModal"
    @click="
      (event) => {
        if (event.target === dialog) closeModal();
      }
    "
  >
    <div class="modal-inner">
      <button
        class="modal-close icon-button"
        aria-label="关闭弹窗"
        :disabled="working"
        @click="closeModal"
      >
        <X :size="20" />
      </button>
      <template v-if="modal === 'entry'">
        <span class="eyebrow">NEW TRANSACTION</span>
        <h2>记录一笔交易</h2>
        <p class="modal-description">金额使用所选账户币种，入账时保存汇率快照。</p>
        <form @submit.prevent="saveEntry">
          <div class="form-segments">
            <button
              type="button"
              :class="{ active: form.kind === 'EXPENSE' }"
              @click="form.kind = 'EXPENSE'"
            >
              <ArrowUpRight :size="17" />
              支出
            </button>
            <button
              type="button"
              :class="{ active: form.kind === 'INCOME' }"
              @click="form.kind = 'INCOME'"
            >
              <ArrowDownLeft :size="17" />
              收入
            </button>
          </div>
          <label>
            交易描述
            <input
              v-model="form.description"
              required
              maxlength="200"
              placeholder="例如：本月软件订阅"
              autofocus
            />
          </label>
          <div class="form-grid">
            <label>
              资金账户
              <select v-model="form.accountId" required>
                <option value="" disabled>请选择账户</option>
                <option v-for="a in activeAccounts" :key="a.id" :value="a.id">
                  {{ a.name }} · {{ a.currency }}
                </option>
              </select>
            </label>
            <label>
              金额
              <input
                v-model="form.amount"
                required
                type="number"
                min="0.01"
                max="9999999999999.99"
                step="0.01"
                placeholder="0.00"
              />
            </label>
            <label>
              交易分类
              <select v-model="form.category" required>
                <option v-for="c in categories" :key="c">{{ c }}</option>
              </select>
            </label>
            <label>
              记账日期
              <input v-model="form.bookedOn" type="date" min="2000-01-01" :max="today()" required />
            </label>
          </div>
          <p v-if="!activeAccounts.length" class="inline-error">请先创建或启用资金账户。</p>
          <p v-if="modalError" class="inline-error" role="alert">
            {{ modalError }}
          </p>
          <div class="modal-actions">
            <button class="secondary" type="button" :disabled="working" @click="closeModal">
              取消
            </button>
            <button class="primary" :disabled="working || !activeAccounts.length">
              {{ working ? "正在保存…" : "确认入账" }}
              <Check :size="16" />
            </button>
          </div>
        </form>
      </template>
      <template v-if="modal === 'account'">
        <span class="eyebrow">NEW ACCOUNT</span>
        <h2>添加资金账户</h2>
        <p class="modal-description">建立账户档案，记录期初余额与后续收支。</p>
        <form @submit.prevent="saveAccount">
          <label>
            账户名称
            <input
              v-model="accountForm.name"
              required
              maxlength="100"
              placeholder="例如：企业运营账户"
              autofocus
            />
          </label>
          <label>
            所属机构
            <input
              v-model="accountForm.institution"
              required
              maxlength="100"
              placeholder="例如：招商银行 ···· 3826"
            />
          </label>
          <div class="form-grid">
            <label>
              币种
              <select v-model="accountForm.currency">
                <option
                  v-for="c in [
                    'USD',
                    'EUR',
                    'GBP',
                    'CNY',
                    'JPY',
                    'SGD',
                    'HKD',
                    'AUD',
                    'CAD',
                    'CHF',
                  ]"
                  :key="c"
                >
                  {{ c }}
                </option>
              </select>
            </label>
            <label>
              账户类型
              <select v-model="accountForm.type">
                <option value="BANK">银行账户</option>
                <option value="WALLET">电子钱包</option>
                <option value="CASH">现金账户</option>
              </select>
            </label>
          </div>
          <label>
            期初余额
            <input
              v-model="accountForm.openingBalance"
              type="number"
              min="0"
              max="9999999999999.99"
              step="0.01"
              required
            />
          </label>
          <p v-if="modalError" class="inline-error" role="alert">
            {{ modalError }}
          </p>
          <div class="modal-actions">
            <button class="secondary" type="button" :disabled="working" @click="closeModal">
              取消
            </button>
            <button class="primary" :disabled="working">
              {{ working ? "正在保存…" : "创建账户" }}
            </button>
          </div>
        </form>
      </template>
      <template v-if="modal === 'detail' && selected">
        <span class="eyebrow">TRANSACTION DETAILS</span>
        <h2>{{ selected.description }}</h2>
        <div class="detail-amount" :class="{ income: selected.kind === 'INCOME' }">
          {{ selected.kind === "INCOME" ? "+" : "−"
          }}{{ money(selected.amount, selected.currency, 2) }}
          <small>{{ selected.currency }}</small>
        </div>
        <dl class="detail-list">
          <div>
            <dt>资金账户</dt>
            <dd>{{ selected.accountName }}</dd>
          </div>
          <div>
            <dt>分类 / 状态</dt>
            <dd>
              {{ selected.category }} ·
              {{ selected.status === "POSTED" ? "已入账" : "已作废" }}
            </dd>
          </div>
          <div>
            <dt>记账日期</dt>
            <dd>{{ selected.bookedOn }}</dd>
          </div>
          <div>
            <dt>USD 折算金额</dt>
            <dd>{{ money(selected.baseAmount, "USD", 2) }}</dd>
          </div>
          <div>
            <dt>入账汇率</dt>
            <dd>1 {{ selected.currency }} = {{ Number(selected.fxRate).toFixed(6) }} USD</dd>
          </div>
          <div>
            <dt>汇率日期 / 来源</dt>
            <dd>{{ selected.fxDate }} · {{ selected.fxSource }}</dd>
          </div>
          <div>
            <dt>交易编号</dt>
            <dd class="mono">{{ selected.id }}</dd>
          </div>
        </dl>
        <div class="modal-actions">
          <button class="secondary" @click="closeModal">关闭</button>
          <button
            v-if="canWrite && selected.status === 'POSTED'"
            class="danger-button"
            @click="modal = 'void'"
          >
            作废交易
          </button>
        </div>
      </template>
      <template v-if="modal === 'void' && selected">
        <span class="eyebrow">VOID TRANSACTION</span>
        <h2>作废这笔交易？</h2>
        <p class="modal-description">
          “{{ selected.description }}”将不再计入账户余额及财务统计。原始流水和审计记录仍会保留。
        </p>
        <p v-if="modalError" class="inline-error" role="alert">
          {{ modalError }}
        </p>
        <div class="modal-actions">
          <button class="secondary" :disabled="working" @click="modal = 'detail'">返回</button>
          <button class="danger-button" :disabled="working" @click="voidEntry">
            {{ working ? "处理中…" : "确认作废" }}
          </button>
        </div>
      </template>
      <template v-if="modal === 'workspace'">
        <span class="eyebrow">YOUR WORKSPACE</span>
        <h2>{{ user?.demo ? "Acme Studio" : "企业工作区" }}</h2>
        <dl class="detail-list">
          <div>
            <dt>登录账户</dt>
            <dd>{{ user?.email }}</dd>
          </div>
          <div>
            <dt>角色</dt>
            <dd>{{ canWrite ? "管理员" : "分析员" }}</dd>
          </div>
          <div>
            <dt>企业编号</dt>
            <dd>{{ user?.tenantId }}</dd>
          </div>
          <div>
            <dt>当前模式</dt>
            <dd>
              {{
                previewMode ? "浏览器交互演示" : user?.demo ? "Java 后端演示环境" : "企业运行环境"
              }}
            </dd>
          </div>
        </dl>
        <p class="modal-description">
          {{
            previewMode
              ? "数据只保存在当前浏览器，不代表真实企业的财务情况。"
              : "企业身份由服务端登录会话确定，不能通过客户端切换。"
          }}
        </p>
        <button class="secondary full" @click="closeModal">知道了</button>
      </template>
      <template v-if="modal === 'help'">
        <span class="eyebrow">ABOUT YOUR NUMBERS</span>
        <h2>每个数字，都有依据</h2>
        <div class="help-copy">
          <h3>统一统计口径</h3>
          <p>仪表盘使用 USD，按每笔交易入账时的汇率快照汇总。账户余额保留原币种。</p>
          <h3>净现金流与结余率</h3>
          <p>
            净现金流 = 收入 − 支出；结余率 = 净现金流 /
            收入。此处为收付口径，不等同于权责发生制净利润。收入为零时，结余率为空。
          </p>
          <h3>环比与日期</h3>
          <p>
            环比与上一自然月比较；上月为零时不计算百分比。本月尚未结束时，比较的是本月至今与完整上月。日期以
            UTC 为准。
          </p>
          <h3>汇率与 AI</h3>
          <p>
            演示模式使用明确标注的固定汇率。正式环境接入 Frankfurter
            日参考汇率，并保存来源、日期和缓存状态。AI 仅可查询当前企业所选月份的数据。
          </p>
        </div>
        <a
          class="text-button reference-link"
          href="https://dribbble.com/shots/27389006-UI-elements-for-Monef-Corporate-Finance-Dashboard"
          target="_blank"
          rel="noopener noreferrer"
        >
          设计参考 · One Collective Design
          <ExternalLink :size="14" />
        </a>
      </template>
    </div>
  </dialog>
</template>
