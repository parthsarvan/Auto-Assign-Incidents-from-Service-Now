create table if not exists mobile_device_token (
    id bigserial primary key,
    user_id bigint not null references users(u_id) on delete cascade,
    device_token varchar(512) not null,
    platform varchar(32) not null,
    environment varchar(32) not null,
    active boolean not null default true,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    last_seen_at timestamp with time zone not null,
    constraint uq_mobile_device_token_user_token unique (user_id, device_token)
);

create index if not exists idx_mobile_device_token_user_active
    on mobile_device_token (user_id, active);
