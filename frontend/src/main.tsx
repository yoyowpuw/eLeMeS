import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AuthProvider } from "react-oidc-context";
import "./index.css";
// Tailwind's `@layer base` reset has LOWER cascade-layer precedence than any
// un-layered rule (a hard CSS rule, independent of import order) — importing
// this alongside the still-un-migrated index.css is safe: index.css's plain
// element selectors keep winning for old pages until they're migrated
// page-by-page (see the redesign plan), while new components can use
// Tailwind utilities immediately. Both imports get removed together in the
// final cleanup phase once every page is migrated.
import "./styles/globals.css";
import { App } from "./app/App.tsx";
import { oidcConfig } from "./auth/oidcConfig.ts";
import { TooltipProvider } from "./components/ui/tooltip.tsx";
import { Toaster } from "./components/ui/toaster.tsx";

const queryClient = new QueryClient();

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <AuthProvider {...oidcConfig}>
      <QueryClientProvider client={queryClient}>
        <TooltipProvider delayDuration={200}>
          <BrowserRouter>
            <App />
          </BrowserRouter>
          <Toaster />
        </TooltipProvider>
      </QueryClientProvider>
    </AuthProvider>
  </StrictMode>,
);
