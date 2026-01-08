# HAPI FHIR Server - Critical Paths & Important Files

**Version:** 6.0.0 | **Date:** December 12, 2025 | **Port:** 8080

**Author:** Walid Boudabbous, Founder and CTO of Dudoxx UG, CEO of Acceleate.com

---

## ⚠️ CRITICAL: Internal Service Only

```
❌ Browser → HAPI FHIR (FORBIDDEN)
❌ Next.js → HAPI FHIR (FORBIDDEN)
✅ NestJS (4100) → HAPI FHIR (ALLOWED)
```

HAPI FHIR is an **internal backend service** that should NEVER be exposed directly to browsers or frontend applications.

---

## Key Files & Directories

### 1. Configuration Files

| File | Path | Purpose | Modify? |
|------|------|---------|---------|
| **application.yaml** | `src/main/resources/` | Main server configuration | ✅ Yes |
| **.env** | `ddx-fhir/` | Environment variables | ✅ Yes |
| **.env.properties** | `src/main/resources/` | Dotenv loader config | ⚠️ Rarely |
| **logback.xml** | `src/main/resources/` | Logging configuration | ⚠️ Rarely |
| **pom.xml** | `ddx-fhir/` | Maven dependencies | ⚠️ Rarely |

### 2. Source Code (Java)

| File | Path | Purpose | Critical? |
|------|------|---------|-----------|
| **Application.java** | `ca.uhn.fhir.jpa.starter.` | Spring Boot entry point | ❌ No |
| **ApiTokenAuthInterceptor.java** | `ca.uhn.fhir.jpa.starter.interceptor.` | Bearer token authentication | ✅ **YES** |
| **ClinicPartitionInterceptor.java** | `ca.uhn.fhir.jpa.starter.interceptor.` | Multi-tenant partition routing | ✅ **YES** |
| **TenantRegistryService.java** | `ca.uhn.fhir.jpa.starter.tenant.` | Dynamic tenant management | ✅ **YES** |
| **TenantAdminController.java** | `ca.uhn.fhir.jpa.starter.tenant.` | Tenant admin REST API | ✅ **YES** |
| **FhirDataSourceConfig.java** | `ca.uhn.fhir.jpa.starter.datasource.` | Primary FHIR database config | ✅ **YES** |
| **TenantDataSourceConfig.java** | `ca.uhn.fhir.jpa.starter.tenant.` | Secondary tenant database config | ✅ **YES** |

### 3. Database Scripts

| File | Path | Purpose | Run When? |
|------|------|---------|-----------|
| **init-partitions.sql** | `src/main/resources/` | Initialize 7 partitions | First install |
| **sync-partitions.sql** | `src/main/resources/` | Sync partitions from NestJS | As needed |

### 4. Documentation

| File | Path | Purpose |
|------|------|---------|
| **README.md** | `ddx-fhir/` | Project overview |
| **CLAUDE.md** | `ddx-fhir/` | Quick reference for AI |
| **INSTALL.md** | `ddx-fhir/` | Installation guide |
| **IMPORTANT.md** | `ddx-fhir/` | This file |
| **ENV_VARIABLES.md** | `ddx-fhir/` | Environment variables |
| **CLAUDE_PARTITIONS.md** | `ddx-fhir/` | Partition management |
| **CLAUDE_TROUBLESHOOTING.md** | `ddx-fhir/` | Debugging guide |
| **UPSTREAM_SYNC.md** | `ddx-fhir/` | HAPI FHIR upstream sync |

---

## Multi-Tenancy Architecture

### Partition System

HAPI FHIR uses **partition-based multi-tenancy** for complete data isolation between clinics.

#### Partition Mapping

| Partition ID | Name | Clinic ID | Database |
|--------------|------|-----------|----------|
| 0 | DEFAULT | default | System partition |
| 1 | HAMBURG | ddx-hamburg-clinic | ddx_fhir_core |
| 2 | BERLIN | ddx-berlin-clinic | ddx_fhir_core |
| 3 | MUNICH | ddx-munich-clinic | ddx_fhir_core |
| 4 | FRANKFURT | ddx-frankfurt-clinic | ddx_fhir_core |
| 5 | COLOGNE | ddx-cologne-clinic | ddx_fhir_core |
| 6 | SHARED | ddx-shared-clinic | ddx_fhir_core |

#### How It Works

```
1. NestJS sends request to HAPI FHIR with headers:
   - Authorization: Bearer ddx-api-token-2024
   - X-Clinic-ID: ddx-hamburg-clinic

2. ApiTokenAuthInterceptor validates Bearer token

3. ClinicPartitionInterceptor:
   - Reads X-Clinic-ID header
   - Looks up partition ID from TenantRegistryService
   - Sets partition context for request

4. HAPI FHIR JPA layer:
   - All queries automatically filtered by partition
   - Resources stored with PARTITION_ID column
   - Complete data isolation
```

### Dynamic Tenant Management

HAPI FHIR can read tenant configurations from the NestJS database (`ddx_api_main`):

```
NestJS (ddx_api_main.organizations) → HAPI FHIR (TenantRegistryService) → Partition Routing
```

**Admin API Endpoints:**

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/admin/tenants/refresh` | Reload all tenants from database |
| POST | `/admin/tenants/register` | Register single tenant |
| GET | `/admin/tenants` | List all cached tenants |
| GET | `/admin/tenants/{slug}` | Check tenant existence |
| DELETE | `/admin/tenants/{slug}` | Remove tenant from cache |
| GET | `/admin/tenants/health` | Health check (no auth) |

---

## FHIR Resources Supported

### Core Clinical Resources

| Resource | Path | Purpose |
|----------|------|---------|
| **Patient** | `/fhir/Patient` | Patient demographics |
| **Practitioner** | `/fhir/Practitioner` | Healthcare providers |
| **Observation** | `/fhir/Observation` | Clinical observations |
| **Condition** | `/fhir/Condition` | Diagnoses and problems |
| **MedicationRequest** | `/fhir/MedicationRequest` | Prescriptions |
| **Appointment** | `/fhir/Appointment` | Scheduled appointments |
| **Encounter** | `/fhir/Encounter` | Patient visits |
| **DiagnosticReport** | `/fhir/DiagnosticReport` | Lab results |
| **Procedure** | `/fhir/Procedure` | Performed procedures |
| **AllergyIntolerance** | `/fhir/AllergyIntolerance` | Patient allergies |

### Administrative Resources

| Resource | Path | Purpose |
|----------|------|---------|
| **Organization** | `/fhir/Organization` | Clinics and facilities |
| **Location** | `/fhir/Location` | Physical locations |
| **HealthcareService** | `/fhir/HealthcareService` | Available services |
| **Schedule** | `/fhir/Schedule` | Provider schedules |
| **Slot** | `/fhir/Slot` | Available time slots |

### All Resources

HAPI FHIR R4 supports **145 FHIR resource types**. See: http://hl7.org/fhir/R4/resourcelist.html

---

## Security Configuration

### 1. Authentication

**Type:** Bearer Token (API Key)

```yaml
# application.yaml
hapi:
  fhir:
    auth:
      enabled: true
      api_token: ddx-api-token-2024
```

**Headers Required:**

```bash
Authorization: Bearer ddx-api-token-2024
X-Clinic-ID: ddx-hamburg-clinic
```

### 2. Public Endpoints (No Auth)

| Endpoint | Purpose |
|----------|---------|
| `/fhir/metadata` | Capability statement |
| `/actuator/health` | Health check |
| `/admin/tenants/health` | Tenant health check |

### 3. Security Best Practices

#### Production Setup

1. **Use Strong API Tokens**
   ```bash
   # Generate secure token
   openssl rand -hex 32
   # Store in environment variable
   export FHIR_API_TOKEN="<generated-token>"
   ```

2. **Enable SSL/TLS**
   ```yaml
   server:
     ssl:
       enabled: true
       key-store: classpath:keystore.p12
       key-store-password: <password>
   ```

3. **Implement OAuth 2.0** (Future)
   - Replace Bearer tokens with OAuth 2.0 / OpenID Connect
   - Use Keycloak, Auth0, or similar
   - Support JWT tokens

4. **Network Security**
   - Use internal network only (no public internet)
   - Configure firewall rules
   - Implement VPN/VPC isolation

#### Database Security

1. **Strong Passwords**
   ```bash
   # Generate database password
   openssl rand -base64 32
   ```

2. **Limited Privileges**
   ```sql
   -- Revoke superuser
   ALTER USER dudoxx_user WITH NOSUPERUSER;
   
   -- Grant specific privileges only
   GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO dudoxx_user;
   ```

3. **Connection Encryption**
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/ddx_fhir_core?sslmode=require
   ```

---

## Performance Tuning

### 1. Database Connection Pool

```yaml
# application.yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10        # Max connections
      minimum-idle: 5               # Min idle connections
      connection-timeout: 30000     # 30 seconds
      idle-timeout: 600000          # 10 minutes
      max-lifetime: 1800000         # 30 minutes
```

**Tuning Guidelines:**

- **Small deployments** (1-10 users): `maximum-pool-size: 5-10`
- **Medium deployments** (10-100 users): `maximum-pool-size: 10-20`
- **Large deployments** (100+ users): `maximum-pool-size: 20-50`

### 2. JPA/Hibernate Optimization

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20              # Batch inserts
        cache:
          use_query_cache: false      # Disable query cache
          use_second_level_cache: false  # Disable 2nd level cache
```

### 3. Search Optimization

```yaml
hapi:
  fhir:
    # Search thread pool
    search-coord-core-pool-size: 20
    search-coord-max-pool-size: 100
    search-coord-queue-capacity: 200
    
    # Cache settings
    retain_cached_searches_mins: 60
    reuse_cached_search_results_millis: 60000
```

### 4. JVM Tuning

```bash
# Set in environment or startup script
export JAVA_OPTS="-Xmx4g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run with optimized settings
java $JAVA_OPTS -jar target/ROOT.war
```

**Memory Guidelines:**

- **Development**: `-Xmx2g -Xms1g`
- **Production (small)**: `-Xmx4g -Xms2g`
- **Production (large)**: `-Xmx8g -Xms4g`

### 5. Monitoring & Metrics

```yaml
# Enable Actuator metrics
management:
  endpoints:
    web:
      exposure:
        include: "health,info,metrics,prometheus"
  endpoint:
    prometheus:
      enabled: true
```

**Access Metrics:**

- **Health**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
- **Prometheus**: http://localhost:8080/actuator/prometheus

---

## Common Issues & Solutions

### 1. Database Connection Errors

**Error:** `Cannot connect to database`

**Solution:**
```bash
# Check PostgreSQL is running
brew services list | grep postgresql  # macOS
systemctl status postgresql           # Linux

# Test connection
psql -U dudoxx_user -h localhost -p 5432 -d ddx_fhir_core

# Check .env file
cat ddx-fhir/.env
```

### 2. Partition Not Found

**Error:** `Partition with ID X not found`

**Solution:**
```bash
# Verify partitions exist
psql -U dudoxx_user -d ddx_fhir_core -c "SELECT * FROM hfj_partition ORDER BY part_id;"

# Re-run partition init
psql -U dudoxx_user -d ddx_fhir_core -f src/main/resources/init-partitions.sql

# Verify partition count (should be 7)
psql -U dudoxx_user -d ddx_fhir_core -c "SELECT COUNT(*) FROM hfj_partition;"
```

### 3. Authentication Failures

**Error:** `401 Unauthorized`

**Solution:**
```bash
# Verify token in request
curl -v -H "Authorization: Bearer ddx-api-token-2024" \
     http://localhost:8080/fhir/metadata

# Check application.yaml
grep api_token src/main/resources/application.yaml

# Check .env
grep FHIR_API_TOKEN ddx-fhir/.env
```

### 4. Port Already In Use

**Error:** `Port 8080 already in use`

**Solution:**
```bash
# Find process using port 8080
lsof -i :8080

# Kill process (if safe)
kill -9 <PID>

# Or change port in application.yaml
server:
  port: 8081
```

### 5. Out of Memory

**Error:** `java.lang.OutOfMemoryError: Java heap space`

**Solution:**
```bash
# Increase heap size
export JAVA_OPTS="-Xmx4g -Xms2g"

# Or in Maven
export MAVEN_OPTS="-Xmx4g -Xms2g"

# Run with increased memory
mvn spring-boot:run
```

### 6. Slow Search Performance

**Issue:** FHIR searches taking too long

**Solution:**
```sql
-- Add database indices
CREATE INDEX idx_patient_family ON patient_names(family_name);
CREATE INDEX idx_patient_given ON patient_names(given_name);

-- Vacuum database
VACUUM ANALYZE;

-- Check query performance
EXPLAIN ANALYZE SELECT * FROM hfj_resource WHERE res_type = 'Patient';
```

---

## Maintenance Tasks

### 1. Database Backups

```bash
# Backup FHIR database
pg_dump -U dudoxx_user -h localhost -d ddx_fhir_core > backup_$(date +%Y%m%d).sql

# Backup with compression
pg_dump -U dudoxx_user -h localhost -d ddx_fhir_core | gzip > backup_$(date +%Y%m%d).sql.gz

# Restore backup
psql -U dudoxx_user -h localhost -d ddx_fhir_core < backup_20251212.sql
```

### 2. Log Rotation

```bash
# Logs location (daemon mode)
ddx-fhir/fhir-server.log

# Rotate logs (weekly)
mv fhir-server.log fhir-server.log.$(date +%Y%m%d)
touch fhir-server.log

# Or use logrotate (Linux)
# See logback.xml for built-in rotation
```

### 3. Partition Management

```bash
# Add new clinic partition
psql -U dudoxx_user -d ddx_fhir_core <<EOF
INSERT INTO hfj_partition (part_id, part_name, part_desc)
VALUES (7, 'STUTTGART', 'Stuttgart Clinic')
ON CONFLICT (part_id) DO NOTHING;
EOF

# Refresh tenant cache
curl -X POST http://localhost:8080/admin/tenants/refresh \
  -H "Authorization: Bearer ddx-api-token-2024"
```

### 4. Clear Cached Searches

```sql
-- Clear old cached searches (older than 1 hour)
DELETE FROM hfj_search WHERE search_created < NOW() - INTERVAL '1 hour';
```

---

## Emergency Procedures

### 1. Server Won't Start

```bash
# Check logs
cat ddx-fhir/fhir-server.log

# Check database connection
psql -U dudoxx_user -h localhost -p 5432 -d ddx_fhir_core

# Clean build
cd ddx-fhir
mvn clean
rm -rf target/
mvn package -DskipTests

# Start fresh
./06_ddx-start-fhir.sh
```

### 2. Data Corruption

```bash
# Stop server
./stop-server.sh

# Restore from backup
psql -U dudoxx_user -h localhost -d ddx_fhir_core < backup_latest.sql

# Verify partition integrity
psql -U dudoxx_user -d ddx_fhir_core -c "SELECT * FROM hfj_partition;"

# Start server
./06_ddx-start-fhir.sh
```

### 3. Performance Degradation

```bash
# Vacuum database
psql -U dudoxx_user -d ddx_fhir_core -c "VACUUM FULL ANALYZE;"

# Restart server
./stop-server.sh
./06_ddx-start-fhir.sh

# Monitor metrics
curl http://localhost:8080/actuator/metrics
```

---

## Support & Resources

### Documentation

- [Installation Guide](./INSTALL.md)
- [Environment Variables](./ENV_VARIABLES.md)
- [Troubleshooting](./CLAUDE_TROUBLESHOOTING.md)
- [Partition Management](./CLAUDE_PARTITIONS.md)

### External Resources

- **HAPI FHIR Docs**: https://hapifhir.io/
- **FHIR R4 Spec**: http://hl7.org/fhir/R4/
- **Community**: https://chat.fhir.org/

### Contact

- **Email**: support@dudoxx.com
- **Internal Docs**: See parent directory documentation

---

**Built with ❤️ by Dudoxx UG**  
**Powered by HAPI FHIR 8.4**
