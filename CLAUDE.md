# HAPI FHIR Server - Claude Context

**Version:** 1.1.0 | **Port:** 8080 | **Owner:** Dudoxx UG

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

**Last Updated:** December 11, 2025
