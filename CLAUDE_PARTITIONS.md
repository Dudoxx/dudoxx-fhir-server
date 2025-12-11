# HAPI FHIR - Multi-Tenancy & Partitions

## Partition Architecture

Each clinic operates in an isolated PostgreSQL partition:

| Clinic ID | Partition ID | Name | Description |
|-----------|--------------|------|-------------|
| `default` | 0 | DEFAULT | System partition |
| `ddx-hamburg-clinic` | 1 | HAMBURG | Hamburg Clinic |
| `ddx-berlin-clinic` | 2 | BERLIN | Berlin Clinic |
| `ddx-munich-clinic` | 3 | MUNICH | Munich Clinic |
| `ddx-frankfurt-clinic` | 4 | FRANKFURT | Frankfurt Clinic |
| `ddx-cologne-clinic` | 5 | COLOGNE | Cologne Clinic |
| `ddx-shared-clinic` | 6 | SHARED | Shared resources |

---

## Adding a New Clinic

### Step 1: Update init-partitions.sql

```sql
-- src/main/resources/init-partitions.sql
INSERT INTO HFJ_PARTITION (PART_ID, PART_NAME, PART_DESC)
VALUES (7, 'DUSSELDORF', 'Düsseldorf Clinic')
ON CONFLICT (PART_ID) DO NOTHING;
```

### Step 2: Update ClinicPartitionInterceptor.java

```java
// src/main/java/.../interceptor/ClinicPartitionInterceptor.java
private static final Map<String, Integer> CLINIC_PARTITION_MAP = Map.of(
    "ddx-hamburg-clinic", 1,
    "ddx-berlin-clinic", 2,
    "ddx-munich-clinic", 3,
    "ddx-frankfurt-clinic", 4,
    "ddx-cologne-clinic", 5,
    "ddx-shared-clinic", 6,
    "ddx-dusseldorf-clinic", 7  // NEW
);
```

### Step 3: Rebuild and Restart

```bash
mvn clean package -DskipTests
./stop-server.sh && ./start-server.sh
```

### Step 4: Verify Partition

```bash
psql -U dudoxx_user -d ddx_hapifhir -c "SELECT * FROM HFJ_PARTITION;"
```

---

## Configuration (application.yaml)

```yaml
hapi:
  fhir:
    partitioning:
      enabled: true
      allow_references_across_partitions: true
      partitioning_include_in_search_hashes: true
      default_partition_id: 0
```

---

## ClinicPartitionInterceptor Implementation

```java
public class ClinicPartitionInterceptor {

    @Hook(Pointcut.STORAGE_PARTITION_IDENTIFY_READ)
    public RequestPartitionId identifyRead(RequestDetails requestDetails) {
        String clinicId = requestDetails.getHeader("X-Clinic-ID");
        Integer partitionId = CLINIC_PARTITION_MAP.get(clinicId);

        if (partitionId == null) {
            throw new InvalidRequestException("Invalid X-Clinic-ID: " + clinicId);
        }

        return RequestPartitionId.fromPartitionId(partitionId);
    }

    @Hook(Pointcut.STORAGE_PARTITION_IDENTIFY_CREATE)
    public RequestPartitionId identifyCreate(RequestDetails requestDetails) {
        // Same logic as read
        return identifyRead(requestDetails);
    }
}
```

---

## Cross-Partition References

Enabled via `allow_references_across_partitions: true` for:
- Shared Medication definitions
- Organization hierarchies
- System-level CodeSystems

**Caution:** Patient data MUST remain partition-isolated for HIPAA compliance.

---

## Database Tables

Key partition-related tables:

| Table | Purpose |
|-------|---------|
| `HFJ_PARTITION` | Partition definitions |
| `HFJ_RESOURCE` | FHIR resources (includes PARTITION_ID column) |
| `HFJ_RES_LINK` | Cross-resource references |
| `HFJ_SPIDX_*` | Search parameter indexes |

---

## Verifying Partition Data

```sql
-- Check partition definitions
SELECT * FROM HFJ_PARTITION;

-- Count resources per partition
SELECT PARTITION_ID, RES_TYPE, COUNT(*)
FROM HFJ_RESOURCE
GROUP BY PARTITION_ID, RES_TYPE
ORDER BY PARTITION_ID;

-- Find resources in specific partition
SELECT RES_ID, RES_TYPE, RES_UPDATED
FROM HFJ_RESOURCE
WHERE PARTITION_ID = 1
LIMIT 10;
```
