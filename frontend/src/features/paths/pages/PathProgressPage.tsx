import { Link, useParams } from "react-router-dom";
import { ArrowRight } from "lucide-react";
import { usePathProgress } from "../../../api/pathEnrollments";
import { StatusBadge } from "../../../components/feedback/StatusBadge";
import { Timeline } from "../../../components/feedback/Timeline";
import type { TimelineStep } from "../../../components/feedback/Timeline";
import { Card, CardContent, CardHeader, CardTitle } from "../../../components/ui/card";
import { Button } from "../../../components/ui/button";
import { Skeleton } from "../../../components/ui/skeleton";

export function PathProgressPage() {
  const { pathProgressId } = useParams<{ pathProgressId: string }>();
  const { data: progress, isLoading } = usePathProgress(pathProgressId);

  if (isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-40 w-full max-w-lg" />
      </div>
    );
  }
  if (!progress) return <p className="text-sm text-slate-500">Path progress not found.</p>;

  const steps: TimelineStep[] = Array.from({ length: progress.totalSteps }, (_, i) => {
    if (i < progress.realizedStepCourseIds.length) return { label: progress.realizedStepCourseIds[i], status: "done" };
    if (i === progress.currentStepIndex) return { label: `Step ${i + 1} (current)`, status: "current" };
    return { label: `Step ${i + 1}`, status: "upcoming" };
  });

  return (
    <div className="flex max-w-lg flex-col gap-4">
      <h1 className="text-h1 font-semibold text-slate-900 dark:text-slate-100">Path Progress</h1>
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <StatusBadge status={progress.status} />
            <span className="text-sm font-normal text-slate-500 dark:text-slate-400">
              Step {Math.min(progress.currentStepIndex + 1, progress.totalSteps)} of {progress.totalSteps}
            </span>
          </CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <Timeline steps={steps} />
          {progress.status === "COMPLETED" ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Path complete — the final certificate carries this exact realized step sequence.{" "}
              <Link to="/certificates" className="text-blue-600 hover:underline">
                View your certificates
              </Link>
              .
            </p>
          ) : (
            progress.currentStepEnrollmentId && (
              <Button variant="outline" asChild className="w-fit gap-1.5">
                <Link to={`/enrollments/${progress.currentStepEnrollmentId}`}>
                  Continue current step
                  <ArrowRight className="size-3.5" />
                </Link>
              </Button>
            )
          )}
        </CardContent>
      </Card>
    </div>
  );
}
