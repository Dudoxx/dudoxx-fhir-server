# HAPI FHIR Server Architecture

**Version:** 1.0.0
**Date:** December 8, 2025
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

The `ClinicPartitionInterceptor` class enforces tenant isolation by routing requests to the appropriate partition based on the `X-Clinic-ID` header.

```java
public class ClinicPartitionInterceptor {
    private static final Map<String, Integer> CLINIC_PARTITION_MAP = Map.of(
        "ddx-hamburg-clinic", 1,
        "ddx-berlin-clinic", 2,
        "ddx-munich-clinic", 3,
        "ddx-frankfurt-clinic", 4,
        "ddx-cologne-clinic", 5
    );

    @Hook(Pointcut.STORAGE_PARTITION_IDENTIFY_READ)
    public RequestPartitionId identifyRead(RequestDetails requestDetails) {
        String clinicId = requestDetails.getHeader("X-Clinic-ID");
        Integer partitionId = CLINIC_PARTITION_MAP.get(clinicId);
        return RequestPartitionId.fromPartitionId(partitionId);
    }

    @Hook(Pointcut.STORAGE_PARTITION_IDENTIFY_CREATE)
    public RequestPartitionId identifyCreate(RequestDetails requestDetails, IBaseResource resource) {
        String clinicId = requestDetails.getHeader("X-Clinic-ID");
        Integer partitionId = CLINIC_PARTITION_MAP.get(clinicId);
        return RequestPartitionId.fromPartitionId(partitionId);
    }
}
```

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

### PostgreSQL Partitioning

- **Single Database:** All partitions reside in `ddx_hapifhir` database
- **Schema Separation:** Each partition has isolated tables
- **Connection Pooling:** HikariCP manages connections
- **Indexing:** Optimized for FHIR search parameters

### Key Tables

- `HFJ_RESOURCE` - FHIR resources
- `HFJ_RES_VER` - Resource versions
- `HFJ_PARTITION` - Partition metadata
- `HFJ_RES_LINK` - Resource references
- `HFJ_SPIDX_*` - Search parameter indexes

## Customizations Beyond Standard HAPI FHIR

### Dudoxx Extensions

1. **Multi-Tenant Partitioning:** Clinic-specific data isolation
2. **MCP AI Integration:** AI-powered FHIR operations
3. **CDS Hooks Support:** Clinical decision support framework
4. **Custom Interceptors:** Enhanced security and routing
5. **Binary Storage:** MinIO integration for attachments
6. **Audit Logging:** Comprehensive operation tracking

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
**Last Updated:** December 8, 2025
