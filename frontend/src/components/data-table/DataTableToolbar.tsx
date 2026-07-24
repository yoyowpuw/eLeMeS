import type { Table } from "@tanstack/react-table";
import { Download, Search, SlidersHorizontal } from "lucide-react";
import { Input } from "../ui/input";
import { Button } from "../ui/button";
import { DropdownMenu, DropdownMenuCheckboxItem, DropdownMenuContent, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from "../ui/dropdown-menu";

/** Serializes the table's currently VISIBLE, filtered/sorted rows — an honest capability since it needs no backend support, unlike a fake "export everything" that implies data this app doesn't have. */
function exportVisibleRowsToCsv<TData>(table: Table<TData>, filename: string) {
  const columns = table.getVisibleLeafColumns().filter((c) => c.id !== "actions");
  const header = columns.map((c) => JSON.stringify(String(c.columnDef.header ?? c.id))).join(",");
  const rows = table.getRowModel().rows.map((row) =>
    columns.map((c) => JSON.stringify(String(row.getValue(c.id) ?? ""))).join(","),
  );
  const csv = [header, ...rows].join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `${filename}.csv`;
  link.click();
  URL.revokeObjectURL(url);
}

export function DataTableToolbar<TData>({
  table,
  searchPlaceholder,
  exportFilename,
}: {
  table: Table<TData>;
  searchPlaceholder: string;
  exportFilename: string;
}) {
  return (
    <div className="flex items-center justify-between gap-2 pb-3">
      <div className="relative w-full max-w-64">
        <Search className="pointer-events-none absolute top-1/2 left-2.5 size-3.5 -translate-y-1/2 text-slate-400" aria-hidden="true" />
        <Input
          placeholder={searchPlaceholder}
          value={(table.getState().globalFilter as string) ?? ""}
          onChange={(e) => table.setGlobalFilter(e.target.value)}
          className="pl-8"
        />
      </div>
      <div className="flex items-center gap-2">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" size="sm" className="gap-1.5">
              <SlidersHorizontal className="size-3.5" />
              Columns
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuLabel>Toggle columns</DropdownMenuLabel>
            <DropdownMenuSeparator />
            {table
              .getAllLeafColumns()
              .filter((column) => column.getCanHide())
              .map((column) => (
                <DropdownMenuCheckboxItem
                  key={column.id}
                  checked={column.getIsVisible()}
                  onCheckedChange={(value) => column.toggleVisibility(!!value)}
                  onSelect={(e) => e.preventDefault()}
                >
                  {typeof column.columnDef.header === "string" ? column.columnDef.header : column.id}
                </DropdownMenuCheckboxItem>
              ))}
          </DropdownMenuContent>
        </DropdownMenu>
        <Button variant="outline" size="sm" className="gap-1.5" onClick={() => exportVisibleRowsToCsv(table, exportFilename)}>
          <Download className="size-3.5" />
          Export
        </Button>
      </div>
    </div>
  );
}
