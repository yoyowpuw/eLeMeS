import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { useCompleteEnrollment, useEnrollment, useStartEnrollment } from "../../../api/enrollments";
import { useStartAssessment, useSubmitAssessment } from "../../../api/assessments";
import { useCertificateByEnrollment } from "../../../api/certificates";
import { ApiError } from "../../../api/http";
import type { Enrollment } from "../../../api/types";
import { StatusBadge } from "../../../components/feedback/StatusBadge";
import { Progress } from "../../../components/ui/progress";
import { Button } from "../../../components/ui/button";
import { Input } from "../../../components/ui/input";
import { Label } from "../../../components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "../../../components/ui/card";
import { Skeleton } from "../../../components/ui/skeleton";

export function EnrollmentDetailPage() {
  const { enrollmentId } = useParams<{ enrollmentId: string }>();
  const { data: enrollment, isLoading } = useEnrollment(enrollmentId);
  const start = useStartEnrollment();
  const complete = useCompleteEnrollment();
  const startAssessment = useStartAssessment();
  const submitAssessment = useSubmitAssessment();
  const queryClient = useQueryClient();

  // A Learning Path step's `Enrollment` is created server-side, not via
  // `useEnroll()` — so `myEnrollments` (the session-scoped list the
  // Certificates page reads) would never learn about it, or about its
  // *current* status, without this. Keyed on the live query result (not
  // captured once) so it stays in sync — see git history for the real bug
  // this fixes (a one-time snapshot froze at whatever status the
  // enrollment had when the user was about to navigate away).
  useEffect(() => {
    if (!enrollment) return;
    queryClient.setQueryData(["myEnrollments"], (existing: Enrollment[] = []) => {
      const index = existing.findIndex((e) => e.enrollmentId === enrollment.enrollmentId);
      if (index === -1) return [...existing, enrollment];
      if (existing[index] === enrollment) return existing;
      const copy = [...existing];
      copy[index] = enrollment;
      return copy;
    });
  }, [enrollment, queryClient]);

  const [assessmentId, setAssessmentId] = useState<string | undefined>();
  const [questionText, setQuestionText] = useState("2 + 2 = ?");
  const [optionA, setOptionA] = useState("3");
  const [optionB, setOptionB] = useState("4");
  const [correctOption, setCorrectOption] = useState<0 | 1>(1);
  const [selectedAnswer, setSelectedAnswer] = useState<0 | 1>(0);

  const isCompleted = enrollment?.status === "COMPLETED";
  const { data: certificate } = useCertificateByEnrollment(enrollmentId, isCompleted);

  if (isLoading) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-32 w-full max-w-lg" />
      </div>
    );
  }
  if (!enrollment) return <p className="text-sm text-slate-500">Enrollment not found.</p>;

  function handleStartAssessment(e: React.FormEvent) {
    e.preventDefault();
    startAssessment.mutate(
      {
        enrollmentId: enrollment!.enrollmentId,
        courseId: enrollment!.courseId,
        passingScore: 70,
        questions: [{ questionId: "q1", text: questionText, options: [optionA, optionB], correctOptionIndex: correctOption }],
      },
      { onSuccess: (assessment) => setAssessmentId(assessment.assessmentId) },
    );
  }

  function handleSubmitAssessment(e: React.FormEvent) {
    e.preventDefault();
    if (!assessmentId) return;
    submitAssessment.mutate({ assessmentId, answers: { q1: selectedAnswer } });
  }

  return (
    <div className="flex max-w-lg flex-col gap-4">
      <h1 className="text-h1 font-semibold text-slate-900 dark:text-slate-100">Enrollment</h1>

      <Card>
        <CardContent className="flex flex-col gap-2 pt-4">
          <div className="flex items-center gap-2">
            <code className="text-xs">{enrollment.courseId}</code>
            <StatusBadge status={enrollment.status} />
          </div>
          <div className="text-sm text-slate-500 dark:text-slate-400">Learner: {enrollment.learnerId}</div>
          <Progress value={enrollment.progressPercent} />
        </CardContent>
      </Card>

      {enrollment.status === "ASSIGNED" && (
        <Button isLoading={start.isPending} onClick={() => start.mutate(enrollment.enrollmentId)} className="self-start">
          {start.isPending ? "Starting…" : "Start course"}
        </Button>
      )}

      {enrollment.status === "IN_PROGRESS" && (
        <Card>
          <CardHeader>
            <CardTitle>Finish this course</CardTitle>
            <p className="text-sm text-slate-500 dark:text-slate-400">Either mark it complete directly, or take a short assessment first.</p>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <Button variant="outline" isLoading={complete.isPending} onClick={() => complete.mutate(enrollment.enrollmentId)} className="self-start">
              {complete.isPending ? "Completing…" : "Mark complete (no assessment)"}
            </Button>

            <p className="text-xs font-medium tracking-wide text-slate-400 uppercase">— or take an assessment —</p>

            {!assessmentId ? (
              <form onSubmit={handleStartAssessment} className="flex flex-col gap-3">
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="question-text">Question</Label>
                  <Input id="question-text" value={questionText} onChange={(e) => setQuestionText(e.target.value)} required />
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="option-a">Option A</Label>
                  <Input id="option-a" value={optionA} onChange={(e) => setOptionA(e.target.value)} required />
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="option-b">Option B</Label>
                  <Input id="option-b" value={optionB} onChange={(e) => setOptionB(e.target.value)} required />
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor="correct-option">Correct option</Label>
                  <select
                    id="correct-option"
                    value={correctOption}
                    onChange={(e) => setCorrectOption(Number(e.target.value) as 0 | 1)}
                    className="h-9 rounded-md border border-slate-200 bg-white px-3 text-sm dark:border-slate-800 dark:bg-slate-950"
                  >
                    <option value={0}>A</option>
                    <option value={1}>B</option>
                  </select>
                </div>
                <Button type="submit" isLoading={startAssessment.isPending} className="self-start">
                  {startAssessment.isPending ? "Creating…" : "Create assessment"}
                </Button>
              </form>
            ) : (
              <form onSubmit={handleSubmitAssessment} className="flex flex-col gap-3">
                <p className="text-sm font-medium text-slate-900 dark:text-slate-100">{questionText}</p>
                <label className="flex items-center gap-2 text-sm">
                  <input type="radio" name="answer" checked={selectedAnswer === 0} onChange={() => setSelectedAnswer(0)} /> {optionA}
                </label>
                <label className="flex items-center gap-2 text-sm">
                  <input type="radio" name="answer" checked={selectedAnswer === 1} onChange={() => setSelectedAnswer(1)} /> {optionB}
                </label>
                <Button type="submit" isLoading={submitAssessment.isPending} className="self-start">
                  {submitAssessment.isPending ? "Submitting…" : "Submit answer"}
                </Button>
                {submitAssessment.isSuccess && (
                  <p className="text-sm">
                    Result: <strong>{submitAssessment.data.status}</strong> (score {submitAssessment.data.score})
                  </p>
                )}
              </form>
            )}
            {startAssessment.isError && (
              <p role="alert" className="text-sm text-red-600">
                {startAssessment.error instanceof ApiError ? startAssessment.error.message : "Failed to create assessment"}
              </p>
            )}
          </CardContent>
        </Card>
      )}

      {isCompleted && (
        <Card>
          <CardHeader>
            <CardTitle>Certificate</CardTitle>
          </CardHeader>
          <CardContent>
            {certificate ? (
              <div className="flex flex-col gap-1 text-sm">
                <span>
                  Issued {new Date(certificate.issuedAt).toLocaleString()} — status <StatusBadge status={certificate.status} />
                </span>
                <code className="text-xs text-slate-500 dark:text-slate-400">{certificate.certificateId}</code>
              </div>
            ) : (
              <p className="text-sm text-slate-500 dark:text-slate-400">Completed — waiting for Certification to issue the certificate (a couple of Kafka hops away)…</p>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
