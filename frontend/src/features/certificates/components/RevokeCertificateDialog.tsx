import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRevokeCertificate } from "../../../api/certificates";
import { ApiError } from "../../../api/http";
import { revokeCertificateSchema } from "../schema";
import type { RevokeCertificateInput } from "../schema";
import { Button } from "../../../components/ui/button";
import { Input } from "../../../components/ui/input";
import { Label } from "../../../components/ui/label";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "../../../components/ui/dialog";

export function RevokeCertificateDialog({ certificateId }: { certificateId: string }) {
  const revoke = useRevokeCertificate();
  const [open, setOpen] = useState(false);
  const form = useForm<RevokeCertificateInput>({ resolver: zodResolver(revokeCertificateSchema), defaultValues: { reason: "" } });

  function onSubmit(values: RevokeCertificateInput) {
    revoke.mutate({ certificateId, reason: values.reason }, { onSuccess: () => { setOpen(false); form.reset(); } });
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="danger" size="sm">
          Revoke…
        </Button>
      </DialogTrigger>
      <DialogContent>
        <form onSubmit={form.handleSubmit(onSubmit)}>
          <DialogHeader>
            <DialogTitle>Revoke certificate</DialogTitle>
          </DialogHeader>
          <div className="flex flex-col gap-4 py-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="revoke-reason">Reason</Label>
              <Input id="revoke-reason" {...form.register("reason")} />
              {form.formState.errors.reason && <p className="text-xs text-red-600">{form.formState.errors.reason.message}</p>}
            </div>
            {revoke.isError && (
              <p role="alert" className="text-sm text-red-600">
                {revoke.error instanceof ApiError ? revoke.error.message : "Failed to revoke — you may be outside this certificate's org scope"}
              </p>
            )}
          </div>
          <DialogFooter>
            <Button type="submit" variant="danger" isLoading={revoke.isPending}>
              {revoke.isPending ? "Revoking…" : "Confirm revoke"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
