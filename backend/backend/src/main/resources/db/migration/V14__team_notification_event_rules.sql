alter table team_notification_settings
    add column if not exists notify_assignment_success boolean not null default true,
    add column if not exists notify_assignment_skipped boolean not null default true,
    add column if not exists notify_unsupported_ci boolean not null default true,
    add column if not exists notify_poller_failure boolean not null default true;
