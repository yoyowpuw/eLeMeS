import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { CheckCircle2, XCircle } from "lucide-react";
import { useVerifyCertificate } from "../../../api/certificates";
import { Card, CardContent, CardHeader, CardTitle } from "../../../components/ui/card";
import { Input } from "../../../components/ui/input";
import { Label } from "../../../components/ui/label";
import { Button } from "../../../components/ui/button";
import { cn } from "../../../lib/utils";

/**
 * Ch.26 §6: no AppShell/Sidebar/Topbar, no auth gate — this page must work
 * for a signed-out visitor, since a certificate is supposed to be
 * independently verifiable by anyone (an employer, a regulator) with no
 * platform access at all. Its own minimal shell, not the app's normal one.
 */
export function VerifyPage() {
  const [searchParams] = useSearchParams();
  const [certificateId, setCertificateId] = useState(searchParams.get("certificateId") ?? "");
  const [submittedId, setSubmittedId] = useState(searchParams.get("certificateId") ?? undefined);
  const { data, isFetching, isError } = useVerifyCertificate(submittedId);

  return (
    <div className="flex min-h-svh flex-col bg-slate-50 dark:bg-slate-950">
      <header className="flex h-14 items-center border-b border-slate-200 bg-white px-6 dark:border-slate-800 dark:bg-slate-950">
        <Link to="/" className="text-base font-bold text-blue-600">
          eLeMeS
        </Link>
      </header>
      <main className="flex flex-1 items-start justify-center p-6">
        <Card className="w-full max-w-md">
          <CardHeader>
            <CardTitle>Verify a Certificate</CardTitle>
            <p className="text-sm text-slate-500 dark:text-slate-400">No sign-in required — paste a certificate ID to independently check its authenticity.</p>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <form
              onSubmit={(e) => {
                e.preventDefault();
                setSubmittedId(certificateId);
              }}
              className="flex flex-col gap-3"
            >
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="certificate-id">Certificate ID</Label>
                <Input id="certificate-id" value={certificateId} onChange={(e) => setCertificateId(e.target.value)} required />
              </div>
              <Button type="submit" isLoading={isFetching} className="self-start">
                Verify
              </Button>
            </form>

            {isError && (
              <p role="alert" className="text-sm text-red-600">
                Could not verify — certificate not found.
              </p>
            )}
            {data && (
              <div
                className={cn(
                  "flex items-center gap-2 rounded-lg border p-4 text-sm font-semibold",
                  data.valid
                    ? "border-green-200 bg-green-50 text-green-700 dark:border-green-900 dark:bg-green-950 dark:text-green-300"
                    : "border-red-200 bg-red-50 text-red-700 dark:border-red-900 dark:bg-red-950 dark:text-red-300",
                )}
              >
                {data.valid ? <CheckCircle2 className="size-5" /> : <XCircle className="size-5" />}
                {data.valid ? "Valid — signature checks out." : "Invalid — signature does not match."}
              </div>
            )}
          </CardContent>
        </Card>
      </main>
    </div>
  );
}
