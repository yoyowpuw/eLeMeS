import { Badge } from "../ui/badge";

const ROLE_LABEL: Record<string, string> = {
  learner: "Learner",
  manager: "Manager",
  admin: "Admin",
  "platform-admin": "Platform Admin",
};

export function RoleBadge({ role }: { role: string }) {
  const variant = role === "platform-admin" || role === "admin" ? "accent" : role === "manager" ? "warning" : "default";
  return <Badge variant={variant}>{ROLE_LABEL[role] ?? role}</Badge>;
}
