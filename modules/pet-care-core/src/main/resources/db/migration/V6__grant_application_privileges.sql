DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'petcare_app') THEN
        RAISE EXCEPTION 'Required NOLOGIN group role "petcare_app" does not exist; create it and grant it to the application login before running Flyway.';
    END IF;
END;
$$;

GRANT USAGE ON SCHEMA petcare TO petcare_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA petcare TO petcare_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA petcare TO petcare_app;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA petcare TO petcare_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA petcare
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO petcare_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA petcare
    GRANT USAGE, SELECT ON SEQUENCES TO petcare_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA petcare
    GRANT EXECUTE ON FUNCTIONS TO petcare_app;
