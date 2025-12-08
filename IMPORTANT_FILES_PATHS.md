# Important Files and Paths - HAPI FHIR Server

**Version:** 1.0.0
**Date:** December 8, 2025
**Owner:** Dudoxx UG

---

## Configuration Files

### Core Configuration

| File | Path | Purpose | Critical Notes |
|------|------|---------|---------------|
| `application.yaml` | `src/main/resources/application.yaml` | Main server configuration | Contains database, FHIR settings, partitioning config |
| `application-cds.yaml` | `src/main/resources/application-cds.yaml` | CDS Hooks configuration | Clinical Decision Support settings |
| `logback.xml` | `src/main/resources/logback.xml` | Logging configuration | Structured logging setup |
| `mdm-rules.json` | `src/main/resources/mdm-rules.json` | Master Data Management rules | Patient matching and deduplication rules |

### Database and Initialization

| File | Path | Purpose | Critical Notes |
|------|------|---------|---------------|
| `init-partitions.sql` | `src/main/resources/init-partitions.sql` | Partition initialization | Creates clinic partitions on startup |
| `pom.xml` | `pom.xml` | Maven dependencies | Java dependencies and build configuration |

### Deployment and Infrastructure

| File | Path | Purpose | Critical Notes |
|------|------|---------|---------------|
| `Dockerfile` | `Dockerfile` | Container build | Multi-stage build for HAPI FHIR |
| `docker-compose.yml` | `docker-compose.yml` | Local development stack | PostgreSQL, MinIO, Redis services |
| `start-hapi-fixed.sh` | `start-hapi-fixed.sh` | Startup script | Production server startup |

## Java Classes - Core Components

### Interceptors (Security & Routing)

| Class | Path | Purpose | Hooks Used |
|-------|------|---------|------------|
| `ApiTokenAuthInterceptor` | `src/main/java/ca/uhn/fhir/jpa/starter/interceptor/ApiTokenAuthInterceptor.java` | Bearer token authentication | `@Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)` |
| `ClinicPartitionInterceptor` | `src/main/java/ca/uhn/fhir/jpa/starter/interceptor/ClinicPartitionInterceptor.java` | Multi-tenant routing | `@Hook(Pointcut.STORAGE_PARTITION_IDENTIFY_READ/CREATE)` |

### MCP Bridge Classes (AI Integration)

| Class | Path | Purpose | Tools Provided |
|-------|------|---------|---------------|
| `McpBridge` | `src/main/java/ca/uhn/fhir/rest/server/McpBridge.java` | Base MCP protocol | Core MCP communication |
| `McpFhirBridge` | `src/main/java/ca/uhn/fhir/rest/server/McpFhirBridge.java` | FHIR operations | CRUD operations, search, validation |
| `McpCdsBridge` | `src/main/java/ca/uhn/fhir/rest/server/McpCdsBridge.java` | CDS integration | Clinical decision support |

### CDS Hooks Components

| Class | Path | Purpose | Key Methods |
|-------|------|---------|-------------|
| `CdsHooksConfigCondition` | `src/main/java/ca/uhn/fhir/jpa/starter/cdshooks/CdsHooksConfigCondition.java` | Configuration condition | Feature flag checking |
| `CdsHooksProperties` | `src/main/java/ca/uhn/fhir/jpa/starter/cdshooks/CdsHooksProperties.java` | CDS properties | Configuration binding |
| `CdsHooksRequest` | `src/main/java/ca/uhn/fhir/jpa/starter/cdshooks/CdsHooksRequest.java` | Request handling | Hook request processing |
| `CdsHooksServlet` | `src/main/java/ca/uhn/fhir/jpa/starter/cdshooks/CdsHooksServlet.java` | HTTP endpoint | `/cds-services` endpoint |
| `ErrorHandling` | `src/main/java/ca/uhn/fhir/jpa/starter/cdshooks/ErrorHandling.java` | Error management | Exception handling |
| `ModuleConfigurationPrefetchSvc` | `src/main/java/ca/uhn/fhir/jpa/starter/cdshooks/ModuleConfigurationPrefetchSvc.java` | Prefetch service | Data prefetching |
| `ProviderConfiguration` | `src/main/java/ca/uhn/fhir/jpa/starter/cdshooks/ProviderConfiguration.java` | Provider setup | CDS service registration |
| `StarterCdsHooksConfig` | `src/main/java/ca/uhn/fhir/jpa/starter/cdshooks/StarterCdsHooksConfig.java` | Main configuration | Spring configuration |

### Core Application Classes

| Class | Path | Purpose | Key Responsibilities |
|-------|------|---------|---------------------|
| `AppProperties` | `src/main/java/ca/uhn/fhir/jpa/starter/AppProperties.java` | Application properties | Configuration binding |
| `StarterJpaConfig` | `src/main/java/ca/uhn/fhir/jpa/starter/common/StarterJpaConfig.java` | JPA configuration | Database and interceptor setup |

## Directory Structure

```
dudoxx-fhir-server/
├── src/main/java/ca/uhn/fhir/jpa/starter/
│   ├── common/
│   │   └── StarterJpaConfig.java           # Main JPA configuration
│   ├── interceptor/
│   │   ├── ApiTokenAuthInterceptor.java    # Authentication
│   │   └── ClinicPartitionInterceptor.java  # Multi-tenancy
│   ├── cdshooks/                           # CDS Hooks implementation
│   │   ├── CdsHooksConfigCondition.java
│   │   ├── CdsHooksProperties.java
│   │   ├── CdsHooksRequest.java
│   │   ├── CdsHooksServlet.java
│   │   ├── ErrorHandling.java
│   │   ├── ModuleConfigurationPrefetchSvc.java
│   │   ├── ProviderConfiguration.java
│   │   └── StarterCdsHooksConfig.java
│   └── AppProperties.java
├── rest/server/
│   ├── McpBridge.java                      # MCP base
│   ├── McpCdsBridge.java                   # CDS MCP
│   └── McpFhirBridge.java                  # FHIR MCP
├── src/main/resources/
│   ├── application.yaml                    # Main config
│   ├── application-cds.yaml               # CDS config
│   ├── init-partitions.sql                # Partition setup
│   ├── logback.xml                        # Logging
│   └── mdm-rules.json                     # MDM rules
├── src/main/webapp/                        # Web resources
├── charts/                                 # Helm charts
├── configs/                                # Additional configs
└── pom.xml                                # Dependencies
```

## Key Configuration Sections

### application.yaml Critical Settings

```yaml
# Database
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ddx_hapifhir
    username: dudoxx_user
    password: [REDACTED]

# HAPI FHIR
hapi:
  fhir:
    # Multi-tenancy
    partitioning:
      enabled: true
      allow_references_across_partitions: true

    # Authentication
    auth:
      enabled: true
      api_token: ddx-api-token-2024

    # CORS (restrict in production)
    cors:
      allowed_origin: ["*"]
```

### Partition Initialization (init-partitions.sql)

```sql
INSERT INTO HFJ_PARTITION (PART_ID, PART_NAME, PART_DESC)
VALUES
  (0, 'DEFAULT', 'Default System Partition'),
  (1, 'HAMBURG', 'Hamburg Clinic'),
  (2, 'BERLIN', 'Berlin Clinic'),
  (3, 'MUNICH', 'Munich Clinic'),
  (4, 'FRANKFURT', 'Frankfurt Clinic'),
  (5, 'COLOGNE', 'Cologne Clinic')
ON CONFLICT (PART_ID) DO NOTHING;
```

## File Modification Guidelines

### Safe to Modify

- `application.yaml` - Configuration changes
- `application-cds.yaml` - CDS settings
- `logback.xml` - Logging levels
- `mdm-rules.json` - MDM rules
- `init-partitions.sql` - Adding new partitions

### Requires Rebuild

- All `.java` files - Code changes
- `pom.xml` - Dependency changes
- Configuration files affecting Spring context

### Requires Restart

- Database configuration changes
- Interceptor modifications
- Partition configuration updates

## Monitoring and Debugging

### Log Files

- Application logs: `server.log`
- HAPI FHIR logs: Configured in `logback.xml`
- Container logs: `docker logs hapi-fhir`

### Health Endpoints

- Application health: `http://localhost:8080/actuator/health`
- FHIR metadata: `http://localhost:8080/fhir/metadata`
- Swagger UI: `http://localhost:8080/fhir/swagger-ui/`

---

**Maintained by:** Dudoxx UG
**Last Updated:** December 8, 2025
