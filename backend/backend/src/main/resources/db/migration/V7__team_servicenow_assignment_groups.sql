alter table if exists team
    add column if not exists servicenow_assignment_groups varchar(4000);
