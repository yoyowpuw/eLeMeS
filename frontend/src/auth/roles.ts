export const ROLE = {
  LEARNER: "learner",
  MANAGER: "manager",
  ADMIN: "admin",
  PLATFORM_ADMIN: "platform-admin",
} as const;

export type Role = (typeof ROLE)[keyof typeof ROLE];
