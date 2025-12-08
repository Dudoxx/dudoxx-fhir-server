# Pitfalls to Avoid - HAPI FHIR Server

**Version:** 1.0.0
**Date:** December 8, 2025
**Owner:** Dudoxx UG

---

## Critical Configuration Issues

### ❌ Clinic ID UUID vs Slug Confusion

**Problem:** Using database UUIDs instead of clinic slugs in `X-Clinic-ID` header

**Wrong:**
```
X-Clinic-ID: 550e8400-e29b-41d4-a716-446655440000
```

**Correct:**
```
X-Clinic-ID: ddx-hamburg-clinic
```

**Impact:** Requests fail with "Invalid clinic ID" errors, data isolation breaks

**Prevention:**
- Always use clinic slug format: `ddx-{city}-clinic`
- Never use database UUIDs or internal IDs
- Validate clinic IDs against the known mapping in `ClinicPartitionInterceptor`

### ❌ Missing Partition Routing

**Problem:** Forgetting to configure partition interceptors properly

**Symptoms:**
- HAPI-1220: "This server is not configured to support search against all partitions"
- Data leakage between clinics
- Incorrect partition assignment

**Solution:**
```yaml
# application.yaml
hapi:
  fhir:
    partitioning:
      enabled: true
      allow_references_across_partitions: true
      partitioning_include_in_search_hashes: true
      default_partition_id: 0
```

**Prevention:**
- Always enable partitioning in production
- Test with multiple clinic IDs
- Verify interceptor registration in `StarterJpaConfig.java`

### ❌ MCP Configuration Issues

**Problem:** Incorrect MCP server setup causing AI integration failures

**Common Issues:**
- Missing MCP bridge classes
- Incorrect tool registration
- Authentication bypass in MCP endpoints

**Prevention:**
- Ensure all three MCP bridge classes are present:
  - `McpBridge.java`
  - `McpFhirBridge.java`
  - `McpCdsBridge.java`
- Register tools correctly in MCP server initialization
- Test MCP endpoints independently of AI agents

## Security Vulnerabilities

### ❌ Exposed Internal Endpoints

**Problem:** Accidentally exposing FHIR endpoints to browsers

**Wrong Configuration:**
```yaml
hapi:
  fhir:
    cors:
      allowed_origin: ["*"]  # DANGEROUS in production
```

**Correct Configuration:**
```yaml
hapi:
  fhir:
    cors:
      allowed_origin: []  # No direct browser access
```

**Impact:** Direct browser access to sensitive health data

**Prevention:**
- Never allow CORS from browsers
- All requests must go through NestJS backend
- Implement proper API gateway security

### ❌ Weak Bearer Token

**Problem:** Using default or weak API tokens

**Wrong:**
```
Authorization: Bearer ddx-api-token-2024  # Default token
```

**Prevention:**
- Change default token in production
- Use strong, randomly generated tokens
- Rotate tokens regularly
- Store tokens securely (not in version control)

### ❌ Cross-Partition Data Access

**Problem:** Allowing references across clinic partitions

**Wrong Configuration:**
```yaml
hapi:
  fhir:
    partitioning:
      allow_references_across_partitions: true  # Can be dangerous
```

**When to Allow:**
- Only for system-level operations
- Never for patient data
- Always audit cross-partition access

## Database Issues

### ❌ Connection Pool Exhaustion

**Problem:** HikariCP pool exhaustion under load

**Symptoms:**
- Slow response times
- Connection timeout errors
- Database unavailability

**Prevention:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # Adjust based on load
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### ❌ Missing Partition Initialization

**Problem:** Starting server without running partition SQL

**Error:** "Partition not found" exceptions

**Prevention:**
- Always run `init-partitions.sql` on first startup
- Include in Docker initialization scripts
- Verify partitions exist before deployment

### ❌ Incorrect Dialect Configuration

**Problem:** Wrong Hibernate dialect for PostgreSQL

**Wrong:**
```yaml
jpa:
  properties:
    hibernate:
      dialect: org.hibernate.dialect.PostgreSQLDialect  # Too generic
```

**Correct:**
```yaml
jpa:
  properties:
    hibernate:
      dialect: ca.uhn.fhir.jpa.model.dialect.HapiFhirPostgresDialect
```

## Performance Pitfalls

### ❌ Unindexed Search Parameters

**Problem:** Slow queries on unindexed FHIR search parameters

**Impact:** Query timeouts, poor user experience

**Prevention:**
- Index commonly searched parameters
- Monitor slow query logs
- Use `_include` and `_revinclude` judiciously

### ❌ Large Result Sets

**Problem:** Returning thousands of resources without pagination

**Wrong:**
```
GET /fhir/Patient  # Returns all patients
```

**Correct:**
```
GET /fhir/Patient?_count=50&_offset=0
```

**Prevention:**
- Always implement pagination
- Set reasonable default page sizes
- Use token-based pagination for large datasets

### ❌ Inefficient Batch Operations

**Problem:** Large batch bundles causing memory issues

**Prevention:**
- Limit batch size to 100-500 entries
- Process batches asynchronously
- Monitor memory usage during batch operations

## Development Issues

### ❌ Missing Test Clinic IDs

**Problem:** Testing with invalid or missing clinic IDs

**Prevention:**
- Use valid test clinic IDs in all tests
- Test with multiple clinic partitions
- Include clinic ID validation in test setup

### ❌ Ignoring Interceptor Order

**Problem:** Incorrect interceptor execution order

**Critical Order:**
1. `ApiTokenAuthInterceptor` (authentication)
2. `ClinicPartitionInterceptor` (routing)

**Prevention:**
- Register interceptors in correct order
- Test authentication + routing together
- Document interceptor dependencies

### ❌ Outdated Dependencies

**Problem:** Using incompatible HAPI FHIR versions

**Prevention:**
- Keep HAPI FHIR version consistent across services
- Test compatibility before upgrades
- Follow HAPI FHIR migration guides

## Operational Issues

### ❌ Log Configuration Problems

**Problem:** Missing or incorrect logging configuration

**Prevention:**
- Configure structured logging in `logback.xml`
- Include correlation IDs for request tracing
- Set appropriate log levels for production

### ❌ Health Check Failures

**Problem:** Unhealthy services due to misconfiguration

**Prevention:**
- Test all health endpoints after deployment
- Monitor database connectivity
- Include partition health checks

### ❌ Backup Strategy Gaps

**Problem:** Incomplete backup coverage

**Prevention:**
- Backup PostgreSQL data with partitions
- Backup MinIO binary storage
- Test restore procedures regularly
- Include configuration files in backups

## Migration Pitfalls

### ❌ Schema Changes Without Testing

**Problem:** Breaking changes during HAPI FHIR upgrades

**Prevention:**
- Test migrations in staging environment
- Backup before schema changes
- Verify data integrity after migration

### ❌ Configuration Drift

**Problem:** Different configurations between environments

**Prevention:**
- Use configuration management
- Validate configurations on startup
- Document environment differences

---

## Quick Reference Checklist

### Pre-Deployment
- [ ] Clinic IDs use slug format (not UUIDs)
- [ ] Partitioning enabled and configured
- [ ] Bearer token changed from default
- [ ] CORS disabled for browsers
- [ ] MCP bridges properly registered
- [ ] Database connections configured
- [ ] Partitions initialized
- [ ] Health checks passing

### Post-Deployment
- [ ] Test with multiple clinic IDs
- [ ] Verify data isolation
- [ ] Check MCP tool functionality
- [ ] Monitor performance metrics
- [ ] Validate backup procedures

---

**Maintained by:** Dudoxx UG
**Last Updated:** December 8, 2025
