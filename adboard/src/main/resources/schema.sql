-- Выполняется до Hibernate ddl-auto. Старая таблица users без username/role: Hibernate не может
-- "ADD COLUMN ... NOT NULL" при непустой таблице — сначала nullable + UPDATE + SET NOT NULL.
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'users'
  ) THEN
    IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'username'
    ) THEN
      ALTER TABLE users ADD COLUMN username varchar(255);
    END IF;
    UPDATE users
    SET username = COALESCE(NULLIF(TRIM(username), ''), 'legacy_' || id::text)
    WHERE username IS NULL OR TRIM(username) = '';
    ALTER TABLE users ALTER COLUMN username SET NOT NULL;

    IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'role'
    ) THEN
      ALTER TABLE users ADD COLUMN role varchar(255);
    END IF;
    UPDATE users SET role = COALESCE(NULLIF(TRIM(role), ''), 'USER') WHERE role IS NULL OR TRIM(role) = '';
    ALTER TABLE users ALTER COLUMN role SET NOT NULL;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'users_role_check_app') THEN
      BEGIN
        ALTER TABLE users ADD CONSTRAINT users_role_check_app CHECK (role IN ('USER', 'ADMIN'));
      EXCEPTION
        WHEN duplicate_object THEN NULL;
      END;
    END IF;

    IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'password_hash'
    ) THEN
      ALTER TABLE users ADD COLUMN password_hash varchar(255);
    END IF;
    ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
  END IF;
END $$;
