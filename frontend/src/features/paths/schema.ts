import { z } from "zod";

export const createPathSchema = z.object({
  name: z.string().min(1, "Name is required"),
  steps: z.array(z.object({ courseId: z.string().min(1, "Choose a course") })).min(1, "At least one step is required"),
});

export type CreatePathInput = z.infer<typeof createPathSchema>;
