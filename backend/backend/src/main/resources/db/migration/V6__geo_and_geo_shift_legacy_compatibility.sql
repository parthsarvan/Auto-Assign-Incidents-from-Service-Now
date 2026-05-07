alter table if exists geo
    add column if not exists time_zone varchar(64);

alter table if exists geo_shift_mapping
    add column if not exists start_time time;

alter table if exists geo_shift_mapping
    add column if not exists end_time time;
