# HAPI FHIR - Troubleshooting Guide

## Common Issues

### HAPI-1220: Partition Error

**Error:** `This server is not configured to support search against all partitions`

**Solution:** Verify `application.yaml`:
```yaml
hapi:
  fhir:
    partitioning:
      allow_references_across_partitions: true
```

See: `docs/troubleshooting/OFFICIAL_PARTITION_FIX.md`

---

### Port 8080 Already in Use

```bash
# Find process
lsof -i :8080

# Kill process
kill -9 <PID>

# Or use different port
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

---

### Database Connection Failed

```bash
# Test PostgreSQL
psql -U dudoxx_user -h localhost -p 5432 -d ddx_fhir_core

# Verify PostgreSQL is running
pg_isready -h localhost -p 5432

# Check credentials in application.yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ddx_fhir_core
    username: dudoxx_user
    password: admin
```

---

### Partition Not Found

```bash
# Check partitions exist
psql -U dudoxx_user -d ddx_fhir_core -c "SELECT * FROM HFJ_PARTITION;"

# Re-run initialization
psql -U dudoxx_user -d ddx_fhir_core -f src/main/resources/init-partitions.sql
```

---

### Invalid X-Clinic-ID Header

**Error:** `Invalid X-Clinic-ID: unknown-clinic`

**Solution:** Use valid clinic ID from partition map:
- `ddx-hamburg-clinic`
- `ddx-berlin-clinic`
- `ddx-munich-clinic`
- `ddx-frankfurt-clinic`
- `ddx-cologne-clinic`
- `ddx-shared-clinic`

---

### Authentication Failed

**Error:** `401 Unauthorized`

**Solution:** Include correct Bearer token:
```bash
curl -H "Authorization: Bearer ddx-api-token-2024" \
     -H "X-Clinic-ID: ddx-hamburg-clinic" \
     http://localhost:8080/fhir/Patient
```

---

### Out of Memory Error

```bash
# Increase heap size
export JAVA_OPTS="-Xmx4g -Xms2g"
mvn spring-boot:run
```

Or in `pom.xml`:
```xml
<configuration>
  <jvmArguments>-Xmx4g -Xms2g</jvmArguments>
</configuration>
```

---

### Slow Startup

HAPI FHIR indexing on first run can take 5-10 minutes. Monitor logs:
```bash
tail -f logs/hapi-fhir.log | grep -E "(Starting|Completed|Index)"
```

---

## Health Checks

```bash
# Basic health
curl http://localhost:8080/actuator/health

# FHIR metadata
curl http://localhost:8080/fhir/metadata

# With auth
curl -H "Authorization: Bearer ddx-api-token-2024" \
     http://localhost:8080/fhir/metadata
```

---

## Database Diagnostics

```sql
-- Check table sizes
SELECT
  schemaname,
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename))
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
LIMIT 10;

-- Resource counts by type
SELECT RES_TYPE, COUNT(*)
FROM HFJ_RESOURCE
GROUP BY RES_TYPE
ORDER BY COUNT(*) DESC;

-- Recent operations
SELECT RES_ID, RES_TYPE, RES_UPDATED
FROM HFJ_RESOURCE
ORDER BY RES_UPDATED DESC
LIMIT 20;
```

---

## Log Analysis

```bash
# Error logs
grep -i error logs/hapi-fhir.log | tail -50

# Slow queries
grep -i "slow" logs/hapi-fhir.log

# Partition operations
grep -i "partition" logs/hapi-fhir.log | tail -20
```
