alter table if exists team
    add column if not exists join_code varchar(255);

alter table if exists servicenow_run_log
    add column if not exists team_name varchar(255);

do $$
declare
    incidents_type text;
begin
    select data_type
      into incidents_type
      from information_schema.columns
     where table_name = 'servicenow_run_log'
       and column_name = 'incidents_json';

    if incidents_type is not null and lower(incidents_type) <> 'text' then
        delete from servicenow_run_log;
        alter table servicenow_run_log
            alter column incidents_json type text using null::text;
    end if;
end $$;

do $$
declare
    selections_type text;
begin
    select data_type
      into selections_type
      from information_schema.columns
     where table_name = 'servicenow_run_log'
       and column_name = 'assignment_selections_json';

    if selections_type is not null and lower(selections_type) <> 'text' then
        delete from servicenow_run_log;
        alter table servicenow_run_log
            alter column assignment_selections_json type text using null::text;
    end if;
end $$;

do $$
declare
    results_type text;
begin
    select data_type
      into results_type
      from information_schema.columns
     where table_name = 'servicenow_run_log'
       and column_name = 'assignment_results_json';

    if results_type is not null and lower(results_type) <> 'text' then
        delete from servicenow_run_log;
        alter table servicenow_run_log
            alter column assignment_results_json type text using null::text;
    end if;
end $$;
