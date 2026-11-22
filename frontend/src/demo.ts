import Decimal from "decimal.js";
import type { Account, Dashboard, Entry, Page, Rate, Money } from "./types";
import { currentMonth, today } from "./format";
const key = "monef-preview-v1";
const unit: Record<string, string> = {
  USD: "1",
  EUR: "0.92",
  GBP: "0.77",
  CNY: "7.12",
  JPY: "150",
  SGD: "1.30",
  HKD: "7.78",
  AUD: "1.49",
  CAD: "1.36",
  CHF: "0.87",
};
const D = (n: Money) => new Decimal(n);
const sum = (list: Entry[], kind: string) =>
  list.filter((e) => e.kind === kind).reduce((s, e) => s.plus(e.baseAmount), D(0));
interface State {
  accounts: Account[];
  entries: Entry[];
  history: Rate[];
  refs: Record<string, { body: string; id: string }>;
  audit: {
    id: string;
    actor: string;
    action: string;
    subjectId: string;
    detail: string;
    createdAt: string;
  }[];
}
function seed(): State {
  const accounts: Account[] = [
    {
      id: "account-usd",
      name: "企业主账户",
      institution: "Mercury ···· 4829",
      currency: "USD",
      type: "BANK",
      openingBalance: "85000",
      balance: "85000",
      active: true,
    },
    {
      id: "account-eur",
      name: "欧洲业务账户",
      institution: "Wise ···· 7291",
      currency: "EUR",
      type: "WALLET",
      openingBalance: "28000",
      balance: "28000",
      active: true,
    },
    {
      id: "account-cny",
      name: "人民币账户",
      institution: "招商银行 ···· 9066",
      currency: "CNY",
      type: "BANK",
      openingBalance: "160000",
      balance: "160000",
      active: true,
    },
  ];
  const entries: Entry[] = [];
  const month = currentMonth();
  const [y, m] = month.split("-").map(Number);
  const names = [
    "Linear · 年度产品订阅",
    "Acme Studio · 项目服务",
    "Notion Labs · 企业授权",
    "Vercel · 技术咨询",
    "Figma · 团队方案",
  ];
  const cats = ["人力成本", "市场营销", "研发投入", "软件订阅", "办公运营", "其他支出"];
  const expenses = [
    "团队薪资",
    "品牌推广计划",
    "研发设备采购",
    "软件与云服务",
    "办公室租金",
    "差旅及行政",
  ];
  for (let back = 11; back >= 0; back--) {
    const date = new Date(Date.UTC(y, m - 1 - back, 1));
    const ym = date.toISOString().slice(0, 7);
    const max =
      back === 0
        ? Number(today().slice(-2))
        : new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth() + 1, 0)).getUTCDate();
    for (let i = 0; i < 20; i++) {
      const income = i % 3 === 0;
      const a = accounts[i % 8 === 0 ? 1 : i % 11 === 0 ? 2 : 0];
      const base = D(
        income
          ? 19000 + ((i * 3137) % 15000) + (12 - back) * 900
          : 2300 + ((i * 1711) % 4500) + (12 - back) * 120,
      );
      const rate = D(1).div(unit[a.currency]);
      const amount = base.div(rate).toFixed(2);
      const day = `${ym}-${String(1 + ((i * 7) % max)).padStart(2, "0")}`;
      entries.push({
        id: `seed-${ym}-${i}`,
        accountId: a.id,
        accountName: a.name,
        kind: income ? "INCOME" : "EXPENSE",
        amount,
        currency: a.currency,
        baseAmount: D(amount).mul(rate).toFixed(2),
        fxRate: rate.toFixed(10),
        fxDate: day,
        fxSource: "DEMO",
        description: income ? names[i % 5] : expenses[i % 6],
        category: income ? (i % 2 === 0 ? "产品收入" : "服务收入") : cats[i % 6],
        bookedOn: day,
        status: "POSTED",
      });
    }
  }
  return { accounts, entries, history: [], refs: {}, audit: [] };
}
let state: State;
try {
  const saved = localStorage.getItem(key);
  state = saved ? JSON.parse(saved) : seed();
} catch {
  state = seed();
}
function save() {
  try {
    localStorage.setItem(key, JSON.stringify(state));
  } catch {
    throw new Error("浏览器存储空间不足，无法保存演示数据");
  }
}
function page<T>(content: T[], p = 0, size = 10): Page<T> {
  return {
    content: content.slice(p * size, p * size + size),
    page: p,
    size,
    totalElements: content.length,
    totalPages: Math.ceil(content.length / size),
  };
}
function rate(base: string, quote: string, date = today()): Rate {
  if (!unit[base] || !unit[quote]) throw new Error("不支持该币种");
  const cached = state.history.find((r) => r.base === base && r.quote === quote && r.date === date);
  if (cached) return { ...cached, cached: true };
  const r = {
    base,
    quote,
    rate: D(unit[quote]).div(unit[base]).toFixed(10),
    date,
    source: "DEMO",
    cached: false,
    fetchedAt: new Date().toISOString(),
  };
  state.history.unshift(r);
  save();
  return r;
}
function dashboard(month: string): Dashboard {
  const selected = state.entries.filter(
    (e) => e.status === "POSTED" && e.bookedOn.startsWith(month),
  );
  const [y, m] = month.split("-").map(Number);
  const prev = new Date(Date.UTC(y, m - 2, 1)).toISOString().slice(0, 7);
  const last = state.entries.filter((e) => e.status === "POSTED" && e.bookedOn.startsWith(prev));
  const income = sum(selected, "INCOME"),
    expense = sum(selected, "EXPENSE"),
    priorIncome = sum(last, "INCOME"),
    priorExpense = sum(last, "EXPENSE");
  const metric = (a: Decimal, b: Decimal) => ({
    value: a.toFixed(2),
    change: b.isZero() ? null : a.minus(b).div(b.abs()).mul(100).toFixed(2),
  });
  const cats = new Map<string, { amount: Decimal; count: number }>();
  selected
    .filter((e) => e.kind === "EXPENSE")
    .forEach((e) => {
      const c = cats.get(e.category) ?? { amount: D(0), count: 0 };
      cats.set(e.category, {
        amount: c.amount.plus(e.baseAmount),
        count: c.count + 1,
      });
    });
  return {
    month,
    baseCurrency: "USD",
    income: metric(income, priorIncome),
    expense: metric(expense, priorExpense),
    netCashFlow: metric(income.minus(expense), priorIncome.minus(priorExpense)),
    surplusMargin: income.isZero() ? null : income.minus(expense).div(income).mul(100).toFixed(2),
    transactionCount: selected.length,
    series: Array.from({ length: 12 }, (_, i) => {
      const ym = new Date(Date.UTC(y, m - 12 + i, 1)).toISOString().slice(0, 7);
      const list = state.entries.filter((e) => e.status === "POSTED" && e.bookedOn.startsWith(ym));
      return {
        month: ym,
        income: sum(list, "INCOME").toFixed(2),
        expense: sum(list, "EXPENSE").toFixed(2),
      };
    }),
    categories: [...cats.entries()]
      .map(([category, v]) => ({
        category,
        amount: v.amount.toFixed(2),
        count: v.count,
      }))
      .sort((a, b) => D(b.amount).cmp(a.amount)),
    generatedAt: new Date().toISOString(),
  };
}
function audit(action: string, id: string) {
  state.audit.unshift({
    id: crypto.randomUUID(),
    actor: "demo@monef.local",
    action,
    subjectId: id,
    detail: "浏览器交互演示",
    createdAt: new Date().toISOString(),
  });
}
export async function demoRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const url = new URL(path, "https://preview.invalid"),
    p = url.pathname,
    s = url.searchParams;
  const body = options.body ? JSON.parse(String(options.body)) : {};
  const method = options.method ?? "GET";
  let result: unknown;
  if (p === "/auth/me" || p === "/auth/login")
    result = {
      email: "demo@monef.local",
      name: "Alex Morgan",
      role: "ADMIN",
      tenantId: "demo",
      demo: true,
    };
  else if (p === "/auth/logout") return undefined as T;
  else if (p === "/dashboard") result = dashboard(s.get("month") ?? currentMonth());
  else if (p === "/accounts" && method === "GET")
    result = state.accounts.map((a) => ({
      ...a,
      balance: state.entries
        .filter((e) => e.accountId === a.id && e.status === "POSTED")
        .reduce(
          (balance, e) => balance.plus(e.kind === "INCOME" ? D(e.amount) : D(e.amount).negated()),
          D(a.openingBalance),
        )
        .toFixed(2),
    }));
  else if (p === "/accounts" && method === "POST") {
    if (!unit[body.currency] || D(body.openingBalance).isNegative())
      throw new Error("账户信息无效");
    const account = {
      ...body,
      id: crypto.randomUUID(),
      balance: body.openingBalance,
      active: true,
    };
    state.accounts.push(account);
    audit("ACCOUNT_CREATED", account.id);
    save();
    result = account;
  } else if (p.endsWith("/status")) {
    const a = state.accounts.find((a) => a.id === p.split("/")[2]);
    if (!a) throw new Error("账户不存在");
    a.active = body.active;
    audit("ACCOUNT_STATUS", a.id);
    save();
    result = a;
  } else if (p === "/transactions" && method === "GET") {
    const filtered = state.entries
      .filter(
        (e) =>
          (!s.get("q") || e.description.toLowerCase().includes(s.get("q")!.toLowerCase())) &&
          (!s.get("kind") || e.kind === s.get("kind")) &&
          (!s.get("status") || e.status === s.get("status")) &&
          (!s.get("accountId") || e.accountId === s.get("accountId")) &&
          (!s.get("start") || e.bookedOn >= s.get("start")!) &&
          (!s.get("end") || e.bookedOn <= s.get("end")!),
      )
      .sort((a, b) => b.bookedOn.localeCompare(a.bookedOn) || b.id.localeCompare(a.id));
    result = page(filtered, Number(s.get("page") ?? 0), Number(s.get("size") ?? 10));
  } else if (p === "/transactions" && method === "POST") {
    const ref = state.refs[body.clientRef];
    if (ref) {
      if (ref.body !== JSON.stringify(body)) throw new Error("重复请求编号冲突");
      return state.entries.find((e) => e.id === ref.id) as T;
    }
    const a = state.accounts.find((a) => a.id === body.accountId);
    if (!a?.active) throw new Error("请选择有效账户");
    if (D(body.amount).lte(0) || D(body.amount).decimalPlaces() > 2 || body.bookedOn > today())
      throw new Error("请检查金额与日期");
    const fx = rate(a.currency, "USD", body.bookedOn);
    const e: Entry = {
      ...body,
      id: crypto.randomUUID(),
      accountName: a.name,
      currency: a.currency,
      baseAmount: D(body.amount).mul(fx.rate).toFixed(2),
      fxRate: fx.rate,
      fxDate: fx.date,
      fxSource: fx.source,
      status: "POSTED",
    };
    state.entries.unshift(e);
    state.refs[body.clientRef] = { body: JSON.stringify(body), id: e.id };
    audit("TRANSACTION_CREATED", e.id);
    save();
    result = e;
  } else if (p.endsWith("/void")) {
    const e = state.entries.find((e) => e.id === p.split("/")[2]);
    if (!e) throw new Error("交易不存在");
    e.status = "VOID";
    audit("TRANSACTION_VOIDED", e.id);
    save();
    result = e;
  } else if (p === "/fx/convert") {
    const amount = s.get("amount") ?? "0";
    if (D(amount).isNegative() || D(amount).decimalPlaces() > 2) throw new Error("请输入有效金额");
    const observation = rate(s.get("base")!, s.get("quote")!, s.get("date") ?? today());
    result = {
      base: observation.base,
      quote: observation.quote,
      amount,
      converted: D(amount).mul(observation.rate).toFixed(2),
      observation,
    };
  } else if (p === "/fx/history")
    result = page(
      state.history.filter((r) => r.base === s.get("base") && r.quote === s.get("quote")),
      Number(s.get("page") ?? 0),
      20,
    );
  else if (p === "/ai/chat") {
    const d = dashboard(body.month);
    result = {
      answer: `【数据摘要 · 交互演示未连接 AI 模型】\n${body.month} 共 ${d.transactionCount} 笔有效交易。\n\n收入 $${d.income.value}\n支出 $${d.expense.value}\n净现金流 $${d.netCashFlow.value}\n\n${d.categories.length ? "主要支出为「" + d.categories[0].category + "」，金额 $" + d.categories[0].amount + "。" : "本月暂无支出。"}\n\n所有数字均来自当前演示账本，按入账汇率折算为 USD。完整后端配置 Spring AI 后可回答自定义问题。`,
      mode: "DATA_SUMMARY",
      month: body.month,
      currency: "USD",
      transactionCount: d.transactionCount,
      tools: ["monthlySummary"],
      generatedAt: new Date().toISOString(),
    };
  } else if (p === "/audit") result = page(state.audit, Number(s.get("page") ?? 0), 20);
  else throw new Error("当前演示不支持此操作");
  return structuredClone(result) as T;
}
