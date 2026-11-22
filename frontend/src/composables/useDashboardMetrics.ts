import { computed, type Ref } from "vue";
import { ArrowDownLeft, ArrowUpRight, ArrowLeftRight, ChartNoAxesCombined } from "lucide-vue-next";
import { money } from "../format";
import type { Dashboard } from "../types";
export function useDashboardMetrics(dashboard: Ref<Dashboard | null>) {
  const totalExpenses = computed(() => Number(dashboard.value?.expense.value ?? 0));
  const metrics = computed(() =>
    dashboard.value
      ? [
          {
            label: "本月收入",
            en: "TOTAL REVENUE",
            value: money(dashboard.value.income.value),
            change: dashboard.value.income.change,
            icon: ArrowDownLeft,
            highlight: true,
            series: dashboard.value.series.map((s) => Number(s.income)),
            good: Number(dashboard.value.income.change) >= 0,
          },
          {
            label: "本月支出",
            en: "TOTAL EXPENSES",
            value: money(dashboard.value.expense.value),
            change: dashboard.value.expense.change,
            icon: ArrowUpRight,
            highlight: false,
            series: dashboard.value.series.map((s) => Number(s.expense)),
            good: Number(dashboard.value.expense.change) <= 0,
          },
          {
            label: "净现金流",
            en: "NET CASH FLOW",
            value: money(dashboard.value.netCashFlow.value),
            change: dashboard.value.netCashFlow.change,
            icon: ArrowLeftRight,
            highlight: false,
            series: dashboard.value.series.map((s) => Number(s.income) - Number(s.expense)),
            good: Number(dashboard.value.netCashFlow.change) >= 0,
          },
          {
            label: "收支结余率",
            en: "SURPLUS MARGIN",
            value:
              dashboard.value.surplusMargin === null
                ? "—"
                : Number(dashboard.value.surplusMargin).toFixed(1) + "%",
            change: null,
            icon: ChartNoAxesCombined,
            highlight: false,
            series: [],
            good: true,
          },
        ]
      : [],
  );
  function spark(values: number[]) {
    const min = Math.min(...values),
      max = Math.max(...values);
    return values
      .map((v, i) => `${i ? "L" : "M"} ${i * 8} ${28 - ((v - min) / (max - min || 1)) * 23}`)
      .join(" ");
  }
  return { metrics, totalExpenses, spark };
}
