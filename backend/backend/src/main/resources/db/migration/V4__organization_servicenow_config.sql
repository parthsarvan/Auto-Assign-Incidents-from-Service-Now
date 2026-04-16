alter table if exists organization
    add column if not exists servicenow_instance_url varchar(512);

alter table if exists organization
    add column if not exists servicenow_username varchar(255);

alter table if exists organization
    add column if not exists servicenow_password varchar(512);

alter table if exists organization
    add column if not exists servicenow_connected_at timestamp with time zone;
