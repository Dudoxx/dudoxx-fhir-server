-- ============================================================================
-- HAPI FHIR Dynamic Partition Synchronization Script
-- ============================================================================
-- This script syncs partitions from the NestJS organizations table (ddx_clinic_fhir)
-- to the HAPI FHIR HFJ_PARTITION table (ddx_hapifhir).
--
-- Prerequisites:
-- - dblink extension must be installed on ddx_hapifhir database
-- - Appropriate permissions for cross-database access
--
-- Usage:
-- psql -U dudoxx_user -d ddx_hapifhir -f sync-partitions.sql
-- ============================================================================

-- Step 1: Enable dblink extension if not already enabled
CREATE EXTENSION IF NOT EXISTS dblink;

-- Step 2: Create a function to sync partitions dynamically
CREATE OR REPLACE FUNCTION sync_fhir_partitions()
RETURNS TABLE(
    action TEXT,
    partition_id INTEGER,
    partition_name TEXT,
    partition_desc TEXT
) AS $$
DECLARE
    rec RECORD;
    existing_count INTEGER;
BEGIN
    -- Ensure default partition exists
    INSERT INTO HFJ_PARTITION (PART_ID, PART_NAME, PART_DESC)
    VALUES (0, 'DEFAULT', 'Default system partition')
    ON CONFLICT (PART_ID) DO NOTHING;

    -- Log default partition
    SELECT COUNT(*) INTO existing_count FROM HFJ_PARTITION WHERE PART_ID = 0;
    IF existing_count > 0 THEN
        RETURN QUERY SELECT 'EXISTS'::TEXT, 0, 'DEFAULT'::TEXT, 'Default system partition'::TEXT;
    END IF;

    -- Fetch organizations from NestJS database (ddx_clinic_fhir)
    FOR rec IN
        SELECT *
        FROM dblink(
            'host=localhost port=5432 dbname=ddx_clinic_fhir user=dudoxx_user password=admin',
            'SELECT
                slug,
                name,
                "displayName",
                "fhirPartitionId",
                "isActive",
                type
             FROM organizations
             WHERE slug IS NOT NULL
               AND "fhirPartitionId" IS NOT NULL
               AND "isActive" = true
             ORDER BY "fhirPartitionId"'
        ) AS t(
            slug VARCHAR,
            name VARCHAR,
            display_name VARCHAR,
            fhir_partition_id INTEGER,
            is_active BOOLEAN,
            org_type VARCHAR
        )
    LOOP
        -- Create partition name (uppercase, no dashes/special chars)
        -- Create description with org details
        INSERT INTO HFJ_PARTITION (PART_ID, PART_NAME, PART_DESC)
        VALUES (
            rec.fhir_partition_id,
            UPPER(REPLACE(REPLACE(rec.slug, 'ddx-', ''), '-clinic', '')),
            COALESCE(rec.display_name, rec.name) || ' - ' || rec.slug
        )
        ON CONFLICT (PART_ID) DO UPDATE
        SET PART_NAME = EXCLUDED.PART_NAME,
            PART_DESC = EXCLUDED.PART_DESC;

        -- Log action
        RETURN QUERY SELECT
            'SYNCED'::TEXT,
            rec.fhir_partition_id,
            UPPER(REPLACE(REPLACE(rec.slug, 'ddx-', ''), '-clinic', '')),
            (COALESCE(rec.display_name, rec.name) || ' - ' || rec.slug);
    END LOOP;

    RETURN;
END;
$$ LANGUAGE plpgsql;

-- Step 3: Run the sync
SELECT * FROM sync_fhir_partitions();

-- Step 4: Verify all partitions
SELECT
    PART_ID AS partition_id,
    PART_NAME AS partition_name,
    PART_DESC AS description
FROM HFJ_PARTITION
ORDER BY PART_ID;
