-- V049__fix_webhooks_rls_missing_ok.sql
-- Fix webhooks_org_isolation RLS policy to use missing_ok=true, consistent
-- with all other tenant-isolation policies in this schema. Without missing_ok,
-- the policy throws an error when app.current_org_id is not set (e.g. background
-- jobs, superuser sessions). With missing_ok=true, it returns NULL instead,
-- which causes the USING clause to evaluate to false — no rows returned.

DROP POLICY IF EXISTS webhooks_org_isolation ON webhooks;

CREATE POLICY webhooks_org_isolation ON webhooks
  USING (org_id = current_setting('app.current_org_id', true)::uuid);
