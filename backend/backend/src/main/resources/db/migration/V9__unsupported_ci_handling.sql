ALTER TABLE team
    ADD COLUMN IF NOT EXISTS unsupported_ci_policy VARCHAR(64) NOT NULL DEFAULT 'SKIP_AND_LOG';

ALTER TABLE team
    ADD COLUMN IF NOT EXISTS unsupported_ci_fallback_tm_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_team_unsupported_ci_fallback_tm'
    ) THEN
        ALTER TABLE team
            ADD CONSTRAINT fk_team_unsupported_ci_fallback_tm
            FOREIGN KEY (unsupported_ci_fallback_tm_id)
            REFERENCES team_member(tm_id)
            ON DELETE SET NULL;
    END IF;
END $$;
