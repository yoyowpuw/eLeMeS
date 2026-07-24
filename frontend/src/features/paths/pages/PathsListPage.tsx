import { useState } from "react";
import { useFieldArray, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Plus, Trash2 } from "lucide-react";
import { useCreatePath, useKnownPaths } from "../../../api/paths";
import { useKnownCourses } from "../../../api/courses";
import { ApiError } from "../../../api/http";
import { useRoles } from "../../../auth/useRoles";
import { createPathSchema } from "../schema";
import type { CreatePathInput } from "../schema";
import { pathColumns } from "../components/columns";
import { DataTable } from "../../../components/data-table/DataTable";
import { Button } from "../../../components/ui/button";
import { Input } from "../../../components/ui/input";
import { Label } from "../../../components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../../components/ui/select";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "../../../components/ui/dialog";

export function PathsListPage() {
  const { isManager, isAdmin } = useRoles();
  const canAuthor = isManager || isAdmin;
  const { data: courses } = useKnownCourses();
  const { data: paths } = useKnownPaths();
  const createPath = useCreatePath();
  const [open, setOpen] = useState(false);

  const form = useForm<CreatePathInput>({
    resolver: zodResolver(createPathSchema),
    defaultValues: { name: "", steps: [{ courseId: "" }] },
  });
  const stepFields = useFieldArray({ control: form.control, name: "steps" });

  function onSubmit(values: CreatePathInput) {
    createPath.mutate(
      { name: values.name, courseIds: values.steps.map((s) => s.courseId) },
      { onSuccess: () => { setOpen(false); form.reset({ name: "", steps: [{ courseId: "" }] }); } },
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-h1 font-semibold text-slate-900 dark:text-slate-100">Learning Paths</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">Ordered, multi-step sequences of courses — a learner completes each step's course in order.</p>
        </div>
        {canAuthor && (
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button className="gap-1.5" disabled={courses.length === 0}>
                <Plus className="size-4" />
                Create path
              </Button>
            </DialogTrigger>
            <DialogContent>
              <form onSubmit={form.handleSubmit(onSubmit)}>
                <DialogHeader>
                  <DialogTitle>Create a path</DialogTitle>
                </DialogHeader>
                <div className="flex max-h-96 flex-col gap-4 overflow-y-auto py-4">
                  <div className="flex flex-col gap-1.5">
                    <Label htmlFor="path-name">Name</Label>
                    <Input id="path-name" {...form.register("name")} />
                    {form.formState.errors.name && <p className="text-xs text-red-600">{form.formState.errors.name.message}</p>}
                  </div>
                  {stepFields.fields.map((field, index) => (
                    <div key={field.id} className="flex items-end gap-2">
                      <div className="flex flex-1 flex-col gap-1.5">
                        <Label>Step {index + 1}</Label>
                        <Select
                          value={form.watch(`steps.${index}.courseId`)}
                          onValueChange={(value) => form.setValue(`steps.${index}.courseId`, value, { shouldValidate: true })}
                        >
                          <SelectTrigger>
                            <SelectValue placeholder="Choose a course" />
                          </SelectTrigger>
                          <SelectContent>
                            {courses.map((course) => (
                              <SelectItem key={course.courseId} value={course.courseId}>
                                {course.code} — {course.title}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                      {stepFields.fields.length > 1 && (
                        <Button type="button" variant="ghost" size="icon" onClick={() => stepFields.remove(index)} aria-label={`Remove step ${index + 1}`}>
                          <Trash2 className="size-4" />
                        </Button>
                      )}
                    </div>
                  ))}
                  <Button type="button" variant="outline" size="sm" className="gap-1.5" onClick={() => stepFields.append({ courseId: "" })}>
                    <Plus className="size-3.5" />
                    Add step
                  </Button>
                  {createPath.isError && (
                    <p role="alert" className="text-sm text-red-600">
                      {createPath.error instanceof ApiError ? createPath.error.message : "Failed to create path"}
                    </p>
                  )}
                </div>
                <DialogFooter>
                  <Button type="submit" isLoading={createPath.isPending}>
                    {createPath.isPending ? "Creating…" : "Create path"}
                  </Button>
                </DialogFooter>
              </form>
            </DialogContent>
          </Dialog>
        )}
      </div>
      {!canAuthor && <p className="text-sm text-slate-500 dark:text-slate-400">Only an admin or manager can create learning paths — you're signed in as a learner.</p>}
      {canAuthor && courses.length === 0 && <p className="text-sm text-slate-500 dark:text-slate-400">Create at least one course first (see the Courses page).</p>}

      <DataTable
        columns={pathColumns}
        data={paths}
        searchPlaceholder="Search paths…"
        exportFilename="learning-paths"
        emptyTitle="No paths yet"
        emptyDescription={canAuthor ? "Create one to get started." : "Ask an admin or manager to create one."}
      />
    </div>
  );
}
