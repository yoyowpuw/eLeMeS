-- Consumer-side dedup log — see the identically-purposed migration in
-- assignment-enrollment and ProcessedMessageStore in `common` for the
-- full rationale.
create table processed_messages (
    message_id uuid primary key,
    tenant_id varchar(64) not null,
    consumer varchar(64) not null,
    processed_at timestamptz not null
);

create index idx_processed_messages_tenant on processed_messages (tenant_id);

alter table processed_messages enable row level security;
alter table processed_messages force row level security;

create policy tenant_isolation on processed_messages
    using (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*')
    with check (tenant_id = current_setting('app.tenant_id', true) or current_setting('app.tenant_id', true) = '*');
