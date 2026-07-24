import type { ReactNode } from "react";

/**
 * Scopes index.css's old plain-element-selector styling (`a`, `button`,
 * `.card`, etc.) to just the pages that still rely on it — without this,
 * those unlayered rules outrank Tailwind utilities on ANY matching element
 * anywhere in the app, including brand-new AppShell components (every
 * `Link`/`NavLink` renders an `<a>`, every `Button` a `<button>`). Removed
 * once every page is migrated to the new design system (final cleanup
 * phase), along with index.css itself.
 */
export function LegacyPage({ children }: { children: ReactNode }) {
  return <div className="legacy-page">{children}</div>;
}
