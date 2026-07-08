import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  build: {
    chunkSizeWarningLimit: 700,
    rollupOptions: {
      output: {
        manualChunks: {
          livekit: ["livekit-client"],
          realtime: ["@stomp/stompjs", "sockjs-client"],
          react: ["react", "react-dom"]
        }
      }
    }
  },
  server: {
    port: 5173,
    strictPort: false,
    proxy: {
      "/salas": "http://localhost:8080",
      "/games": "http://localhost:8080",
      "/voice": "http://localhost:8080",
      "/metrics": "http://localhost:8080",
      "/ws": {
        target: "http://localhost:8080",
        ws: true
      }
    }
  }
});
