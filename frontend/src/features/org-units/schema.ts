import { z } from "zod";

export const createOrgUnitSchema = z.object({
  name: z.string().min(1, "Name is required"),
  unitType: z.string().min(1, "Unit type is required"),
  managerUserId: z.string().optional(),
});

export type CreateOrgUnitInput = z.infer<typeof createOrgUnitSchema>;
