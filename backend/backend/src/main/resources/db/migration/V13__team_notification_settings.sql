create table if not exists team_notification_settings (
    id bigserial primary key,
    team_id bigint not null,
    slack_enabled boolean not null default false,
    email_enabled boolean not null default false,
    slack_webhook_url varchar(2048),
    slack_destination varchar(255),
    email_recipients varchar(4000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uq_team_notification_settings_team unique (team_id),
    constraint fk_team_notification_settings_team
        foreign key (team_id)
        references team (team_id)
        on delete cascade
);
