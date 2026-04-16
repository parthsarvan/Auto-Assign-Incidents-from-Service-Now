alter table if exists organization
    add column if not exists email_domain varchar(255);

alter table if exists users
    add column if not exists work_email varchar(255);

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'uq_organization_email_domain'
    ) then
        alter table organization
            add constraint uq_organization_email_domain unique (email_domain);
    end if;
end $$;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'uq_users_work_email'
    ) then
        alter table users
            add constraint uq_users_work_email unique (work_email);
    end if;
end $$;
