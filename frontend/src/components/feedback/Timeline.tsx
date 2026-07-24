import { Check, Circle } from "lucide-react";
import { cn } from "../../lib/utils";

export interface TimelineStep {
  label: string;
  status: "done" | "current" | "upcoming";
}

export function Timeline({ steps }: { steps: TimelineStep[] }) {
  return (
    <ol className="flex flex-col gap-1">
      {steps.map((step, index) => (
        <li key={index} className="flex items-center gap-3 py-1">
          <span
            className={cn(
              "flex size-5 shrink-0 items-center justify-center rounded-full border text-xs",
              step.status === "done" && "border-green-600 bg-green-600 text-white",
              step.status === "current" && "border-blue-600 text-blue-600",
              step.status === "upcoming" && "border-slate-300 text-slate-300 dark:border-slate-700 dark:text-slate-700",
            )}
          >
            {step.status === "done" ? <Check className="size-3" /> : <Circle className="size-2 fill-current" />}
          </span>
          <span
            className={cn(
              "text-sm",
              step.status === "current" ? "font-medium text-slate-900 dark:text-slate-100" : "text-slate-500 dark:text-slate-400",
            )}
          >
            {step.label}
          </span>
        </li>
      ))}
    </ol>
  );
}
