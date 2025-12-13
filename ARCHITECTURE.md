# HAPI FHIR Server Architecture

**Version:** 1.0.0
**Date:** December 12, 2025
**Owner:** Dudoxx UG

---

## Overview

The Dudoxx HAPI FHIR Server implements a multi-tenant architecture using PostgreSQL partitioning with strict data isolation enforced through custom interceptors. The server provides FHIR R4 REST API capabilities with AI integration via MCP (Model Context Protocol).

## Multi-Tenancy Architecture

### Partition System

The server uses HAPI FHIR's built-in partitioning feature to isolate data between clinics:

| Clinic ID | Partition ID | Clinic Name | Location |
|-----------|--------------|-------------|----------|
| `default` | 0 | System Default | N/A |
| `ddx-hamburg-clinic` | 1 | Hamburg Clinic | Hamburg, Germany |
| `ddx-berlin-clinic` | 2 | Berlin Clinic | Berlin, Germany |
| `ddx-munich-clinic` | 3 | Munich Clinic | Munich, Germany |
| `ddx-frankfurt-clinic` | 4 | Frankfurt Clinic | Frankfurt, Germany |
| `ddx-cologne-clinic` | 5 | Cologne Clinic | Cologne, Germany |

### ClinicPartitionInterceptor

The `ClinicPartitionInterceptor` class enforces tenant isolation by routing requests to the appropriate partition based on the `X-Clinic-ID` header. It uses the `TenantRegistryService` for dynamic partition lookup.

```java
@Component
@Interceptor
public class ClinicPartitionInterceptor {

    @Autowired
    private TenantRegistryService tenantRegistry;

    @Hook(Pointcut.STORAGE_PARTITION_IDENTIFY_READ)
    public RequestPartitionId identifyPartitionForRead(RequestDetails theRequestDetails) {
        // System operations - use DEFAULT partition
        if (theRequestDetails == null || theRequestDetails instanceof SystemRequestDetails) {
            return RequestPartitionId.defaultPartition();
        }

        // Extract clinic ID and lookup partition dynamically
        String clinicId = extractClinicId(theRequestDetails);
        if (clinicId == null) {
            return RequestPartitionId.defaultPartition();
        }

        Integer partitionId = tenantRegistry.getPartitionId(clinicId);
        if (partitionId == null) {
            throw new AuthenticationException("Unknown clinic ID: " + clinicId);
        }

        return RequestPartitionId.fromPartitionId(partitionId);
    }

    @Hook(Pointcut.STORAGE_PARTITION_IDENTIFY_CREATE)
    public RequestPartitionId identifyPartitionForCreate(RequestDetails theRequestDetails) {
        // Similar logic for create operations
        // ...
    }
}
```

### TenantRegistryService

The `TenantRegistryService` manages dynamic tenant-to-partition mappings by reading from the NestJS `ddx_api_main` database.

**Key Features**:
- Reads `organizations` table for active clinic mappings
- Maintains in-memory cache of `slug → fhirPartitionId`
- Auto-refreshes cache every 60 seconds (configurable)
- Supports dynamic tenant creation without server restart
- Provides REST API for manual cache management

**Database Tables Used** (from `ddx_api_main`):
```sql
-- organizations table
CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    slug VARCHAR(255) UNIQUE NOT NULL,
    "fhirPartitionId" INTEGER,
    "isActive" BOOLEAN DEFAULT true
);

-- global_config table
CREATE TABLE global_config (
    key VARCHAR(255) PRIMARY KEY,
    value TEXT
);

-- partition_sequence table
CREATE TABLE partition_sequence (
    nextval INTEGER DEFAULT 1
);
```

**Tenant Registration Flow**:
1. NestJS creates organization with `fhirPartitionId` in `ddx_api_main`
2. NestJS calls `POST /admin/tenants/refresh` on HAPI FHIR
3. `TenantRegistryService` queries `organizations` table
4. Cache updated with new clinic mapping
5. `ClinicPartitionInterceptor` uses cached mapping for routing

### Partition Routing Flow

```
HTTP Request
    ↓
X-Clinic-ID Header Extracted
    ↓
ClinicPartitionInterceptor.identifyRead/Create()
    ↓
Partition ID Resolved (1-5)
    ↓
HAPI FHIR Routes to Clinic-Specific Partition
    ↓
PostgreSQL Partition Accessed
```

### Data Isolation Guarantees

- **Strict Isolation:** Each clinic's data is physically separated in PostgreSQL partitions
- **No Cross-Contamination:** Resources cannot reference across partitions
- **Header-Based Routing:** All requests must include valid `X-Clinic-ID` header
- **Fallback Handling:** Invalid clinic IDs result in access denied

## Security Architecture

### Authentication Sequence

1. **Bearer Token Validation:** `ApiTokenAuthInterceptor` validates `Authorization: Bearer ddx-api-token-2024`
2. **Clinic ID Verification:** `ClinicPartitionInterceptor` ensures valid `X-Clinic-ID` header
3. **Partition Routing:** Request routed to appropriate clinic partition
4. **Resource Access:** FHIR operations performed within isolated partition

### Interceptor Chain

```
Request → ApiTokenAuthInterceptor → ClinicPartitionInterceptor → HAPI FHIR Core → Database
```

## AI Integration Architecture

### MCP Server Integration

The server integrates with AI agents through three MCP bridge classes:

- **McpFhirBridge:** Core FHIR CRUD operations
- **McpCdsBridge:** Clinical Decision Support integration
- **McpBridge:** Base MCP protocol implementation

### MCP Tools Available

1. `fhir_search` - Advanced FHIR resource searching
2. `fhir_create` - Resource creation
3. `fhir_read` - Resource retrieval by ID
4. `fhir_update` - Resource modification
5. `fhir_delete` - Resource deletion
6. `fhir_history` - Version history access
7. `fhir_validate` - FHIR compliance validation
8. `fhir_batch` - Batch operations
9. `fhir_capabilities` - Server capability statement

## Database Architecture

### Dual Database Configuration

HAPI FHIR uses **two separate PostgreSQL databases**:

#### Primary Database: `ddx_fhir_core`
- **Purpose:** FHIR resource storage and retrieval
- **Connection Pool:** HapiFhirHikariPool (10 connections)
- **Tables:** 58 HAPI FHIR tables with partition support
- **Access:** Read/Write via HAPI FHIR JPA

**Key Tables:**
- `HFJ_RESOURCE` - FHIR resources (with PARTITION_ID)
- `HFJ_RES_VER` - Resource versions
- `HFJ_PARTITION` - Partition metadata
- `HFJ_RES_LINK` - Resource references
- `HFJ_SPIDX_*` - Search parameter indexes

#### Secondary Database: `ddx_api_main`
- **Purpose:** Tenant registry (managed by NestJS)
- **Connection Pool:** TenantRegistryPool (3 connections)
- **Tables:** organizations, global_config, partition_sequence
- **Access:** Read-only by HAPI FHIR

**Key Tables:**
- `organizations` - Clinic slugs and partition mappings
- `global_config` - System-wide configuration
- `partition_sequence` - Next available partition ID

### PostgreSQL Partitioning

- **Logical Partitions:** Data isolated by PARTITION_ID column in all FHIR tables
- **Physical Storage:** Single database with partition-aware queries
- **Connection Pooling:** Separate HikariCP pools for each datasource
- **Indexing:** Optimized for FHIR search parameters with partition pruning

## Customizations Beyond Standard HAPI FHIR

### Dudoxx Extensions

1. **Dynamic Multi-Tenant Partitioning:** Clinic-specific data isolation via database-backed registry
2. **Tenant Registry Service:** Dual database architecture for dynamic tenant management
3. **MCP AI Integration:** AI-powered FHIR operations via Model Context Protocol
4. **CDS Hooks Support:** Clinical decision support framework
5. **Custom Interceptors:** Enhanced security and dynamic partition routing
6. **Binary Storage:** MinIO integration for attachments (configurable)
7. **Audit Logging:** Comprehensive operation tracking

**Key Differences from Standard HAPI FHIR**:
- Dynamic tenant registration without server restart
- Database-backed tenant registry (reads from NestJS database)
- Dual datasource configuration (FHIR + Tenant registry)
- REST API for tenant cache management (`/admin/tenants/*`)
- Automatic cache refresh every 60 seconds
- Support for subdomain and URL path-based tenant identification

### Configuration Files

- `application.yaml` - Main server configuration
- `application-cds.yaml` - CDS Hooks settings
- `init-partitions.sql` - Partition initialization
- `mdm-rules.json` - Master Data Management rules

## Deployment Architecture

### Containerized Deployment

```yaml
# Docker Compose Configuration
services:
  hapi-fhir:
    image: ddx-hapi-fhir:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    depends_on:
      - postgres
      - minio

  postgres:
    image: postgres:15
    ports:
      - "5432:5432"

  minio:
    image: minio/minio:latest
    ports:
      - "9000:9000"
```

### Health Checks

- **Application Health:** `/actuator/health`
- **Database Connectivity:** PostgreSQL connection validation
- **MCP Services:** AI integration status
- **Partition Integrity:** Data isolation verification

## Performance Considerations

### Optimization Strategies

1. **Connection Pooling:** HikariCP with optimized settings
2. **Indexing:** Comprehensive FHIR search parameter indexes
3. **Caching:** Redis integration for frequently accessed data
4. **Partition Pruning:** Query optimization for partitioned data
5. **Batch Operations:** Efficient bulk data processing

### Monitoring

- **Metrics:** Spring Boot Actuator endpoints
- **Logging:** Structured logging with correlation IDs
- **Tracing:** Request tracing across services
- **Alerts:** Automated alerts for performance degradation

---

**Maintained by:** Dudoxx UG
**Last Updated:** December 12, 2025
