import { z } from "zod";

export const createTenantSchema = z.object({
  tenantId: z.string().min(1, "Tenant ID is required"),
  name: z.string().min(1, "Name is required"),
  isolationTier: z.enum(["POOLED", "SILO"]),
  region: z.string().min(1, "Region is required"),
});

export type CreateTenantInput = z.infer<typeof createTenantSchema>;
