# HAPI FHIR Server - Documentation Index

**Version:** 6.0.0 | **Date:** December 12, 2025 | **Port:** 8080

**Author:** Walid Boudabbous, Founder and CTO of Dudoxx UG, CEO of Acceleate.com

---

## 📚 Documentation Overview

This directory contains comprehensive documentation for the Dudoxx HAPI FHIR 8.4 Server, a multi-tenant FHIR R4 server with authentication and PostgreSQL backend.

---

## 🚀 Getting Started

### Quick Links

| Document | Purpose | Audience |
|----------|---------|----------|
| [INSTALL.md](../INSTALL.md) | Installation and setup guide | Developers, DevOps |
| [IMPORTANT.md](../IMPORTANT.md) | Critical paths and key files | All |
| [ENV_VARIABLES.md](../ENV_VARIABLES.md) | Environment configuration | DevOps, Developers |
| [README.md](../README.md) | Project overview | All |
| [CLAUDE.md](../CLAUDE.md) | Quick reference for AI | AI Assistants |

### Prerequisites

- **Java 17+** (JDK)
- **Maven 3.8+**
- **PostgreSQL 12+**
- **Node.js 18+** (optional, for TypeScript client)

### 5-Minute Quick Start

```bash
# 1. Create database
psql -U postgres -c "CREATE DATABASE ddx_fhir_core;"
psql -U postgres -c "CREATE USER dudoxx_user WITH PASSWORD 'admin';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE ddx_fhir_core TO dudoxx_user;"

# 2. Initialize partitions
cd ddx-fhir
psql -U dudoxx_user -d ddx_fhir_core -f src/main/resources/init-partitions.sql

# 3. Create .env file
cat > .env <<'EOF'
PG_HOST=localhost
PG_PORT=5432
PG_DATABASE=ddx_fhir_core
PG_USER=dudoxx_user
PG_PASSWORD=admin
PG_DATABASE_TENANT=ddx_api_main
FHIR_AUTH_ENABLED=true
FHIR_API_TOKEN=ddx-api-token-2024
EOF

# 4. Start server
cd ..
./start-server.sh

# 5. Test
curl http://localhost:8080/actuator/health
```

---

## 📖 Core Documentation

### Installation & Configuration

1. **[INSTALL.md](../INSTALL.md)** - Complete installation guide
   - Prerequisites and verification
   - Database setup
   - Environment configuration
   - Build and run instructions
   - Verification steps
   - Troubleshooting

2. **[ENV_VARIABLES.md](../ENV_VARIABLES.md)** - Environment variables reference
   - Required variables
   - Connection pool configuration
   - Security settings
   - Environment-specific configs
   - Best practices

3. **[IMPORTANT.md](../IMPORTANT.md)** - Critical paths and important files
   - Key file locations
   - Multi-tenancy architecture
   - FHIR resources supported
   - Security configuration
   - Performance tuning
   - Common issues and solutions

### Quick Reference

4. **[README.md](../README.md)** - Project overview
   - About the fork
   - Quick start
   - Architecture diagrams
   - API usage examples
   - Development guide

5. **[CLAUDE.md](../CLAUDE.md)** - Quick reference for AI assistants
   - Critical rules
   - Multi-tenancy mapping
   - Common commands
   - Dual database architecture

### Advanced Topics

6. **[CLAUDE_PARTITIONS.md](../CLAUDE_PARTITIONS.md)** - Partition management
   - Adding new clinics
   - Partition troubleshooting
   - Dynamic tenant registration

7. **[CLAUDE_TROUBLESHOOTING.md](../CLAUDE_TROUBLESHOOTING.md)** - Debugging guide
   - Common errors
   - Database issues
   - Performance problems
   - Emergency procedures

8. **[UPSTREAM_SYNC.md](../UPSTREAM_SYNC.md)** - Syncing with upstream HAPI FHIR
   - Keeping fork updated
   - Merging upstream changes
   - Conflict resolution

9. **[DUDOXX_CUSTOMIZATIONS.md](../DUDOXX_CUSTOMIZATIONS.md)** - Custom modifications
   - Interceptors (Auth, Partitioning)
   - Tenant registry integration
   - Database configuration

---

## 🏗️ Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Dudoxx Platform                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Browser → Next.js (4000) → NestJS (4100) → HAPI FHIR (8080)│
│                              ↓                               │
│                         X-Clinic-ID                          │
│                              ↓                               │
│                  ClinicPartitionInterceptor                  │
│                              ↓                               │
│                   TenantRegistryService                      │
│                     (In-Memory Cache)                        │
│                              ↓                               │
│              ┌───────────────┴────────────────┐             │
│              │                                 │             │
│    ddx_api_main (Tenant Registry)   ddx_fhir_core (FHIR)   │
│    - organizations table             - 58 HAPI tables       │
│    - global_config                   - Partition isolation  │
│    - partition_sequence              - Resource storage     │
│              │                                 │             │
│              └────────── PostgreSQL (5432) ───┘             │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Multi-Tenancy Design

- **Partitions**: Dynamic partition allocation via database registry
- **Dynamic Routing**: X-Clinic-ID header maps to partition ID via TenantRegistryService
- **Authentication**: Bearer token validation
- **Dual Database**:
  - `ddx_fhir_core` - FHIR resource storage with partition isolation
  - `ddx_api_main` - Tenant registry (managed by NestJS)
- **Auto-Refresh**: Tenant cache refreshes every 60 seconds
- **REST API**: `/admin/tenants/*` endpoints for tenant management

See [IMPORTANT.md](../IMPORTANT.md#multi-tenancy-architecture) and [ARCHITECTURE.md](../ARCHITECTURE.md#database-architecture) for details.

---

## 🔍 FHIR Resources

### Capability Statement

```bash
# Get server capabilities
curl http://localhost:8080/fhir/metadata
```

### Supported Resources (R4)

HAPI FHIR R4 supports **145 resource types**. Common resources:

#### Clinical

- **Patient** - Patient demographics
- **Practitioner** - Healthcare providers
- **Observation** - Clinical observations
- **Condition** - Diagnoses and problems
- **MedicationRequest** - Prescriptions
- **AllergyIntolerance** - Patient allergies
- **DiagnosticReport** - Lab results
- **Procedure** - Performed procedures

#### Administrative

- **Appointment** - Scheduled appointments
- **Encounter** - Patient visits
- **Organization** - Clinics and facilities
- **Location** - Physical locations
- **Schedule** - Provider schedules
- **Slot** - Available time slots

### FHIR Specification

- **FHIR R4 Spec**: http://hl7.org/fhir/R4/
- **Resource List**: http://hl7.org/fhir/R4/resourcelist.html
- **Search Parameters**: http://hl7.org/fhir/R4/searchparameter-registry.html

---

## 🔐 API Documentation

### Interactive API Docs

- **Swagger UI**: http://localhost:8080/fhir/swagger-ui/
- **OpenAPI Spec**: http://localhost:8080/fhir/api-docs

### REST API Basics

#### Authentication

All requests (except public endpoints) require:

```bash
Authorization: Bearer ddx-api-token-2024
X-Clinic-ID: ddx-hamburg-clinic
```

#### Public Endpoints (No Auth)

```bash
GET /fhir/metadata           # Capability statement
GET /actuator/health         # Server health
GET /admin/tenants/health    # Tenant registry health check
```

#### Tenant Admin Endpoints (Requires Auth)

```bash
POST   /admin/tenants/refresh              # Force reload tenant cache
POST   /admin/tenants/register             # Register single tenant
GET    /admin/tenants                      # List all cached tenants
GET    /admin/tenants/{slug}               # Check if tenant exists
DELETE /admin/tenants/{slug}               # Remove tenant from cache
```

**Example - Refresh Tenant Cache:**
```bash
curl -X POST http://localhost:8080/admin/tenants/refresh \
  -H "Authorization: Bearer ddx-api-token-2024"
```

#### CRUD Operations

```bash
# Create Patient
POST /fhir/Patient
Content-Type: application/fhir+json
{
  "resourceType": "Patient",
  "name": [{"family": "Doe", "given": ["John"]}]
}

# Read Patient
GET /fhir/Patient/{id}

# Update Patient
PUT /fhir/Patient/{id}

# Delete Patient
DELETE /fhir/Patient/{id}

# Search Patients
GET /fhir/Patient?family=Doe&given=John
```

### Search Parameters

```bash
# Search by name
GET /fhir/Patient?name=Schmidt

# Search by birthdate
GET /fhir/Patient?birthdate=1980-01-01

# Search by identifier
GET /fhir/Patient?identifier=12345

# Combined search
GET /fhir/Patient?family=Schmidt&gender=male&birthdate=ge1980-01-01

# Pagination
GET /fhir/Patient?_count=20&_offset=40

# Include related resources
GET /fhir/Patient?_include=Patient:organization

# Reverse include
GET /fhir/Patient?_revinclude=Observation:patient
```

---

## 🛠️ Development

### Build & Run

```bash
# Build
cd ddx-fhir
mvn clean package -DskipTests

# Run with Maven
mvn spring-boot:run

# Run with startup script (recommended)
cd ..
./start-server.sh

# Run in daemon mode
./start-server.sh --daemon

# Stop daemon
./stop-server.sh
```

### Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=YourTestClass

# Run integration tests
mvn verify

# Skip tests during build
mvn package -DskipTests
```

### Development Tools

- **IntelliJ IDEA** - Recommended IDE
- **Postman** - API testing (see `/postman` directory in parent)
- **DBeaver** - Database management
- **PostgreSQL Client** - psql or pgAdmin

---

## 📊 Monitoring & Health

### Health Endpoints

```bash
# Server health
curl http://localhost:8080/actuator/health

# Detailed health (requires auth)
curl -H "Authorization: Bearer ddx-api-token-2024" \
     http://localhost:8080/actuator/health

# Tenant health
curl http://localhost:8080/admin/tenants/health
```

### Metrics

```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Application metrics
curl http://localhost:8080/actuator/metrics

# Specific metric
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

### Logs

```bash
# Daemon mode logs
tail -f ddx-fhir/fhir-server.log

# Follow with grep
tail -f ddx-fhir/fhir-server.log | grep ERROR

# Check specific time range
grep "2025-12-12" ddx-fhir/fhir-server.log
```

---

## 🔗 External Resources

### HAPI FHIR

- **Official Documentation**: https://hapifhir.io/
- **GitHub Repository**: https://github.com/hapifhir/hapi-fhir
- **JPA Server Starter**: https://github.com/hapifhir/hapi-fhir-jpaserver-starter
- **Community Chat**: https://chat.fhir.org/

### FHIR Specification

- **FHIR R4**: http://hl7.org/fhir/R4/
- **HL7 FHIR**: https://www.hl7.org/fhir/
- **FHIR Implementers**: https://confluence.hl7.org/display/FHIR/

### Tools

- **FHIR Validator**: https://validator.fhir.org/
- **FHIR Path Tester**: https://hl7.github.io/fhirpath.js/
- **Synthea** (Test Data): https://synthetichealth.github.io/synthea/

---

## 🤝 Related Projects

### Dudoxx Platform Components

| Project | Port | Description |
|---------|------|-------------|
| **ddx-web** | 4000 | Next.js 16 + React 19 Frontend |
| **ddx-api** | 4100 | NestJS 10 + Prisma Backend |
| **ddx-fhir** | 8080 | HAPI FHIR 8.4 Server (this project) |
| **ddx-sdk-fhir** | - | TypeScript FHIR Client Library |

### TypeScript Client

See **[../ddx-sdk-fhir/README.md](../../ddx-sdk-fhir/README.md)** for TypeScript client documentation.

**Quick Example:**

```typescript
import { FhirClient } from '@dudoxx/sdk-fhir';

const client = new FhirClient({
  baseUrl: 'http://localhost:8080/fhir',
  auth: {
    type: 'bearer',
    token: 'ddx-api-token-2024'
  },
  clinicId: 'ddx-hamburg-clinic'
});

// Search patients
const patients = await client.search('Patient', {
  family: 'Schmidt'
});

// Create patient
const patient = await client.create('Patient', {
  resourceType: 'Patient',
  name: [{ family: 'Doe', given: ['John'] }]
});
```

---

## 📞 Support

### Documentation

- **This Guide**: You're reading it!
- **Installation**: [INSTALL.md](../INSTALL.md)
- **Configuration**: [ENV_VARIABLES.md](../ENV_VARIABLES.md)
- **Troubleshooting**: [CLAUDE_TROUBLESHOOTING.md](../CLAUDE_TROUBLESHOOTING.md)

### Community

- **HAPI FHIR Chat**: https://chat.fhir.org/
- **GitHub Issues**: https://github.com/hapifhir/hapi-fhir/issues
- **Stack Overflow**: Tag `hapi-fhir`

### Contact

- **Email**: support@dudoxx.com
- **Organization**: Dudoxx UG
- **Website**: https://dudoxx.com

---

## 📝 Changelog

### Version 6.0.0 (December 12, 2025)

**Added:**
- Complete documentation suite (INSTALL.md, IMPORTANT.md, ENV_VARIABLES.md)
- Multi-tenant partition system (7 partitions)
- API token authentication
- Dynamic tenant registry integration
- Dual database architecture (FHIR + Tenant)
- Comprehensive monitoring and health checks

**Modified:**
- Upgraded to HAPI FHIR 8.4
- PostgreSQL configuration with HapiFhirPostgresDialect
- Custom interceptors (Auth + Partition routing)
- Connection pool optimization

**Database:**
- PostgreSQL dual database architecture:
  - Primary: `ddx_fhir_core` (FHIR resources, 58 tables)
  - Secondary: `ddx_api_main` (Tenant registry, read-only)
- Dynamic partition allocation via `organizations` table
- HikariCP connection pooling (10 for FHIR, 3 for tenants)
- Automatic tenant cache refresh every 60 seconds

---

## 🎯 Next Steps

1. **Installation**: Follow [INSTALL.md](../INSTALL.md) to set up the server
2. **Configuration**: Review [ENV_VARIABLES.md](../ENV_VARIABLES.md) for environment setup
3. **Testing**: Use Swagger UI to explore API endpoints
4. **Integration**: Connect from NestJS backend
5. **Production**: See [../PRODUCTION_SETUP.md](../../PRODUCTION_SETUP.md)

---

**Built with ❤️ by Dudoxx UG**  
**Powered by HAPI FHIR 8.4**  
**Licensed under Apache 2.0**
