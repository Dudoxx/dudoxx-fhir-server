# HAPI FHIR Server - Environment Variables Reference

**Version:** 6.0.0 | **Date:** December 12, 2025

**Author:** Walid Boudabbous, Founder and CTO of Dudoxx UG, CEO of Acceleate.com

---

## Overview

HAPI FHIR uses environment variables for configuration through the **spring-dotenv** library. Variables are loaded from `.env` file in the `ddx-fhir/` directory.

### Configuration Files

| File | Path | Purpose |
|------|------|---------|
| **`.env`** | `ddx-fhir/.env` | Environment variables (create this) |
| **`.env.properties`** | `src/main/resources/.env.properties` | Dotenv loader configuration |
| **`application.yaml`** | `src/main/resources/application.yaml` | Spring Boot configuration (uses env vars) |

---

## Required Environment Variables

### Primary FHIR Database (ddx_fhir_core)

```bash
# PostgreSQL Host
PG_HOST=localhost

# PostgreSQL Port
PG_PORT=5432

# Database Name (FHIR resources)
PG_DATABASE=ddx_fhir_core

# Database User
PG_USER=dudoxx_user

# Database Password
PG_PASSWORD=admin
```

### Tenant Registry Database (ddx_api_main)

```bash
# Tenant Database Name (NestJS organizations table)
PG_DATABASE_TENANT=ddx_api_main
```

**Note:** This database is read by `TenantRegistryService` to dynamically load clinic configurations from NestJS.

---

## Connection Pool Configuration

### Primary FHIR Database Pool

```bash
# Maximum Pool Size (default: 10)
# Recommended: 10-20 for production
FHIR_POOL_MAX_SIZE=10

# Minimum Idle Connections (default: 5)
# Recommended: 50% of max pool size
FHIR_POOL_MIN_IDLE=5
```

### Tenant Registry Database Pool

```bash
# Maximum Pool Size (default: 3)
# Lower because it's read-only and infrequent
TENANT_POOL_MAX_SIZE=3

# Minimum Idle Connections (default: 1)
TENANT_POOL_MIN_IDLE=1
```

### Pool Sizing Guidelines

| Deployment Size | Users | FHIR_POOL_MAX_SIZE | TENANT_POOL_MAX_SIZE |
|-----------------|-------|-------------------|---------------------|
| Small (Dev) | 1-10 | 5 | 2 |
| Medium | 10-100 | 10-20 | 3 |
| Large | 100-500 | 20-50 | 5 |
| Enterprise | 500+ | 50-100 | 10 |

---

## HAPI FHIR Configuration

### Authentication

```bash
# Enable API Token Authentication (default: true)
FHIR_AUTH_ENABLED=true

# API Token for Bearer Authentication (default: ddx-api-token-2024)
FHIR_API_TOKEN=ddx-api-token-2024
```

**Security Best Practice:**

```bash
# Generate secure token for production
FHIR_API_TOKEN=$(openssl rand -hex 32)

# Example: a1b2c3d4e5f6...
```

### Server Configuration

```bash
# FHIR Server Base URL (default: http://localhost:8080/fhir)
FHIR_BASE_URL=http://localhost:8080/fhir

# Production example:
# FHIR_BASE_URL=https://fhir.dudoxx.com/fhir
```

---

## Optional Variables

### JVM Memory Configuration

```bash
# Java Heap Size
JAVA_OPTS=-Xmx4g -Xms2g

# Maven Memory (for development)
MAVEN_OPTS=-Xmx4g -Xms2g
```

### Logging Configuration

```bash
# Log Level (default: INFO)
# Options: TRACE, DEBUG, INFO, WARN, ERROR
LOG_LEVEL=INFO

# Enable SQL Logging (default: false)
HIBERNATE_SHOW_SQL=false

# Enable Hibernate SQL Formatting
HIBERNATE_FORMAT_SQL=false
```

### Server Port

```bash
# HTTP Port (default: 8080)
SERVER_PORT=8080

# Production example:
# SERVER_PORT=8443 (with SSL)
```

---

## Complete .env Template

Create this file at `ddx-fhir/.env`:

```bash
# =============================================================================
# DDX FHIR Server - Environment Configuration
# =============================================================================
# Version: 6.0.0
# Date: December 12, 2025
# =============================================================================

# -----------------------------------------------------------------------------
# PostgreSQL Configuration - Primary FHIR Database
# -----------------------------------------------------------------------------
PG_HOST=localhost
PG_PORT=5432
PG_DATABASE=ddx_fhir_core
PG_USER=dudoxx_user
PG_PASSWORD=admin

# -----------------------------------------------------------------------------
# PostgreSQL Configuration - Tenant Registry (NestJS)
# -----------------------------------------------------------------------------
PG_DATABASE_TENANT=ddx_api_main

# -----------------------------------------------------------------------------
# Connection Pool Configuration
# -----------------------------------------------------------------------------
# Primary FHIR Database Pool
FHIR_POOL_MAX_SIZE=10
FHIR_POOL_MIN_IDLE=5

# Tenant Registry Database Pool
TENANT_POOL_MAX_SIZE=3
TENANT_POOL_MIN_IDLE=1

# -----------------------------------------------------------------------------
# HAPI FHIR Configuration
# -----------------------------------------------------------------------------
# Authentication
FHIR_AUTH_ENABLED=true
FHIR_API_TOKEN=ddx-api-token-2024

# Server Base URL
FHIR_BASE_URL=http://localhost:8080/fhir

# -----------------------------------------------------------------------------
# Optional: JVM & Logging
# -----------------------------------------------------------------------------
# Java Memory Settings (uncomment to override)
# JAVA_OPTS=-Xmx4g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
# MAVEN_OPTS=-Xmx4g -Xms2g

# Logging Level (TRACE, DEBUG, INFO, WARN, ERROR)
LOG_LEVEL=INFO

# SQL Logging (true/false)
HIBERNATE_SHOW_SQL=false
HIBERNATE_FORMAT_SQL=false

# -----------------------------------------------------------------------------
# Optional: Server Configuration
# -----------------------------------------------------------------------------
# Server Port (uncomment to override)
# SERVER_PORT=8080

# -----------------------------------------------------------------------------
# Production Security Notes
# -----------------------------------------------------------------------------
# 1. Generate secure API token:
#    FHIR_API_TOKEN=$(openssl rand -hex 32)
#
# 2. Use strong database password:
#    PG_PASSWORD=$(openssl rand -base64 32)
#
# 3. Enable SSL/TLS in production
#
# 4. Never commit this file to version control
# =============================================================================
```

---

## How Variables Are Loaded

### 1. Spring Dotenv Configuration

File: `src/main/resources/.env.properties`

```properties
directory=./
filename=.env
ignoreIfMissing=false
```

This tells Spring Boot to:
- Look for `.env` in the **project root** (`./`)
- Use filename `.env`
- Fail if file is missing (`ignoreIfMissing=false`)

### 2. Variable Resolution in application.yaml

```yaml
spring:
  datasource:
    # ${VAR_NAME:default_value}
    url: jdbc:postgresql://${PG_HOST:localhost}:${PG_PORT:5432}/${PG_DATABASE:ddx_fhir_core}
    username: ${PG_USER:dudoxx_user}
    password: ${PG_PASSWORD:admin}
```

**Syntax:** `${VAR_NAME:default_value}`

- If `VAR_NAME` is set in `.env`, use that value
- Otherwise, use `default_value`

### 3. Load Order

1. **`.env` file** (via spring-dotenv)
2. **System environment variables** (override `.env`)
3. **Command-line arguments** (override all)

**Example:**

```bash
# .env file
PG_HOST=localhost

# Override via environment
export PG_HOST=production-db.example.com

# Override via command-line
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:postgresql://override-host:5432/db"
```

---

## Environment-Specific Configuration

### Development (.env)

```bash
# Development Environment
PG_HOST=localhost
PG_PORT=5432
PG_DATABASE=ddx_fhir_core
PG_USER=dudoxx_user
PG_PASSWORD=admin
FHIR_POOL_MAX_SIZE=5
LOG_LEVEL=DEBUG
HIBERNATE_SHOW_SQL=true
```

### Staging (.env.staging)

```bash
# Staging Environment
PG_HOST=staging-db.internal
PG_PORT=5432
PG_DATABASE=ddx_fhir_core_staging
PG_USER=ddx_staging_user
PG_PASSWORD=<secure-password>
FHIR_POOL_MAX_SIZE=10
LOG_LEVEL=INFO
FHIR_BASE_URL=https://fhir-staging.dudoxx.com/fhir
```

### Production (.env.production)

```bash
# Production Environment
PG_HOST=prod-fhir-db.rds.amazonaws.com
PG_PORT=5432
PG_DATABASE=ddx_fhir_core_prod
PG_USER=ddx_prod_user
PG_PASSWORD=<secure-password-from-secrets-manager>
FHIR_POOL_MAX_SIZE=20
FHIR_POOL_MIN_IDLE=10
LOG_LEVEL=WARN
FHIR_BASE_URL=https://fhir.dudoxx.com/fhir
FHIR_API_TOKEN=<secure-token-from-vault>
```

**Switching Environments:**

```bash
# Copy environment-specific file
cp .env.production .env

# Or use symbolic link
ln -sf .env.production .env
```

---

## Security Best Practices

### 1. Never Commit .env Files

Add to `.gitignore`:

```gitignore
# Environment files
.env
.env.local
.env.production
.env.staging
.env.*.local

# Keep example template
!.env.example
```

### 2. Use Secrets Management

**For Production:**

```bash
# AWS Secrets Manager
aws secretsmanager get-secret-value --secret-id ddx/fhir/db-password

# HashiCorp Vault
vault kv get secret/ddx/fhir/config

# Kubernetes Secrets
kubectl get secret ddx-fhir-secrets -o yaml
```

### 3. Rotate Credentials Regularly

```bash
# Generate new database password
NEW_PASSWORD=$(openssl rand -base64 32)

# Update PostgreSQL
psql -U postgres -c "ALTER USER dudoxx_user WITH PASSWORD '$NEW_PASSWORD';"

# Update .env
sed -i '' "s/PG_PASSWORD=.*/PG_PASSWORD=$NEW_PASSWORD/" .env

# Restart server
./stop-server.sh
./start-server.sh
```

### 4. Least Privilege Principle

```sql
-- Revoke unnecessary privileges
REVOKE ALL ON DATABASE ddx_fhir_core FROM PUBLIC;

-- Grant only required privileges
GRANT CONNECT ON DATABASE ddx_fhir_core TO dudoxx_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO dudoxx_user;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO dudoxx_user;

-- Tenant database: Read-only
GRANT CONNECT ON DATABASE ddx_api_main TO dudoxx_user;
GRANT SELECT ON TABLE organizations TO dudoxx_user;
GRANT SELECT ON TABLE global_config TO dudoxx_user;
```

---

## Troubleshooting

### Variable Not Loading

**Issue:** Environment variable not recognized

**Check:**

```bash
# 1. Verify .env file exists
ls -la ddx-fhir/.env

# 2. Check .env.properties configuration
cat src/main/resources/.env.properties

# 3. Test variable loading
mvn spring-boot:run -Ddebug | grep "Loading environment variables"

# 4. Check for syntax errors in .env
cat ddx-fhir/.env | grep -v '^#' | grep '='
```

### Connection Pool Exhausted

**Error:** `Connection pool exhausted`

**Solution:**

```bash
# Increase pool size
# Edit .env
FHIR_POOL_MAX_SIZE=20
FHIR_POOL_MIN_IDLE=10

# Restart server
./stop-server.sh
./start-server.sh

# Monitor connections
psql -U dudoxx_user -d ddx_fhir_core -c "SELECT count(*) FROM pg_stat_activity WHERE datname='ddx_fhir_core';"
```

### Authentication Failures

**Error:** `401 Unauthorized`

**Check:**

```bash
# Verify token in .env
grep FHIR_API_TOKEN ddx-fhir/.env

# Verify token in application.yaml
grep api_token src/main/resources/application.yaml

# Test with correct token
curl -H "Authorization: Bearer $(grep FHIR_API_TOKEN ddx-fhir/.env | cut -d= -f2)" \
     http://localhost:8080/fhir/metadata
```

### Database Connection Errors

**Error:** `Cannot connect to database`

**Check:**

```bash
# Verify database variables
grep ^PG_ ddx-fhir/.env

# Test connection
PGPASSWORD=$(grep PG_PASSWORD ddx-fhir/.env | cut -d= -f2) \
  psql -U $(grep PG_USER ddx-fhir/.env | cut -d= -f2) \
       -h $(grep PG_HOST ddx-fhir/.env | cut -d= -f2) \
       -p $(grep PG_PORT ddx-fhir/.env | cut -d= -f2) \
       -d $(grep PG_DATABASE ddx-fhir/.env | cut -d= -f2)
```

---

## Reference

### Variable Quick Reference

| Variable | Default | Purpose | Required? |
|----------|---------|---------|-----------|
| `PG_HOST` | localhost | Database host | ✅ Yes |
| `PG_PORT` | 5432 | Database port | ✅ Yes |
| `PG_DATABASE` | ddx_fhir_core | FHIR database name | ✅ Yes |
| `PG_USER` | dudoxx_user | Database user | ✅ Yes |
| `PG_PASSWORD` | admin | Database password | ✅ Yes |
| `PG_DATABASE_TENANT` | ddx_api_main | Tenant database | ✅ Yes |
| `FHIR_POOL_MAX_SIZE` | 10 | Max pool size | ⚠️ Recommended |
| `FHIR_POOL_MIN_IDLE` | 5 | Min idle connections | ⚠️ Recommended |
| `TENANT_POOL_MAX_SIZE` | 3 | Tenant pool max | ⚠️ Recommended |
| `TENANT_POOL_MIN_IDLE` | 1 | Tenant pool min | ⚠️ Recommended |
| `FHIR_AUTH_ENABLED` | true | Enable auth | ⚠️ Recommended |
| `FHIR_API_TOKEN` | ddx-api-token-2024 | Bearer token | ✅ Yes |
| `FHIR_BASE_URL` | http://localhost:8080/fhir | Server base URL | ⚠️ Recommended |
| `JAVA_OPTS` | (none) | JVM options | ❌ Optional |
| `MAVEN_OPTS` | (none) | Maven options | ❌ Optional |
| `LOG_LEVEL` | INFO | Logging level | ❌ Optional |
| `HIBERNATE_SHOW_SQL` | false | Show SQL | ❌ Optional |
| `SERVER_PORT` | 8080 | HTTP port | ❌ Optional |

---

## Additional Resources

- [Installation Guide](./INSTALL.md)
- [Important Files](./IMPORTANT.md)
- [Troubleshooting](./CLAUDE_TROUBLESHOOTING.md)
- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)

---

**Built with ❤️ by Dudoxx UG**  
**Powered by HAPI FHIR 8.4**
