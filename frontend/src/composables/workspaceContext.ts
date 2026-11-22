import { inject, provide, type InjectionKey } from "vue";
import type { useFinanceWorkspace } from "./useFinanceWorkspace";
type FinanceWorkspace = ReturnType<typeof useFinanceWorkspace>;
const workspaceKey: InjectionKey<FinanceWorkspace> = Symbol("finance-workspace");
export function provideWorkspace(workspace: FinanceWorkspace) {
  provide(workspaceKey, workspace);
}
export function useWorkspace() {
  const workspace = inject(workspaceKey);
  if (!workspace) throw new Error("Workspace provider is missing");
  return workspace;
}
