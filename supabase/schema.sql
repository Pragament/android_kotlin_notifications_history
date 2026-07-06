-- =============================================================================
-- Supabase Schema for Notification History Forwarding
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. TABLES
-- ---------------------------------------------------------------------------

-- Devices table: stores each registered device and its group membership
CREATE TABLE IF NOT EXISTS public.devices (
    device_id   TEXT        PRIMARY KEY,
    device_name TEXT        NOT NULL,
    group_id    UUID        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- OTPs table: short-lived codes used to link a new device to an existing group
CREATE TABLE IF NOT EXISTS public.otps (
    code        TEXT        PRIMARY KEY,
    device_id   TEXT        NOT NULL,
    group_id    UUID        NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL
);

-- Forward rules table: per-group rules that control which notifications are forwarded
CREATE TABLE IF NOT EXISTS public.forward_rules (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id      UUID        NOT NULL,
    app_source    TEXT,                       -- package-name filter (NULL = any app)
    text_contains TEXT,                       -- content substring filter (NULL = any text)
    is_enabled    BOOLEAN     NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Forwarded notifications table: the actual notifications pushed between devices
CREATE TABLE IF NOT EXISTS public.forwarded_notifications (
    id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id           UUID        NOT NULL,
    sender_device_id   TEXT        NOT NULL,
    sender_device_name TEXT        NOT NULL,
    package_name       TEXT        NOT NULL,
    app_name           TEXT        NOT NULL,
    title              TEXT,
    content            TEXT,
    timestamp          BIGINT      NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- 2. INDEXES
-- ---------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_devices_group_id
    ON public.devices (group_id);

CREATE INDEX IF NOT EXISTS idx_forward_rules_group_id
    ON public.forward_rules (group_id);

CREATE INDEX IF NOT EXISTS idx_forwarded_notifications_group_id
    ON public.forwarded_notifications (group_id);

-- ---------------------------------------------------------------------------
-- 3. REALTIME
-- ---------------------------------------------------------------------------

ALTER PUBLICATION supabase_realtime ADD TABLE public.forwarded_notifications;

-- ---------------------------------------------------------------------------
-- 4. ROW LEVEL SECURITY
-- ---------------------------------------------------------------------------

-- Enable RLS on every table
ALTER TABLE public.devices                  ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.otps                     ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.forward_rules            ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.forwarded_notifications  ENABLE ROW LEVEL SECURITY;

-- Permissive policies for the anon role (anon key is used by the Android app)

-- devices
CREATE POLICY "anon_select_devices"  ON public.devices  FOR SELECT  TO anon USING (true);
CREATE POLICY "anon_insert_devices"  ON public.devices  FOR INSERT  TO anon WITH CHECK (true);
CREATE POLICY "anon_update_devices"  ON public.devices  FOR UPDATE  TO anon USING (true) WITH CHECK (true);
CREATE POLICY "anon_delete_devices"  ON public.devices  FOR DELETE  TO anon USING (true);

-- otps
CREATE POLICY "anon_select_otps"  ON public.otps  FOR SELECT  TO anon USING (true);
CREATE POLICY "anon_insert_otps"  ON public.otps  FOR INSERT  TO anon WITH CHECK (true);
CREATE POLICY "anon_update_otps"  ON public.otps  FOR UPDATE  TO anon USING (true) WITH CHECK (true);
CREATE POLICY "anon_delete_otps"  ON public.otps  FOR DELETE  TO anon USING (true);

-- forward_rules
CREATE POLICY "anon_select_forward_rules"  ON public.forward_rules  FOR SELECT  TO anon USING (true);
CREATE POLICY "anon_insert_forward_rules"  ON public.forward_rules  FOR INSERT  TO anon WITH CHECK (true);
CREATE POLICY "anon_update_forward_rules"  ON public.forward_rules  FOR UPDATE  TO anon USING (true) WITH CHECK (true);
CREATE POLICY "anon_delete_forward_rules"  ON public.forward_rules  FOR DELETE  TO anon USING (true);

-- forwarded_notifications
CREATE POLICY "anon_select_forwarded_notifications"  ON public.forwarded_notifications  FOR SELECT  TO anon USING (true);
CREATE POLICY "anon_insert_forwarded_notifications"  ON public.forwarded_notifications  FOR INSERT  TO anon WITH CHECK (true);
CREATE POLICY "anon_update_forwarded_notifications"  ON public.forwarded_notifications  FOR UPDATE  TO anon USING (true) WITH CHECK (true);
CREATE POLICY "anon_delete_forwarded_notifications"  ON public.forwarded_notifications  FOR DELETE  TO anon USING (true);
