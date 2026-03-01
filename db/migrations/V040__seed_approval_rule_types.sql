-- ============================================================================
-- V040: Ensure rules 003-008 are PUBLISHED so they can have v2 PENDING_APPROVAL
--
-- Rules seeded in V022 for SCORECARD, SCRIPT, FLOW, DMN, ENSEMBLE are already
-- PUBLISHED. CEP (rule 008) was seeded as DRAFT. This migration normalises
-- them all to PUBLISHED so V041 can safely create v2 versions.
-- ============================================================================

-- Update rules to PUBLISHED if they are not already
UPDATE rules
SET    status     = 'PUBLISHED',
       updated_at = NOW()
WHERE  id IN (
    'e0000000-0000-0000-0000-000000000003',
    'e0000000-0000-0000-0000-000000000004',
    'e0000000-0000-0000-0000-000000000005',
    'e0000000-0000-0000-0000-000000000006',
    'e0000000-0000-0000-0000-000000000007',
    'e0000000-0000-0000-0000-000000000008'
)
AND status != 'PUBLISHED';

-- Update their v1 versions to PUBLISHED
UPDATE rule_versions
SET    status = 'PUBLISHED'
WHERE  rule_id IN (
    'e0000000-0000-0000-0000-000000000003',
    'e0000000-0000-0000-0000-000000000004',
    'e0000000-0000-0000-0000-000000000005',
    'e0000000-0000-0000-0000-000000000006',
    'e0000000-0000-0000-0000-000000000007',
    'e0000000-0000-0000-0000-000000000008'
)
AND version = 1
AND status != 'PUBLISHED';
