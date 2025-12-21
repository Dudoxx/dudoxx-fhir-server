# HAPI FHIR 8.4 Server - Installation Guide

**Version:** 6.0.0 | **Date:** December 12, 2025 | **Port:** 8080

## Repo Policy

- Default branch: `main`
- This repository is used as a submodule of `dudoxx-hapifihr` (see `../.gitmodules`)

**Author:** Walid Boudabbous, Founder and CTO of Dudoxx UG, CEO of Acceleate.com

---

## Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| **Java JDK** | 17+ | HAPI FHIR runtime |
| **Maven** | 3.8+ | Build tool |
| **PostgreSQL** | 12+ | Database backend |
| **Node.js** | 18+ (optional) | TypeScript client |

### Verify Installation

```bash
# Java
java -version
# Expected: openjdk version "17.x.x" or higher

# Maven
mvn -version
# Expected: Apache Maven 3.8.x or higher

# PostgreSQL
psql --version
# Expected: psql (PostgreSQL) 12.x or higher
```

---

## Installation Steps

### 1. Clone Repository

```bash
cd /path/to/workspace
git clone <repository-url> dudoxx-hapifihr
cd dudoxx-hapifihr/ddx-fhir
```

### 2. Database Setup

#### Create FHIR Database

```bash
# Connect to PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE ddx_fhir_core;

# Create user
CREATE USER dudoxx_user WITH PASSWORD 'admin';

# Grant privileges
GRANT ALL PRIVILEGES ON DATABASE ddx_fhir_core TO dudoxx_user;

# Exit
\q
```

#### Initialize Partitions

```bash
# Run partition initialization script
psql -U dudoxx_user -h localhost -p 5432 -d ddx_fhir_core -f src/main/resources/init-partitions.sql
```

This creates 7 partitions:

| Partition ID | Name | Clinic ID | Purpose |
|--------------|------|-----------|---------|
| 0 | DEFAULT | default | System partition |
| 1 | HAMBURG | ddx-hamburg-clinic | Hamburg clinic |
| 2 | BERLIN | ddx-berlin-clinic | Berlin clinic |
| 3 | MUNICH | ddx-munich-clinic | Munich clinic |
| 4 | FRANKFURT | ddx-frankfurt-clinic | Frankfurt clinic |
| 5 | COLOGNE | ddx-cologne-clinic | Cologne clinic |
| 6 | SHARED | ddx-shared-clinic | Shared resources |

#### Create Tenant Registry Database (Optional)

For dynamic tenant management via NestJS:

```bash
psql -U postgres -c "CREATE DATABASE ddx_api_main;"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE ddx_api_main TO dudoxx_user;"
```

### 3. Configuration

#### Create Environment File

```bash
# Navigate to ddx-fhir directory
cd /path/to/dudoxx-hapifihr/ddx-fhir

# Create .env file
cat > .env <<'EOF'
# PostgreSQL Configuration - Primary FHIR Database
PG_HOST=localhost
PG_PORT=5432
PG_DATABASE=ddx_fhir_core
PG_USER=dudoxx_user
PG_PASSWORD=admin

# PostgreSQL Configuration - Tenant Registry (NestJS)
PG_DATABASE_TENANT=ddx_api_main

# Connection Pool Configuration
FHIR_POOL_MAX_SIZE=10
FHIR_POOL_MIN_IDLE=5
TENANT_POOL_MAX_SIZE=3
TENANT_POOL_MIN_IDLE=1

# HAPI FHIR Configuration
FHIR_AUTH_ENABLED=true
FHIR_API_TOKEN=ddx-api-token-2024
FHIR_BASE_URL=http://localhost:8080/fhir
EOF
```

#### Application Configuration

The main configuration is in `src/main/resources/application.yaml`. Key settings:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${PG_HOST}:${PG_PORT}/${PG_DATABASE}
    username: ${PG_USER}
    password: ${PG_PASSWORD}
  
hapi:
  fhir:
    auth:
      enabled: true
      api_token: ddx-api-token-2024
    partitioning:
      enabled: true
      default_partition_id: 0
```

See [ENV_VARIABLES.md](./ENV_VARIABLES.md) for complete variable reference.

### 4. Build the Server

```bash
# Navigate to ddx-fhir directory
cd /path/to/dudoxx-hapifihr/ddx-fhir

# Clean and build (skip tests for faster build)
mvn clean package -DskipTests

# Expected output:
# [INFO] BUILD SUCCESS
# [INFO] Total time: ~3-5 minutes
```

#### Build with Tests

```bash
# Full build with tests
mvn clean package

# Run specific test
mvn test -Dtest=YourTestClass
```

---

## Running the Server

### Option 1: Using Startup Script (Recommended)

```bash
# From parent directory (dudoxx-hapifihr)
cd /path/to/dudoxx-hapifihr

# Start in foreground (Ctrl+C to stop)
./start-server.sh

# Start in background (daemon mode)
./start-server.sh --daemon

# Stop daemon
./stop-server.sh
```

#### Startup Script Features

- ✅ Auto-validates database connections
- ✅ Checks if already running
- ✅ Loads environment variables
- ✅ Creates PID file for daemon mode
- ✅ Graceful shutdown handling

### Option 2: Using Maven Directly

```bash
# Navigate to ddx-fhir directory
cd /path/to/dudoxx-hapifihr/ddx-fhir

# Run with Maven
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=production
```

### Option 3: Using JAR File

```bash
# Build JAR
mvn clean package -DskipTests

# Run JAR
java -jar target/ROOT.war
```

---

## Verification

### 1. Health Check

```bash
# Check server health
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}
```

### 2. FHIR Metadata

```bash
# Get capability statement (no auth required)
curl http://localhost:8080/fhir/metadata

# Expected: FHIR CapabilityStatement JSON
```

### 3. Authenticated Request

```bash
# Test authentication and multi-tenancy
curl -X GET http://localhost:8080/fhir/Patient \
  -H "Authorization: Bearer ddx-api-token-2024" \
  -H "X-Clinic-ID: ddx-hamburg-clinic" \
  -H "Accept: application/fhir+json"

# Expected: FHIR Bundle with patients from Hamburg clinic
```

### 4. Swagger UI

Open in browser:

- **Swagger UI**: http://localhost:8080/fhir/swagger-ui/
- **API Docs**: http://localhost:8080/fhir/api-docs

---

## Post-Installation

### 1. Verify Partitions

```bash
# Connect to database
psql -U dudoxx_user -h localhost -p 5432 -d ddx_fhir_core

# List partitions
SELECT * FROM hfj_partition ORDER BY part_id;

# Expected: 7 partitions (0-6)
```

### 2. Test Multi-Tenancy

```bash
# Create patient in Hamburg
curl -X POST http://localhost:8080/fhir/Patient \
  -H "Authorization: Bearer ddx-api-token-2024" \
  -H "X-Clinic-ID: ddx-hamburg-clinic" \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Patient",
    "name": [{"family": "Schmidt", "given": ["Hans"]}],
    "gender": "male"
  }'

# Search in Hamburg (should find patient)
curl -H "Authorization: Bearer ddx-api-token-2024" \
     -H "X-Clinic-ID: ddx-hamburg-clinic" \
     http://localhost:8080/fhir/Patient

# Search in Berlin (should NOT find patient)
curl -H "Authorization: Bearer ddx-api-token-2024" \
     -H "X-Clinic-ID: ddx-berlin-clinic" \
     http://localhost:8080/fhir/Patient
```

### 3. Monitor Logs

```bash
# Daemon mode logs
tail -f ddx-fhir/fhir-server.log

# Maven console output (foreground mode)
# Logs appear in terminal
```

---

## Directory Structure

```
ddx-fhir/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── ca/uhn/fhir/jpa/starter/
│   │   │       ├── interceptor/
│   │   │       │   ├── ApiTokenAuthInterceptor.java
│   │   │       │   └── ClinicPartitionInterceptor.java
│   │   │       ├── tenant/
│   │   │       │   ├── TenantRegistryService.java
│   │   │       │   ├── TenantAdminController.java
│   │   │       │   └── TenantDataSourceConfig.java
│   │   │       ├── datasource/
│   │   │       │   └── FhirDataSourceConfig.java
│   │   │       └── Application.java
│   │   └── resources/
│   │       ├── application.yaml          # Main configuration
│   │       ├── .env.properties           # Dotenv loader config
│   │       ├── init-partitions.sql       # Partition setup
│   │       └── logback.xml               # Logging config
│   └── test/
├── target/                               # Build output
├── .env                                  # Environment variables (create this)
├── pom.xml                               # Maven configuration
└── README.md                             # Project documentation
```

---

## Troubleshooting

### Database Connection Failed

```bash
# Error: Cannot connect to database
# Solution: Check PostgreSQL is running
brew services list | grep postgresql
# or
systemctl status postgresql

# Verify credentials
psql -U dudoxx_user -h localhost -p 5432 -d ddx_fhir_core
```

### Port 8080 Already In Use

```bash
# Find process using port 8080
lsof -i :8080
# or
netstat -ano | findstr :8080

# Kill process or change port in application.yaml
server:
  port: 8081
```

### Build Failures

```bash
# Clear Maven cache
mvn clean

# Update dependencies
mvn dependency:resolve

# Full rebuild
mvn clean install -U
```

### Authentication Errors

```bash
# Error: 401 Unauthorized
# Solution: Check Bearer token
curl -H "Authorization: Bearer ddx-api-token-2024" \
     http://localhost:8080/fhir/metadata

# Verify token in application.yaml matches .env
grep FHIR_API_TOKEN .env
```

### Partition Errors

```bash
# Error: Partition not found
# Solution: Verify partitions exist
psql -U dudoxx_user -d ddx_fhir_core -c "SELECT * FROM hfj_partition;"

# Re-run partition init
psql -U dudoxx_user -d ddx_fhir_core -f src/main/resources/init-partitions.sql
```

---

## Next Steps

1. **Read Configuration Guide**: [ENV_VARIABLES.md](./ENV_VARIABLES.md)
2. **Review Important Files**: [IMPORTANT.md](./IMPORTANT.md)
3. **Explore API Documentation**: http://localhost:8080/fhir/swagger-ui/
4. **Test with TypeScript Client**: See [../ddx-sdk-fhir/README.md](../ddx-sdk-fhir/README.md)
5. **Setup Production Environment**: See [../PRODUCTION_SETUP.md](../PRODUCTION_SETUP.md)

---

## Support

For issues and questions:

- **Documentation**: [README.md](./README.md), [CLAUDE.md](./CLAUDE.md)
- **Troubleshooting**: [CLAUDE_TROUBLESHOOTING.md](./CLAUDE_TROUBLESHOOTING.md)
- **Email**: support@dudoxx.com
- **HAPI FHIR Community**: https://chat.fhir.org/

---

**Built with ❤️ by Dudoxx UG**  
**Powered by HAPI FHIR 8.4**
