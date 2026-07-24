import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Plus } from "lucide-react";
import { useCreateOrgUnit, useKnownOrgUnits } from "../../../api/orgUnits";
import { ApiError } from "../../../api/http";
import { useRoles } from "../../../auth/useRoles";
import { createOrgUnitSchema } from "../schema";
import type { CreateOrgUnitInput } from "../schema";
import { orgUnitColumns } from "../components/columns";
import { DataTable } from "../../../components/data-table/DataTable";
import { Button } from "../../../components/ui/button";
import { Input } from "../../../components/ui/input";
import { Label } from "../../../components/ui/label";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "../../../components/ui/dialog";

export function OrgUnitsListPage() {
  const { isManager, isAdmin } = useRoles();
  const canAuthor = isManager || isAdmin;
  const { data: units } = useKnownOrgUnits();
  const createUnit = useCreateOrgUnit();
  const [open, setOpen] = useState(false);

  const form = useForm<CreateOrgUnitInput>({
    resolver: zodResolver(createOrgUnitSchema),
    defaultValues: { name: "", unitType: "team", managerUserId: "" },
  });

  function onSubmit(values: CreateOrgUnitInput) {
    createUnit.mutate(
      { name: values.name, unitType: values.unitType, managerUserId: values.managerUserId || undefined },
      { onSuccess: () => { setOpen(false); form.reset({ name: "", unitType: "team", managerUserId: "" }); } },
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-h1 font-semibold text-slate-900 dark:text-slate-100">Org Hierarchy</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Each unit can sit at a different position in several independent hierarchy types at once (e.g. <code>reporting-line</code> vs.{" "}
            <code>cost-center</code>) — re-parenting moves a whole subtree in one bounded operation.
          </p>
        </div>
        {canAuthor && (
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button className="gap-1.5">
                <Plus className="size-4" />
                Create org unit
              </Button>
            </DialogTrigger>
            <DialogContent>
              <form onSubmit={form.handleSubmit(onSubmit)}>
                <DialogHeader>
                  <DialogTitle>Create an org unit</DialogTitle>
                </DialogHeader>
                <div className="flex flex-col gap-4 py-4">
                  <div className="flex flex-col gap-1.5">
                    <Label htmlFor="unit-name">Name</Label>
                    <Input id="unit-name" {...form.register("name")} />
                    {form.formState.errors.name && <p className="text-xs text-red-600">{form.formState.errors.name.message}</p>}
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <Label htmlFor="unit-type">Unit type</Label>
                    <Input id="unit-type" {...form.register("unitType")} />
                    {form.formState.errors.unitType && <p className="text-xs text-red-600">{form.formState.errors.unitType.message}</p>}
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <Label htmlFor="unit-manager">Manager username (optional)</Label>
                    <Input id="unit-manager" placeholder="e.g. maya" {...form.register("managerUserId")} />
                  </div>
                  <p className="text-xs text-slate-500 dark:text-slate-400">New units are created unparented — attach them to a hierarchy from the unit's own page.</p>
                  {createUnit.isError && (
                    <p role="alert" className="text-sm text-red-600">
                      {createUnit.error instanceof ApiError ? createUnit.error.message : "Failed to create org unit"}
                    </p>
                  )}
                </div>
                <DialogFooter>
                  <Button type="submit" isLoading={createUnit.isPending}>
                    {createUnit.isPending ? "Creating…" : "Create org unit"}
                  </Button>
                </DialogFooter>
              </form>
            </DialogContent>
          </Dialog>
        )}
      </div>
      {!canAuthor && <p className="text-sm text-slate-500 dark:text-slate-400">Only an admin or manager can create org units — you're signed in as a learner.</p>}

      <DataTable
        columns={orgUnitColumns}
        data={units}
        searchPlaceholder="Search org units…"
        exportFilename="org-units"
        emptyTitle="No org units yet"
        emptyDescription={canAuthor ? "Create one to get started." : undefined}
      />
    </div>
  );
}
