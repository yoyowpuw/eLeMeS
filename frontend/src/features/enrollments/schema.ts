import { z } from "zod";

export const enrollSchema = z.object({
  courseId: z.string().min(1, "Course ID is required"),
});

export type EnrollInput = z.infer<typeof enrollSchema>;
