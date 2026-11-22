import { useDashboardMetrics } from "./useDashboardMetrics";
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from "vue";
import { LayoutGrid, ArrowLeftRight, Wallet, ArrowDownUp, Sparkles } from "lucide-vue-next";
import { api } from "../api";
import { currentMonth, today, monthBounds, csvCell } from "../format";
import type { Account, AccountInput, Dashboard, Entry, EntryInput, Page } from "../types";
import { useSession } from "./useSession";

export function useFinanceWorkspace() {
  const {
    user,
    initializing,
    loginEmail,
    loginPassword,
    loginBusy,
    loginError,
    login,
    register,
    logout,
  } = useSession({
    onAuthenticated: load,
    onSignedOut: clearWorkspace,
    onError: notify,
  });
  const devMode = import.meta.env.DEV;

  const nav = [
    { id: "dashboard", label: "财务总览", icon: LayoutGrid },
    { id: "transactions", label: "交易流水", icon: ArrowLeftRight },
    { id: "accounts", label: "资金账户", icon: Wallet },
    { id: "exchange", label: "汇率中心", icon: ArrowDownUp },
    { id: "assistant", label: "AI 财务助手", icon: Sparkles },
  ];

  const route = ref("dashboard");
  const mobileNav = ref(false);
  const loading = ref(false);
  const error = ref("");
  const toast = ref("");
  const working = ref(false);

  const month = ref(currentMonth());
  const dashboard = ref<Dashboard | null>(null);
  const accounts = ref<Account[]>([]);
  const entries = ref<Page<Entry> | null>(null);

  const page = ref(0);
  const search = ref("");
  const kind = ref("");
  const accountFilter = ref("");
  const status = ref("");
  const filtersOpen = ref(false);
  const refreshKey = ref(0);

  const selected = ref<Entry | null>(null);
  const modal = ref("");
  const dialog = ref<HTMLDialogElement | null>(null);
  const modalError = ref("");

  const form = reactive<EntryInput>({
    accountId: "",
    kind: "EXPENSE",
    amount: "",
    description: "",
    category: "软件订阅",
    bookedOn: today(),
    clientRef: crypto.randomUUID(),
  });

  const accountForm = reactive<AccountInput>({
    name: "",
    institution: "",
    currency: "USD",
    type: "BANK",
    openingBalance: "0.00",
  });

  const auditPage = ref(0);
  const auditData = ref<Awaited<ReturnType<typeof api.audit>> | null>(null);

  const { metrics, totalExpenses, spark } = useDashboardMetrics(dashboard);

  const categories = computed(() =>
    form.kind === "INCOME"
      ? ["产品收入", "服务收入", "其他收入"]
      : ["人力成本", "市场营销", "研发投入", "软件订阅", "办公运营", "其他支出"],
  );

  const canWrite = computed(() => user.value?.role === "ADMIN");

  const title = computed(() => nav.find((n) => n.id === route.value)?.label ?? "系统与审计");

  const activeAccounts = computed(() => accounts.value.filter((a) => a.active));

  const shownEntries = computed(() =>
    route.value === "dashboard" ? entries.value?.content.slice(0, 5) : entries.value?.content,
  );

  let toastTimer: ReturnType<typeof setTimeout>;

  function notify(message: string) {
    toast.value = message;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => (toast.value = ""), 4500);
  }

  function changeRoute(id: string) {
    route.value = id;
    location.hash = id;
    mobileNav.value = false;
  }

  function hashChange() {
    const id = location.hash.slice(1);
    route.value = nav.some((n) => n.id === id) || id === "audit" ? id : "dashboard";
  }

  let loadId = 0;

  async function load() {
    if (!user.value) return;
    const id = ++loadId;
    loading.value = true;
    error.value = "";
    try {
      const [d, a, e] = await Promise.all([
        api.dashboard(month.value),
        api.accounts(),
        api.entries({
          ...monthBounds(month.value),
          q: search.value || undefined,
          kind: kind.value || undefined,
          status: status.value || undefined,
          accountId: accountFilter.value || undefined,
          page: page.value,
          size: 10,
        }),
      ]);
      if (id === loadId) {
        dashboard.value = d;
        accounts.value = a;
        entries.value = e;
      }
      if (route.value === "audit") await loadAudit();
    } catch (e) {
      if (id === loadId) error.value = (e as Error).message;
    } finally {
      if (id === loadId) loading.value = false;
    }
  }

  async function loadAudit() {
    try {
      auditData.value = await api.audit(auditPage.value);
    } catch (e) {
      error.value = (e as Error).message;
    }
  }

  function clearWorkspace() {
    loadId++;
    dashboard.value = null;
    accounts.value = [];
    entries.value = null;
    auditData.value = null;
    selected.value = null;
    modal.value = "";
    error.value = "";
    loading.value = false;
  }

  watch([month, kind, status, accountFilter], () => {
    page.value = 0;
    void load();
  });

  watch(page, load);

  watch(route, () => {
    page.value = 0;
    search.value = "";
    kind.value = "";
    accountFilter.value = "";
    status.value = "";
    void load();
  });

  let searchTimer: ReturnType<typeof setTimeout>;

  watch(search, () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
      page.value = 0;
      void load();
    }, 300);
  });

  watch(
    () => form.kind,
    () => (form.category = categories.value[0]),
  );

  watch(modal, async (value) => {
    await nextTick();
    if (value) {
      dialog.value?.showModal();
    } else {
      dialog.value?.close();
    }
  });

  function closeModal() {
    if (!working.value) {
      modal.value = "";
      modalError.value = "";
    }
  }

  function openEntry() {
    Object.assign(form, {
      accountId: activeAccounts.value[0]?.id ?? "",
      kind: "EXPENSE",
      amount: "",
      description: "",
      category: "软件订阅",
      bookedOn: today(),
      clientRef: crypto.randomUUID(),
    });
    modalError.value = "";
    modal.value = "entry";
  }

  function openAccount() {
    Object.assign(accountForm, {
      name: "",
      institution: "",
      currency: "USD",
      type: "BANK",
      openingBalance: "0.00",
    });
    modalError.value = "";
    modal.value = "account";
  }

  function viewEntry(entry: Entry) {
    selected.value = entry;
    modal.value = "detail";
  }

  async function saveEntry() {
    working.value = true;
    modalError.value = "";
    try {
      await api.createEntry({ ...form });
      modal.value = "";
      notify("交易已入账，统计数据已更新");
      refreshKey.value++;
      await load();
    } catch (e) {
      modalError.value = (e as Error).message;
    } finally {
      working.value = false;
    }
  }

  async function saveAccount() {
    working.value = true;
    modalError.value = "";
    try {
      await api.createAccount({ ...accountForm });
      modal.value = "";
      notify("账户已创建");
      await load();
    } catch (e) {
      modalError.value = (e as Error).message;
    } finally {
      working.value = false;
    }
  }

  async function voidEntry() {
    if (!selected.value) return;
    working.value = true;
    modalError.value = "";
    try {
      await api.voidEntry(selected.value.id);
      modal.value = "";
      notify("交易已作废，审计记录已保留");
      refreshKey.value++;
      await load();
    } catch (e) {
      modalError.value = (e as Error).message;
    } finally {
      working.value = false;
    }
  }

  async function toggleAccount(a: Account) {
    working.value = true;
    try {
      await api.accountStatus(a.id, !a.active);
      await load();
      notify(a.active ? "账户已停用" : "账户已启用");
    } catch (e) {
      notify((e as Error).message);
    } finally {
      working.value = false;
    }
  }

  async function exportCsv() {
    working.value = true;
    try {
      const all: Entry[] = [];
      let index = 0;
      let total = 1;
      while (index < total) {
        const result = await api.entries({
          ...monthBounds(month.value),
          q: search.value || undefined,
          kind: kind.value || undefined,
          status: status.value || undefined,
          accountId: accountFilter.value || undefined,
          page: index,
          size: 100,
        });
        total = result.totalPages;
        if (total > 100) throw new Error("导出最多支持 10,000 笔，请缩小筛选范围");
        all.push(...result.content);
        index++;
      }
      const csv = [
        [
          "交易编号",
          "日期",
          "描述",
          "账户",
          "类型",
          "分类",
          "原币金额",
          "币种",
          "USD折算",
          "入账汇率",
          "汇率日期",
          "汇率来源",
          "状态",
        ],
        ...all.map((e) => [
          e.id,
          e.bookedOn,
          e.description,
          e.accountName,
          e.kind,
          e.category,
          e.amount,
          e.currency,
          e.baseAmount,
          e.fxRate,
          e.fxDate,
          e.fxSource,
          e.status,
        ]),
      ]
        .map((row) => row.map(csvCell).join(","))
        .join("\r\n");
      const url = URL.createObjectURL(
        new Blob(["\uFEFF" + csv], { type: "text/csv;charset=utf-8" }),
      );
      const a = document.createElement("a");
      a.href = url;
      a.download = `monef-transactions-${month.value}.csv`;
      a.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
      notify(`已导出 ${all.length} 笔交易`);
    } catch (e) {
      notify((e as Error).message);
    } finally {
      working.value = false;
    }
  }

  onMounted(() => {
    hashChange();
    window.addEventListener("hashchange", hashChange);
  });

  onUnmounted(() => {
    window.removeEventListener("hashchange", hashChange);
    clearTimeout(toastTimer);
    clearTimeout(searchTimer);
  });
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
    devMode,
    nav,
    route,
    mobileNav,
    loading,
    error,
    toast,
    working,
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
    refreshKey,
    selected,
    modal,
    dialog,
    modalError,
    form,
    accountForm,
    auditPage,
    auditData,
    categories,
    canWrite,
    title,
    activeAccounts,
    totalExpenses,
    shownEntries,
    metrics,
    spark,
    notify,
    changeRoute,
    load,
    loadAudit,
    closeModal,
    openEntry,
    openAccount,
    viewEntry,
    saveEntry,
    saveAccount,
    voidEntry,
    toggleAccount,
    exportCsv,
  };
}
