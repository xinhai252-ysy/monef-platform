import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

// 两行保留一行生效：切换后重新启动 pnpm dev。
const backendTarget = "http://127.0.0.1:8088"; // Docker 后端，经 Nginx 转发
// const backendTarget = "http://127.0.0.1:8081"; // IDEA 后端，local 配置

export default defineConfig({
  plugins: [vue()],
  server: {
    host: "127.0.0.1",
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": { target: backendTarget, changeOrigin: true },
    },
  },
});
