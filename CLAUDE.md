# HAPI FHIR Server - Claude Context

**Version:** 1.1.0 | **Port:** 8080 | **Owner:** Dudoxx UG

## Repo Policy

- Default branch: `main`
- This repository is used as a submodule of `dudoxx-hapifihr` (see `../.gitmodules`)

---

## Quick Reference

| Item | Value |
|------|-------|
| Tech | HAPI FHIR 8.4.0, Java 17+, Spring Boot, PostgreSQL |
| Database | `ddx_fhir_core` on port 5432 |
| Auth | Bearer `ddx-api-token-2024` + `X-Clinic-ID` header |
| Called By | NestJS (4100) only |
| NEVER Called By | Browser, Next.js |

---

## ⚠️ CRITICAL: Internal Service Only

```
❌ Browser → HAPI FHIR (FORBIDDEN)
❌ Next.js → HAPI FHIR (FORBIDDEN)
✅ NestJS → HAPI FHIR (ALLOWED)
```

---

## Multi-Tenancy (Partitions)

| Clinic ID | Partition |
|-----------|-----------|
| `ddx-hamburg-clinic` | 1 |
| `ddx-berlin-clinic` | 2 |
| `ddx-munich-clinic` | 3 |
| `ddx-frankfurt-clinic` | 4 |
| `ddx-cologne-clinic` | 5 |
| `ddx-shared-clinic` | 6 |
| `default` | 0 |

**Required Headers:**
```bash
Authorization: Bearer ddx-api-token-2024
X-Clinic-ID: ddx-hamburg-clinic
```

---

## Key Files

| File | Purpose |
|------|---------|
| `src/main/resources/application.yaml` | Server config |
| `src/.../interceptor/ClinicPartitionInterceptor.java` | Partition routing |
| `src/.../interceptor/ApiTokenAuthInterceptor.java` | Auth validation |
| `src/main/resources/init-partitions.sql` | Partition setup |

---

## Common Commands

```bash
# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run

# Test endpoints
curl http://localhost:8080/fhir/metadata
curl -H "Authorization: Bearer ddx-api-token-2024" \
     -H "X-Clinic-ID: ddx-hamburg-clinic" \
     http://localhost:8080/fhir/Patient
```

---

## MCP Tools (AI Integration)

9 tools available: `fhir_search`, `fhir_create`, `fhir_read`, `fhir_update`, `fhir_delete`, `fhir_history`, `fhir_validate`, `fhir_batch`, `fhir_capabilities`

---

## 📚 Extended Documentation

| Document | When to Read |
|----------|--------------|
| [CLAUDE_PARTITIONS.md](./CLAUDE_PARTITIONS.md) | Adding clinics, partition issues |
| [CLAUDE_TROUBLESHOOTING.md](./CLAUDE_TROUBLESHOOTING.md) | Errors, debugging |
| [DUDOXX_CUSTOMIZATIONS.md](./DUDOXX_CUSTOMIZATIONS.md) | Custom modifications |
| [UPSTREAM_SYNC.md](./UPSTREAM_SYNC.md) | Syncing with HAPI upstream |

---

## 🔧 DUAL DATABASE ARCHITECTURE (QUICK REMINDER)

### Two PostgreSQL Datasources

HAPI FHIR is configured with **two separate PostgreSQL connections**:

| Datasource | Database | Purpose | Pool |
|------------|----------|---------|------|
| **Primary** | `ddx_fhir_core` | FHIR resources (hfj_* tables) | HapiFhirHikariPool (10 conn) |
| **Tenant** | `ddx_api_main` | Read org configs from NestJS | TenantRegistryPool (3 conn) |

### Configuration Files

| File | Purpose |
|------|---------|
| `FhirDataSourceConfig.java` | @Primary bean for HAPI FHIR JPA persistence |
| `TenantDataSourceConfig.java` | Secondary bean for reading tenant registry |
| `TenantRegistryService.java` | Reads `organizations` table, caches slug→partition |
| `TenantAdminController.java` | REST API for cache invalidation |

### How Tenant Isolation Works

```
1. NestJS creates organization in ddx_api_main (with fhirPartitionId)
2. NestJS calls POST /admin/tenants/refresh on HAPI FHIR
3. TenantRegistryService reads organizations table
4. ClinicPartitionInterceptor routes requests to partition via X-Clinic-ID header
```

### Tenant Admin REST API

All endpoints require: `Authorization: Bearer ddx-api-token-2024`

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/admin/tenants/refresh` | Force reload all tenants from DB |
| POST | `/admin/tenants/register` | Register single tenant (body: `{slug, partitionId}`) |
| GET | `/admin/tenants` | List all cached tenants |
| GET | `/admin/tenants/{slug}` | Check if tenant exists |
| DELETE | `/admin/tenants/{slug}` | Remove tenant from cache |
| GET | `/admin/tenants/health` | Health check (no auth) |

**Example - Trigger Cache Refresh from NestJS:**
```bash
curl -X POST http://localhost:8080/admin/tenants/refresh \
  -H "Authorization: Bearer ddx-api-token-2024"
```

### Environment Variables (dotenv)

HAPI FHIR uses **spring-dotenv** to load `.env` from project root.

**Config:** `src/main/resources/.env.properties`
```properties
directory=./
filename=.env
ignoreIfMissing=false
```

**Required .env Variables:**
```bash
# Primary FHIR Database
PG_HOST=localhost
PG_PORT=5432
PG_DATABASE=ddx_fhir_core
PG_USER=dudoxx_user
PG_PASSWORD=admin

# Tenant Registry Database (NestJS)
PG_DATABASE_TENANT=ddx_api_main
```

### Database Tables

**ddx_fhir_core (58 tables):**
- `hfj_partition` - Partition definitions
- `hfj_resource` - All FHIR resources
- `hfj_res_*` - Resource indices

**ddx_api_main (read-only by FHIR):**
- `organizations` - slug, fhirPartitionId, isActive
- `global_config` - key/value settings
- `partition_sequence` - next partition ID

### Auto-Refresh Behavior

`TenantRegistryService` refreshes every 60 seconds (configurable via `hapi.fhir.tenant.cache_refresh_seconds`).

On cache miss, it performs ONE immediate DB refresh before returning null.

---

**Last Updated:** December 12, 2025
