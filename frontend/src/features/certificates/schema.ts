import { z } from "zod";

export const revokeCertificateSchema = z.object({
  reason: z.string().min(1, "A reason is required"),
});

export type RevokeCertificateInput = z.infer<typeof revokeCertificateSchema>;
