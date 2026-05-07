alter table if exists team
    add column if not exists timezone varchar(64);

alter table if exists shift
    add column if not exists start_time time;

alter table if exists shift
    add column if not exists end_time time;
