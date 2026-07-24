import { z } from "zod";

export const createCourseSchema = z.object({
  code: z.string().min(1, "Code is required"),
  title: z.string().min(1, "Title is required"),
});

export type CreateCourseInput = z.infer<typeof createCourseSchema>;
