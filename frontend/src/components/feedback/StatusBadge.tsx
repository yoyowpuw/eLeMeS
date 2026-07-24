import { Badge } from "../ui/badge";
import type { BadgeProps } from "../ui/badge";

/**
 * Maps every status string this app's API types actually produce
 * (Enrollment/Assessment/Certificate/Tenant status unions) to a badge
 * variant — falls back to the default neutral variant for anything unlisted
 * rather than guessing, since a wrong color is worse than a plain one.
 */
const STATUS_VARIANT: Record<string, NonNullable<BadgeProps["variant"]>> = {
  ACTIVE: "success",
  ISSUED: "success",
  PASSED: "success",
  COMPLETED: "success",
  IN_PROGRESS: "accent",
  MIGRATING: "accent",
  STARTED: "accent",
  ASSIGNED: "default",
  PROVISIONING: "warning",
  AWAITING_GRADING: "warning",
  FAILED: "danger",
  REVOKED: "danger",
  OFFBOARDED: "danger",
};

export function StatusBadge({ status }: { status: string }) {
  return <Badge variant={STATUS_VARIANT[status] ?? "default"}>{status.replaceAll("_", " ")}</Badge>;
}
