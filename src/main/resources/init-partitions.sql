-- ============================================================================
-- HAPI FHIR Multi-Tenant Partition Initialization (Dynamic)
-- ============================================================================
-- This script initializes partitions for clinic-based multi-tenancy.
--
-- For static initialization (when NestJS DB is not available), use the
-- hardcoded INSERT statements below. For dynamic sync from NestJS database,
-- run sync-partitions.sql instead.
--
-- Tables affected:
-- - HFJ_PARTITION: HAPI FHIR partition registry
--
-- Related tables in ddx_clinic_fhir (NestJS):
-- - organizations: Contains slug, fhirPartitionId, isActive
-- - partition_sequence: Auto-increment for new partition IDs
-- - global_config: Contains fhir.api_token and other settings
-- ============================================================================

-- ============================================================================
-- STATIC INITIALIZATION (Default Tenants)
-- ============================================================================
-- These are the default tenants created on fresh installation.
-- New tenants created via Super Admin portal are synced dynamically.

-- Partition 0: Default/System partition (required)
INSERT INTO HFJ_PARTITION (PART_ID, PART_NAME, PART_DESC)
VALUES (0, 'DEFAULT', 'Default system partition - used for system resources and public endpoints')
ON CONFLICT (PART_ID) DO NOTHING;

-- Partition 1: Hamburg Clinic (demo tenant)
INSERT INTO HFJ_PARTITION (PART_ID, PART_NAME, PART_DESC)
VALUES (1, 'HAMBURG', 'Hamburg Clinic - ddx-hamburg-clinic')
ON CONFLICT (PART_ID) DO NOTHING;

-- Partition 2: Berlin Clinic (demo tenant)
INSERT INTO HFJ_PARTITION (PART_ID, PART_NAME, PART_DESC)
VALUES (2, 'BERLIN', 'Berlin Clinic - ddx-berlin-clinic')
ON CONFLICT (PART_ID) DO NOTHING;

-- Partition 3: Munich Clinic (demo tenant)
INSERT INTO HFJ_PARTITION (PART_ID, PART_NAME, PART_DESC)
VALUES (3, 'MUNICH', 'Munich Clinic - ddx-munich-clinic')
ON CONFLICT (PART_ID) DO NOTHING;

-- Partition 4: Frankfurt Clinic (demo tenant)
INSERT INTO HFJ_PARTITION (PART_ID, PART_NAME, PART_DESC)
VALUES (4, 'FRANKFURT', 'Frankfurt Clinic - ddx-frankfurt-clinic')
ON CONFLICT (PART_ID) DO NOTHING;

-- Partition 5: Cologne Clinic (demo tenant)
INSERT INTO HFJ_PARTITION (PART_ID, PART_NAME, PART_DESC)
VALUES (5, 'COLOGNE', 'Cologne Clinic - ddx-cologne-clinic')
ON CONFLICT (PART_ID) DO NOTHING;

-- Partition 6: Shared Resources (cross-clinic resources)
INSERT INTO HFJ_PARTITION (PART_ID, PART_NAME, PART_DESC)
VALUES (6, 'SHARED', 'Shared resources - ddx-shared-clinic')
ON CONFLICT (PART_ID) DO NOTHING;

-- ============================================================================
-- VERIFICATION
-- ============================================================================
SELECT
    PART_ID AS id,
    PART_NAME AS name,
    PART_DESC AS description
FROM HFJ_PARTITION
ORDER BY PART_ID;

-- ============================================================================
-- DYNAMIC SYNC NOTES
-- ============================================================================
-- For production environments with dynamic tenant creation:
--
-- 1. The TenantRegistryService.java reads from ddx_clinic_fhir.organizations
--    and caches the mapping in memory with periodic refresh (60s default).
--
-- 2. New tenants created via Super Admin are automatically:
--    - Added to organizations table with fhirPartitionId
--    - Registered in HAPI FHIR via provisionPartition() endpoint
--    - Available immediately after cache refresh (or force refresh)
--
-- 3. To manually sync all partitions from NestJS to HAPI FHIR:
--    psql -U dudoxx_user -d ddx_hapifhir -f sync-partitions.sql
--
-- 4. Cross-database sync requires:
--    - PostgreSQL dblink extension
--    - Same credentials for both databases
--    - Network access between databases
-- ============================================================================
