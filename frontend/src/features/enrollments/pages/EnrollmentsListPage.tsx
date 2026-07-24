import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Plus } from "lucide-react";
import { useEnroll, useMyEnrollments } from "../../../api/enrollments";
import { ApiError } from "../../../api/http";
import { enrollSchema } from "../schema";
import type { EnrollInput } from "../schema";
import { enrollmentColumns } from "../components/columns";
import { DataTable } from "../../../components/data-table/DataTable";
import { Button } from "../../../components/ui/button";
import { Input } from "../../../components/ui/input";
import { Label } from "../../../components/ui/label";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "../../../components/ui/dialog";

export function EnrollmentsListPage() {
  const auth = useAuth();
  const [searchParams] = useSearchParams();
  const { data: enrollments } = useMyEnrollments();
  const enroll = useEnroll();
  const [open, setOpen] = useState(false);
  const learnerId = auth.user?.profile.preferred_username ?? "";

  const form = useForm<EnrollInput>({
    resolver: zodResolver(enrollSchema),
    defaultValues: { courseId: searchParams.get("courseId") ?? "" },
  });

  function onSubmit(values: EnrollInput) {
    enroll.mutate({ learnerId, courseId: values.courseId }, { onSuccess: () => { setOpen(false); form.reset({ courseId: "" }); } });
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-h1 font-semibold text-slate-900 dark:text-slate-100">My Enrollments</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">Enrollments this session.</p>
        </div>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger asChild>
            <Button className="gap-1.5">
              <Plus className="size-4" />
              Enroll in a course
            </Button>
          </DialogTrigger>
          <DialogContent>
            <form onSubmit={form.handleSubmit(onSubmit)}>
              <DialogHeader>
                <DialogTitle>Enroll in a course</DialogTitle>
              </DialogHeader>
              <div className="flex flex-col gap-4 py-4">
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="enroll-course-id">Course ID</Label>
                  <Input id="enroll-course-id" placeholder="from the Courses page" {...form.register("courseId")} />
                  {form.formState.errors.courseId && <p className="text-xs text-red-600">{form.formState.errors.courseId.message}</p>}
                </div>
                {enroll.isError && (
                  <p role="alert" className="text-sm text-red-600">
                    {enroll.error instanceof ApiError ? enroll.error.message : "Failed to enroll"}
                  </p>
                )}
              </div>
              <DialogFooter>
                <Button type="submit" isLoading={enroll.isPending}>
                  {enroll.isPending ? "Enrolling…" : "Enroll"}
                </Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      <DataTable
        columns={enrollmentColumns}
        data={enrollments}
        searchPlaceholder="Search enrollments…"
        exportFilename="enrollments"
        emptyTitle="No enrollments yet"
        emptyDescription="Enroll in a course to get started."
      />
    </div>
  );
}
