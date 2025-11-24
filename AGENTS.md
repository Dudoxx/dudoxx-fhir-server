# AI Agent Configuration - HAPI FHIR Server

Project-specific guidance for `dudoxx-fhir-server` (HAPI FHIR JPA). Keep auth + partition interceptors intact and preserve partitioned data isolation.

**Version:** HAPI 8.4.0 / Java 17  
**Last Updated:** 2025-11-24

---

## Stack & Runtime Facts
- Spring Boot + HAPI FHIR JPA Server (R4) on port 8080, context path `/fhir`
- Database: PostgreSQL `ddx_hapifhir`
- Storage: MinIO (binary storage enabled)
- Auth: `ApiTokenAuthInterceptor` using Bearer token (`hapi.fhir.auth.api_token`, default `ddx-api-token-2024`)
- Tenancy: `ClinicPartitionInterceptor` with partition map (default=0, Hamburg=1, Berlin=2, Munich=3, Frankfurt=4, Cologne=5, Shared=6)
- Partitioning enabled with `allow_references_across_partitions: true`; subscriptions disabled to avoid HAPI-1220
- MCP server enabled under `/mcp/messages` (spring.ai.mcp)

---

## Security & Tenancy Rules
- Internal service only: callers are NestJS backend and ops scripts—never expose directly to the browser.
- Every non-public request must include `Authorization: Bearer <token>` and `X-Clinic-ID: ddx-*-clinic` (metadata/health endpoints are allowed without).
- Do not remove/disable `ApiTokenAuthInterceptor` or `ClinicPartitionInterceptor`. Keep partition defaults in `src/main/resources/application.yaml` and `init-partitions.sql` aligned.
- Keep `allow_references_across_partitions: true` and `default_partition_id: 0` to prevent startup/search errors.

---

## Dev Commands
```bash
# Run locally
mvn spring-boot:run

# Build
mvn clean package            # with tests
mvn clean package -DskipTests

# Health / smoke checks
curl http://localhost:8080/actuator/health
curl http://localhost:8080/fhir/metadata
curl -H "Authorization: Bearer ddx-api-token-2024" \
     -H "X-Clinic-ID: ddx-hamburg-clinic" \
     http://localhost:8080/fhir/Patient
```

---

## Files to Know
- `src/main/resources/application.yaml` — ports, auth token, partitioning, CORS
- `src/main/resources/init-partitions.sql` — partition bootstrap script (keep IDs/names in sync with interceptor)
- `src/main/java/ca/uhn/fhir/jpa/starter/interceptor/{ApiTokenAuthInterceptor,ClinicPartitionInterceptor}.java` — critical interceptors
- `DUDOXX_CUSTOMIZATIONS.md`, `UPSTREAM_SYNC.md`, `README.md` — local changes and sync notes

---

## Quality Gates
- ✅ Auth enforced (401 without token) and partitioning intact
- ✅ Metadata available at `/fhir/metadata`
- ✅ Build succeeds (`mvn clean package`)
- ✅ Partition map consistent between `application.yaml`, `init-partitions.sql`, and interceptor
- ✅ No subscriptions enabled (keeps partition warmup stable)

---

## Integration Path
- Callers: NestJS backend on `http://localhost:4100` using Bearer token + `X-Clinic-ID`
- External UI/clients should go through NestJS; do not expose HAPI endpoints directly
