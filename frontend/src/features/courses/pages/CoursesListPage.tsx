import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Plus } from "lucide-react";
import { useCreateCourse, useKnownCourses } from "../../../api/courses";
import { ApiError } from "../../../api/http";
import { useRoles } from "../../../auth/useRoles";
import { createCourseSchema } from "../schema";
import type { CreateCourseInput } from "../schema";
import { courseColumns } from "../components/columns";
import { DataTable } from "../../../components/data-table/DataTable";
import { Button } from "../../../components/ui/button";
import { Input } from "../../../components/ui/input";
import { Label } from "../../../components/ui/label";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "../../../components/ui/dialog";

export function CoursesListPage() {
  const { isManager, isAdmin } = useRoles();
  const canAuthor = isManager || isAdmin;
  const { data: courses } = useKnownCourses();
  const createCourse = useCreateCourse();
  const [open, setOpen] = useState(false);

  const form = useForm<CreateCourseInput>({
    resolver: zodResolver(createCourseSchema),
    defaultValues: { code: "", title: "" },
  });

  function onSubmit(values: CreateCourseInput) {
    createCourse.mutate(
      { code: values.code, title: values.title, initialContentHash: `sha256-${crypto.randomUUID()}` },
      { onSuccess: () => { setOpen(false); form.reset(); } },
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-h1 font-semibold text-slate-900 dark:text-slate-100">Courses</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">Courses created this session — course-management has no list endpoint, so this reflects what's been created here, not the full catalog.</p>
        </div>
        {canAuthor && (
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button className="gap-1.5">
                <Plus className="size-4" />
                Create course
              </Button>
            </DialogTrigger>
            <DialogContent>
              <form onSubmit={form.handleSubmit(onSubmit)}>
                <DialogHeader>
                  <DialogTitle>Create a course</DialogTitle>
                </DialogHeader>
                <div className="flex flex-col gap-4 py-4">
                  <div className="flex flex-col gap-1.5">
                    <Label htmlFor="course-code">Code</Label>
                    <Input id="course-code" {...form.register("code")} />
                    {form.formState.errors.code && <p className="text-xs text-red-600">{form.formState.errors.code.message}</p>}
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <Label htmlFor="course-title">Title</Label>
                    <Input id="course-title" {...form.register("title")} />
                    {form.formState.errors.title && <p className="text-xs text-red-600">{form.formState.errors.title.message}</p>}
                  </div>
                  {createCourse.isError && (
                    <p role="alert" className="text-sm text-red-600">
                      {createCourse.error instanceof ApiError ? createCourse.error.message : "Failed to create course"}
                    </p>
                  )}
                </div>
                <DialogFooter>
                  <Button type="submit" isLoading={createCourse.isPending}>
                    {createCourse.isPending ? "Creating…" : "Create course"}
                  </Button>
                </DialogFooter>
              </form>
            </DialogContent>
          </Dialog>
        )}
      </div>
      {!canAuthor && <p className="text-sm text-slate-500 dark:text-slate-400">Only an admin or manager can create courses — you're signed in as a learner.</p>}

      <DataTable
        columns={courseColumns}
        data={courses}
        searchPlaceholder="Search courses…"
        exportFilename="courses"
        emptyTitle="No courses yet"
        emptyDescription={canAuthor ? "Create one to get started." : "Ask an admin or manager to create one."}
      />
    </div>
  );
}
