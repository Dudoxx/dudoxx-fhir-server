# Important API Patterns - HAPI FHIR Server

**Version:** 1.0.0
**Date:** December 12, 2025
**Owner:** Dudoxx UG

---

## MCP Server Endpoints

### MCP Tool Specifications

The HAPI FHIR Server exposes 9 MCP tools for AI-powered FHIR operations:

#### 1. fhir_search
**Purpose:** Advanced FHIR resource searching with filtering
**Parameters:**
- `resourceType`: FHIR resource type (Patient, Observation, etc.)
- `searchParams`: Map of search parameters
- `clinicId`: Clinic identifier for partition routing

**Example:**
```json
{
  "resourceType": "Patient",
  "searchParams": {
    "family": "Smith",
    "birthdate": "gt1980-01-01"
  },
  "clinicId": "ddx-hamburg-clinic"
}
```

#### 2. fhir_create
**Purpose:** Create new FHIR resources
**Parameters:**
- `resourceType`: FHIR resource type
- `resource`: Complete FHIR resource JSON
- `clinicId`: Clinic identifier

**Example:**
```json
{
  "resourceType": "Patient",
  "resource": {
    "resourceType": "Patient",
    "name": [{"family": "Smith", "given": ["John"]}],
    "birthDate": "1980-01-01"
  },
  "clinicId": "ddx-hamburg-clinic"
}
```

#### 3. fhir_read
**Purpose:** Retrieve specific FHIR resources by ID
**Parameters:**
- `resourceType`: FHIR resource type
- `id`: Resource identifier
- `clinicId`: Clinic identifier

#### 4. fhir_update
**Purpose:** Modify existing FHIR resources
**Parameters:**
- `resourceType`: FHIR resource type
- `id`: Resource identifier
- `resource`: Updated FHIR resource JSON
- `clinicId`: Clinic identifier

#### 5. fhir_delete
**Purpose:** Remove FHIR resources
**Parameters:**
- `resourceType`: FHIR resource type
- `id`: Resource identifier
- `clinicId`: Clinic identifier

#### 6. fhir_history
**Purpose:** Access resource version history
**Parameters:**
- `resourceType`: FHIR resource type
- `id`: Resource identifier
- `clinicId`: Clinic identifier

#### 7. fhir_validate
**Purpose:** Validate FHIR resource compliance
**Parameters:**
- `resource`: FHIR resource JSON to validate
- `clinicId`: Clinic identifier

#### 8. fhir_batch
**Purpose:** Execute batch operations
**Parameters:**
- `batch`: FHIR Bundle with batch entries
- `clinicId`: Clinic identifier

#### 9. fhir_capabilities
**Purpose:** Get server capability statement
**Parameters:**
- `clinicId`: Clinic identifier

## FHIR REST API Patterns

### Standard FHIR Endpoints

All FHIR endpoints require:
- `Authorization: Bearer ddx-api-token-2024`
- `X-Clinic-ID: ddx-{clinic}-clinic`

#### Resource CRUD Operations

```
GET    /fhir/{ResourceType}           # Search resources
GET    /fhir/{ResourceType}/{id}      # Read specific resource
POST   /fhir/{ResourceType}           # Create resource
PUT    /fhir/{ResourceType}/{id}      # Update resource
DELETE /fhir/{ResourceType}/{id}      # Delete resource
GET    /fhir/{ResourceType}/{id}/_history  # Resource history
```

#### Search Parameters

**Common Search Patterns:**
- `?family=Smith&given=John` - Name search
- `?birthdate=gt1980-01-01` - Date range
- `?status=active` - Status filtering
- `?_count=50` - Result pagination
- `?_sort=-date` - Sorting

**Advanced Search:**
- `?code:in=http://loinc.org|12345-6` - Code system search
- `?subject:Patient.name=Smith` - Chained search
- `?_include=Observation:subject` - Include related resources

### Batch Operations

**Bundle-based Operations:**
```json
{
  "resourceType": "Bundle",
  "type": "batch",
  "entry": [
    {
      "request": {
        "method": "POST",
        "url": "Patient"
      },
      "resource": { /* Patient resource */ }
    }
  ]
}
```

### CDS Hooks Integration

#### Service Discovery
```
GET /cds-services
```

**Response:**
```json
{
  "services": [
    {
      "id": "medication-prescribe",
      "title": "Medication Prescription CDS",
      "hook": "medication-prescribe",
      "description": "Provides medication interaction warnings"
    }
  ]
}
```

#### Hook Execution
```
POST /cds-services/{serviceId}
```

**Request Body:**
```json
{
  "hook": "medication-prescribe",
  "hookInstance": "uuid",
  "context": {
    "medications": [/* medication resources */]
  },
  "prefetch": {
    "patient": { /* patient resource */ }
  }
}
```

## Authentication Patterns

### Bearer Token Authentication

**Required Headers:**
```
Authorization: Bearer ddx-api-token-2024
X-Clinic-ID: ddx-hamburg-clinic
Content-Type: application/fhir+json
```

### Multi-Tenant Routing

**Clinic ID Mapping:**
- `ddx-hamburg-clinic` → Partition 1
- `ddx-berlin-clinic` → Partition 2
- `ddx-munich-clinic` → Partition 3
- `ddx-frankfurt-clinic` → Partition 4
- `ddx-cologne-clinic` → Partition 5

**Invalid Clinic ID Response:**
```json
{
  "resourceType": "OperationOutcome",
  "issue": [
    {
      "severity": "error",
      "code": "security",
      "details": {
        "text": "Invalid clinic ID"
      }
    }
  ]
}
```

## Error Handling Patterns

### FHIR OperationOutcome

**Validation Error:**
```json
{
  "resourceType": "OperationOutcome",
  "issue": [
    {
      "severity": "error",
      "code": "invalid",
      "details": {
        "text": "Invalid resource"
      },
      "location": ["Patient.name[0]"]
    }
  ]
}
```

### HTTP Status Codes

- `200` - Success
- `201` - Created
- `400` - Bad Request
- `401` - Unauthorized
- `403` - Forbidden (invalid clinic)
- `404` - Not Found
- `409` - Conflict
- `422` - Unprocessable Entity
- `500` - Internal Server Error

## Performance Patterns

### Pagination

**Search with Pagination:**
```
GET /fhir/Patient?_count=50&_offset=100
```

**Token-based Pagination:**
```
GET /fhir/Patient?_count=50&pageToken=abc123
```

### Caching Headers

**Response Headers:**
```
ETag: "12345"
Last-Modified: Wed, 21 Oct 2015 07:28:00 GMT
Cache-Control: max-age=3600
```

### Conditional Operations

**Conditional Create:**
```
POST /fhir/Patient
If-None-Exist: identifier=12345
```

**Conditional Update:**
```
PUT /fhir/Patient/123
If-Match: "12345"
```

## Monitoring Endpoints

### Health Checks

```
GET /actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

### Metrics

```
GET /actuator/metrics
GET /actuator/metrics/http.server.requests
```

### FHIR Metadata

```
GET /fhir/metadata
```

**Returns:** Complete CapabilityStatement with supported operations, search parameters, and profiles.

---

**Maintained by:** Dudoxx UG
**Last Updated:** December 12, 2025
