# HAPI FHIR MCP (MODEL CONTEXT PROTOCOL) - COMPLETE GUIDE

```
██████╗ ██╗   ██╗██████╗  ██████╗ ██╗  ██╗██╗  ██╗
██╔══██╗██║   ██║██╔══██╗██╔═══██╗╚██╗██╔╝╚██╗██╔╝
██║  ██║██║   ██║██║  ██║██║   ██║ ╚███╔╝  ╚███╔╝
██║  ██║██║   ██║██║  ██║██║   ██║ ██╔██╗  ██╔██╗
██████╔╝╚██████╔╝██████╔╝╚██████╔╝██╔╝ ██╗██╔╝ ██╗
╚═════╝  ╚═════╝ ╚═════╝  ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝
         MCP INTEGRATION GUIDE v1.0.0
```

**Version:** 1.0.0
**Date:** November 25, 2025
**Owner:** Dudoxx UG
**HAPI FHIR Version:** 8.4.0
**Spring AI Version:** 1.1.0-M2+

---

## TABLE OF CONTENTS

1. [OVERVIEW](#overview)
2. [WHAT IS MCP?](#what-is-mcp)
3. [THE 9 REGISTERED MCP TOOLS](#the-9-registered-mcp-tools)
4. [MCP ARCHITECTURE](#mcp-architecture)
5. [HOW TO ACCESS MCP](#how-to-access-mcp)
6. [CLINICAL USE CASES](#clinical-use-cases)
7. [INTEGRATION METHODS](#integration-methods)
8. [SECURITY CONSIDERATIONS](#security-considerations)
9. [TESTING WITH MCP INSPECTOR](#testing-with-mcp-inspector)
10. [TUTORIAL: BUILD AI MEDICAL ASSISTANT](#tutorial-build-ai-medical-assistant)
11. [ADVANCED FEATURES](#advanced-features)
12. [PERFORMANCE TUNING](#performance-tuning)
13. [TROUBLESHOOTING](#troubleshooting)
14. [WHEN TO DISABLE MCP](#when-to-disable-mcp)
15. [REFERENCES](#references)

---

## OVERVIEW

### WHAT IS MCP IN THIS SERVER?

The **Model Context Protocol (MCP)** enables AI agents (Claude, ChatGPT, custom LLMs) to interact with your HAPI FHIR server using **natural language** instead of complex FHIR REST queries.

Your HAPI FHIR server (version 8.4.0) has **9 registered MCP tools** that allow AI to:
- Create, read, update, delete FHIR resources
- Search clinical data with natural language
- Execute batch transactions
- Generate clinical summaries
- Perform complex clinical decision support

### KEY BENEFITS

| Benefit | Description |
|---------|-------------|
| **Natural Language** | "Find diabetic patients" instead of complex FHIR queries |
| **AI-Powered** | Enable LLMs to access clinical data intelligently |
| **Standardized** | Uses open MCP standard (Anthropic) |
| **Multi-Clinic** | Works with partitioned multi-tenant architecture |
| **Extensible** | Easy to add custom clinical workflows |

### STARTUP LOG EVIDENCE

```
Enable tools capabilities, notification: true
Registered tools: 9
Enable resources capabilities, notification: true
Enable prompts capabilities, notification: true
```

**Location:** Server startup logs (2025-11-25T23:04:33)

---

## WHAT IS MCP?

### DEFINITION

**Model Context Protocol (MCP)** is an open-source standard developed by Anthropic that standardizes how AI applications connect to external systems.

**Analogy:** MCP is like a "USB-C port for AI" - just as USB-C provides a universal connection for devices, MCP provides a universal way for AI to connect to data sources.

### MCP CLIENT-SERVER ARCHITECTURE

```
┌─────────────────────────────────────────────────────────┐
│         AI APPLICATION (MCP CLIENT)                      │
│  Examples: Claude Desktop, Custom AI Agent, ChatGPT     │
└────────────────────┬────────────────────────────────────┘
                     │ MCP Protocol (HTTP/SSE)
                     ▼
┌─────────────────────────────────────────────────────────┐
│         HAPI FHIR SERVER (MCP SERVER)                    │
│  Endpoint: http://localhost:8080/mcp/message            │
│  Exposes: 9 FHIR tools + resources + prompts            │
└────────────────────┬────────────────────────────────────┘
                     │ FHIR REST API
                     ▼
┌─────────────────────────────────────────────────────────┐
│         POSTGRESQL (ddx_hapifhir)                        │
│  Partitioned: Hamburg, Berlin, Munich, etc.             │
└─────────────────────────────────────────────────────────┘
```

### THE THREE MCP PRIMITIVES

#### 1. TOOLS (MODEL-CONTROLLED) ⚙️

**What:** Functions that AI can invoke to **perform actions** (verbs)
**Who Controls:** The AI model decides when to call them
**Your Server Has:** 9 FHIR tools

**Example:**
```
User: "Create a patient named Anna Schmidt"
AI thinks: "I need to use create-fhir-resource tool"
AI calls: create-fhir-resource({ resourceType: "Patient", resource: {...} })
FHIR Server: Creates patient, returns ID
AI responds: "Created patient Anna Schmidt with ID Patient/789"
```

#### 2. RESOURCES (APPLICATION-CONTROLLED) 📚

**What:** Read-only data sources providing context (knowledge)
**Who Controls:** Client application explicitly manages access
**Your Server Has:** Enabled but not actively used yet

**Example:**
```
Resource: "clinic-demographics-hamburg"
→ Provides: Patient statistics, common diagnoses, active medications
AI uses this to understand clinic context before answering questions
```

#### 3. PROMPTS (USER-CONTROLLED) 💬

**What:** Pre-defined instruction templates for optimal AI interactions
**Who Controls:** User selects before inference
**Your Server Has:** Enabled but not actively used yet

**Example:**
```
Prompt: "generate-discharge-summary"
Template: "Get patient demographics + conditions + medications → format as summary"
User selects this prompt, AI executes entire workflow automatically
```

---

## THE 9 REGISTERED MCP TOOLS

### COMPLETE TOOL INVENTORY

| # | Tool Name | HTTP Method | Purpose | Example Use Case |
|---|-----------|-------------|---------|------------------|
| **1** | `create-fhir-resource` | POST | Create new FHIR resources | "Create patient John Doe, DOB 1980-05-15" |
| **2** | `read-fhir-resource` | GET | Read individual resources by ID | "Show me patient with ID 123" |
| **3** | `update-fhir-resource` | PUT | Replace entire resource | "Update patient 123's full record" |
| **4** | `conditional-update-fhir-resource` | PUT + Search | Update based on search criteria | "Update patient where MRN=12345" |
| **5** | `delete-fhir-resource` | DELETE | Remove resources | "Delete observation ID 456" |
| **6** | `patch-fhir-resource` | PATCH | Partially update resource | "Change patient 123's phone number only" |
| **7** | `conditional-patch-fhir-resource` | PATCH + Search | Partial update by criteria | "Update active status where name=Ivan" |
| **8** | `search-fhir-resources` | GET + Query | Search with FHIR queries | "Find all diabetic patients in Hamburg" |
| **9** | `create-fhir-transaction` | POST Bundle | Batch operations (atomic) | "Create patient + 3 observations together" |

### TOOL DETAILS

#### TOOL 1: CREATE-FHIR-RESOURCE

**Purpose:** Create new FHIR resources (Patient, Observation, etc.)

**Schema:**
```json
{
  "name": "create-fhir-resource",
  "description": "Create a new FHIR resource",
  "inputSchema": {
    "type": "object",
    "properties": {
      "resourceType": {
        "type": "string",
        "description": "Type of resource to create (Patient, Observation, etc.)"
      },
      "resource": {
        "type": "object",
        "description": "Resource content in JSON format"
      },
      "headers": {
        "type": "object",
        "description": "Optional headers (e.g., If-None-Exist for conditional create)"
      }
    },
    "required": ["resourceType", "resource"]
  }
}
```

**Example Call:**
```json
{
  "resourceType": "Patient",
  "resource": {
    "resourceType": "Patient",
    "identifier": [{
      "system": "urn:ddx:mrn",
      "value": "MRN-123456"
    }],
    "name": [{
      "family": "Schmidt",
      "given": ["Anna", "Maria"]
    }],
    "gender": "female",
    "birthDate": "1990-05-20",
    "address": [{
      "city": "Hamburg",
      "postalCode": "20095",
      "country": "DE"
    }]
  }
}
```

**Response:**
```json
{
  "status": 201,
  "body": {
    "resourceType": "Patient",
    "id": "789",
    "meta": {
      "versionId": "1",
      "lastUpdated": "2025-11-25T23:15:00Z"
    }
  }
}
```

---

#### TOOL 2: READ-FHIR-RESOURCE

**Purpose:** Retrieve a specific resource by ID

**Schema:**
```json
{
  "name": "read-fhir-resource",
  "description": "Read an individual FHIR resource",
  "inputSchema": {
    "type": "object",
    "properties": {
      "resourceType": {
        "type": "string",
        "description": "Type of resource to read"
      },
      "id": {
        "type": "string",
        "description": "ID of the resource"
      }
    },
    "required": ["resourceType", "id"]
  }
}
```

**Example Call:**
```json
{
  "resourceType": "Patient",
  "id": "789"
}
```

---

#### TOOL 3: UPDATE-FHIR-RESOURCE

**Purpose:** Replace entire resource (must include all fields)

**Schema:**
```json
{
  "name": "update-fhir-resource",
  "description": "Update an existing FHIR resource",
  "inputSchema": {
    "type": "object",
    "properties": {
      "resourceType": {"type": "string"},
      "id": {"type": "string"},
      "resource": {"type": "object"}
    },
    "required": ["resourceType", "id", "resource"]
  }
}
```

**Example Call:**
```json
{
  "resourceType": "Patient",
  "id": "789",
  "resource": {
    "resourceType": "Patient",
    "id": "789",
    "name": [{"family": "Schmidt-Mueller", "given": ["Anna"]}],
    "birthDate": "1990-05-20"
  }
}
```

---

#### TOOL 4: CONDITIONAL-UPDATE-FHIR-RESOURCE

**Purpose:** Update resource matching search criteria

**Schema:**
```json
{
  "name": "conditional-update-fhir-resource",
  "description": "Conditional update an existing FHIR resource",
  "inputSchema": {
    "type": "object",
    "properties": {
      "resourceType": {"type": "string"},
      "resource": {"type": "object"},
      "query": {
        "type": "string",
        "description": "Search params (e.g., '_id=pt-1,name=ivan')"
      },
      "headers": {
        "type": "object",
        "description": "Optional If-None-Match header with ETag"
      }
    },
    "required": ["resourceType", "resource"]
  }
}
```

**Example Call:**
```json
{
  "resourceType": "Patient",
  "query": "identifier=MRN-123456",
  "resource": {
    "resourceType": "Patient",
    "identifier": [{"system": "urn:ddx:mrn", "value": "MRN-123456"}],
    "active": false
  }
}
```

---

#### TOOL 5: DELETE-FHIR-RESOURCE

**Purpose:** Remove resource from server

**Schema:**
```json
{
  "name": "delete-fhir-resource",
  "description": "Delete an existing FHIR resource",
  "inputSchema": {
    "type": "object",
    "properties": {
      "resourceType": {"type": "string"},
      "id": {"type": "string"}
    },
    "required": ["resourceType", "id"]
  }
}
```

**Example Call:**
```json
{
  "resourceType": "Observation",
  "id": "obs-456"
}
```

---

#### TOOL 6: PATCH-FHIR-RESOURCE

**Purpose:** Update only specific fields (partial update)

**Schema:**
```json
{
  "name": "patch-fhir-resource",
  "description": "Patch an existing FHIR resource",
  "inputSchema": {
    "type": "object",
    "properties": {
      "resourceType": {"type": "string"},
      "id": {"type": "string"},
      "resource": {
        "type": "object",
        "description": "JSON Patch format content"
      }
    },
    "required": ["resourceType", "id", "resource"]
  }
}
```

**Example Call (JSON Patch):**
```json
{
  "resourceType": "Patient",
  "id": "789",
  "resource": [
    {
      "op": "replace",
      "path": "/telecom/0/value",
      "value": "+49-40-123456"
    }
  ]
}
```

---

#### TOOL 7: CONDITIONAL-PATCH-FHIR-RESOURCE

**Purpose:** Partial update based on search criteria

**Schema:**
```json
{
  "name": "conditional-patch-fhir-resource",
  "description": "Conditional patch an existing FHIR resource",
  "inputSchema": {
    "type": "object",
    "properties": {
      "resourceType": {"type": "string"},
      "resource": {"type": "object"},
      "query": {"type": "string"}
    },
    "required": ["resourceType", "resource"]
  }
}
```

**Example Call:**
```json
{
  "resourceType": "Patient",
  "query": "name=Anna,birthdate=1990-05-20",
  "resource": [
    {"op": "add", "path": "/active", "value": true}
  ]
}
```

---

#### TOOL 8: SEARCH-FHIR-RESOURCES

**Purpose:** Query resources with FHIR search parameters

**Schema:**
```json
{
  "name": "search-fhir-resources",
  "description": "Search existing FHIR resources",
  "inputSchema": {
    "type": "object",
    "properties": {
      "resourceType": {
        "type": "string",
        "description": "Type to search (Patient, Observation, etc.)"
      },
      "query": {
        "type": "string",
        "description": "Comma-separated search params (e.g., 'name=Schmidt,birthdate=1990')"
      }
    },
    "required": ["resourceType", "query"]
  }
}
```

**Example Calls:**

```json
// Find patients by name
{
  "resourceType": "Patient",
  "query": "name=Schmidt"
}

// Find diabetic patients
{
  "resourceType": "Condition",
  "query": "code=E11,clinical-status=active"
}

// Find recent lab results for patient
{
  "resourceType": "Observation",
  "query": "patient=Patient/789,category=laboratory,_sort=-date,_count=10"
}

// Find appointments for practitioner
{
  "resourceType": "Appointment",
  "query": "practitioner=Practitioner/Dr-Schmidt,date=ge2025-12-01"
}
```

**Response (Bundle):**
```json
{
  "resourceType": "Bundle",
  "type": "searchset",
  "total": 42,
  "entry": [
    {
      "resource": {
        "resourceType": "Patient",
        "id": "789",
        "name": [{"family": "Schmidt", "given": ["Anna"]}]
      }
    }
  ]
}
```

---

#### TOOL 9: CREATE-FHIR-TRANSACTION

**Purpose:** Execute multiple operations atomically (all-or-nothing)

**Schema:**
```json
{
  "name": "create-fhir-transaction",
  "description": "Create a FHIR transaction (batch operations)",
  "inputSchema": {
    "type": "object",
    "properties": {
      "resourceType": {
        "type": "string",
        "description": "Must be 'Bundle' with type 'transaction'"
      },
      "resource": {
        "type": "object",
        "description": "Bundle resource containing multiple entries"
      }
    },
    "required": ["resourceType", "resource"]
  }
}
```

**Example Call (Create Patient + Observations):**
```json
{
  "resourceType": "Bundle",
  "resource": {
    "resourceType": "Bundle",
    "type": "transaction",
    "entry": [
      {
        "request": {
          "method": "POST",
          "url": "Patient"
        },
        "resource": {
          "resourceType": "Patient",
          "identifier": [{"value": "TEMP-001"}],
          "name": [{"family": "Test", "given": ["John"]}]
        }
      },
      {
        "request": {
          "method": "POST",
          "url": "Observation"
        },
        "resource": {
          "resourceType": "Observation",
          "status": "final",
          "code": {
            "coding": [{
              "system": "http://loinc.org",
              "code": "8867-4",
              "display": "Heart rate"
            }]
          },
          "subject": {"reference": "Patient/TEMP-001"},
          "valueQuantity": {"value": 72, "unit": "bpm"}
        }
      }
    ]
  }
}
```

---

## MCP ARCHITECTURE

### SYSTEM FLOW DIAGRAM

```
┌─────────────────────────────────────────────────────────────────┐
│                  AI AGENT (MCP CLIENT)                           │
│  Examples: Claude Desktop, Custom Python App, NestJS Service    │
│                                                                  │
│  User Query: "Find all diabetic patients in Hamburg clinic"     │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ 1. MCP Request
                         │    Protocol: HTTP Streamable (SSE)
                         │    Endpoint: /mcp/message
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│               HAPI FHIR SERVER (Port 8080)                       │
│                                                                  │
│  ┌────────────────────────────────────────────────────────┐     │
│  │  McpServerConfig.java (Spring AI)                      │     │
│  │  - Registers MCP capabilities                          │     │
│  │  - Exposes endpoint: /mcp/message                      │     │
│  └──────────────────────┬─────────────────────────────────┘     │
│                         │                                        │
│  ┌────────────────────────────────────────────────────────┐     │
│  │  McpFhirBridge.java                                    │     │
│  │  - Implements McpBridge interface                      │     │
│  │  - generateTools() → Returns 9 tool specs              │     │
│  │  - getToolResult() → Handles tool execution            │     │
│  └──────────────────────┬─────────────────────────────────┘     │
│                         │                                        │
│  ┌────────────────────────────────────────────────────────┐     │
│  │  ToolFactory.java                                      │     │
│  │  - createFhirResource() → Tool definition #1           │     │
│  │  - readFhirResource() → Tool definition #2             │     │
│  │  - searchFhirResources() → Tool definition #8          │     │
│  │  - ... (all 9 tools)                                   │     │
│  └──────────────────────┬─────────────────────────────────┘     │
│                         │                                        │
│  ┌────────────────────────────────────────────────────────┐     │
│  │  RequestBuilder.java                                   │     │
│  │  - Converts MCP request → FHIR HTTP request            │     │
│  │  - Adds headers (Authorization, X-Clinic-ID)           │     │
│  └──────────────────────┬─────────────────────────────────┘     │
│                         │                                        │
│                         │ 2. FHIR REST API Call                  │
│                         │                                        │
│  ┌────────────────────────────────────────────────────────┐     │
│  │  RestfulServer (HAPI FHIR Core)                        │     │
│  │  - Handles FHIR requests                               │     │
│  │  - Applies interceptors (Auth, Partitioning)           │     │
│  └──────────────────────┬─────────────────────────────────┘     │
│                         │                                        │
│  ┌────────────────────────────────────────────────────────┐     │
│  │  INTERCEPTORS                                          │     │
│  │  ┌──────────────────────────────────────────────┐     │     │
│  │  │  ApiTokenAuthInterceptor                     │     │     │
│  │  │  - Validates Bearer token                    │     │     │
│  │  └──────────────────────────────────────────────┘     │     │
│  │  ┌──────────────────────────────────────────────┐     │     │
│  │  │  ClinicPartitionInterceptor                  │     │     │
│  │  │  - Maps X-Clinic-ID → Partition ID           │     │     │
│  │  │  - ddx-hamburg-clinic → Partition 1          │     │     │
│  │  └──────────────────────────────────────────────┘     │     │
│  └──────────────────────┬─────────────────────────────────┘     │
│                         │                                        │
└─────────────────────────┼────────────────────────────────────────┘
                          │
                          │ 3. JPA Query (Hibernate)
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│              POSTGRESQL DATABASE (Port 5432)                     │
│                     Database: ddx_hapifhir                       │
│                                                                  │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │  HFJ_RESOURCE   │  │  HFJ_RES_VER   │  │  HFJ_PARTITION  │ │
│  │  (Resources)    │  │  (Versions)    │  │  (Clinics)      │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│                                                                  │
│  Partitions:                                                     │
│  0 → DEFAULT (System)                                           │
│  1 → HAMBURG (ddx-hamburg-clinic)                               │
│  2 → BERLIN (ddx-berlin-clinic)                                 │
│  3 → MUNICH (ddx-munich-clinic)                                 │
│  4 → FRANKFURT (ddx-frankfurt-clinic)                           │
│  5 → COLOGNE (ddx-cologne-clinic)                               │
│  6 → SHARED (Cross-clinic resources)                            │
└─────────────────────────────────────────────────────────────────┘
```

### CODE IMPLEMENTATION

**File Locations:**
- `src/main/java/ca/uhn/fhir/rest/server/McpFhirBridge.java` (34-74)
- `src/main/java/ca/uhn/fhir/jpa/starter/mcp/ToolFactory.java` (251-329)
- `src/main/java/ca/uhn/fhir/jpa/starter/mcp/McpServerConfig.java`
- `src/main/java/ca/uhn/fhir/jpa/starter/mcp/RequestBuilder.java`
- `src/main/java/ca/uhn/fhir/jpa/starter/mcp/CallToolResultFactory.java`

**Key Code Snippet (McpFhirBridge.java):**
```java
public List<McpServerFeatures.SyncToolSpecification> generateTools() {
    return List.of(
        new McpServerFeatures.SyncToolSpecification.Builder()
            .tool(ToolFactory.createFhirResource())
            .callHandler((exchange, request) -> getToolResult(request, Interaction.CREATE))
            .build(),
        new McpServerFeatures.SyncToolSpecification.Builder()
            .tool(ToolFactory.searchFhirResources())
            .callHandler((exchange, request) -> getToolResult(request, Interaction.SEARCH))
            .build(),
        // ... 7 more tools
    );
}
```

---

## HOW TO ACCESS MCP

### MCP ENDPOINT URL

```
Protocol: HTTP Streamable (Server-Sent Events)
URL:      http://localhost:8080/mcp/message
Method:   POST (for tool calls)
```

### ENDPOINT CAPABILITIES

When you connect to the MCP endpoint, it exposes:

1. **Tools:** 9 FHIR operations (list above)
2. **Resources:** Enabled but not populated yet
3. **Prompts:** Enabled but not populated yet
4. **Notifications:** Enabled (server can push updates)

### PROTOCOL MESSAGES

**Initialize Connection:**
```json
{
  "jsonrpc": "2.0",
  "method": "initialize",
  "params": {
    "protocolVersion": "2024-11-05",
    "capabilities": {
      "tools": {}
    },
    "clientInfo": {
      "name": "my-ai-agent",
      "version": "1.0.0"
    }
  }
}
```

**List Available Tools:**
```json
{
  "jsonrpc": "2.0",
  "method": "tools/list"
}
```

**Response:**
```json
{
  "tools": [
    {
      "name": "create-fhir-resource",
      "description": "Create a new FHIR resource",
      "inputSchema": { ... }
    },
    {
      "name": "search-fhir-resources",
      "description": "Search existing FHIR resources",
      "inputSchema": { ... }
    }
    // ... 7 more tools
  ]
}
```

**Call a Tool:**
```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "search-fhir-resources",
    "arguments": {
      "resourceType": "Patient",
      "query": "name=Schmidt"
    }
  }
}
```

---

## CLINICAL USE CASES

### USE CASE 1: CLINICAL DECISION SUPPORT (AI DOCTOR ASSISTANT)

**Scenario:** Doctor reviewing patient, asks AI for context

**User Query:**
```
"What are Anna Schmidt's recent lab results? Any concerning trends?"
```

**AI Workflow:**

1. **Tool Call 1:** `search-fhir-resources`
   ```json
   {
     "resourceType": "Patient",
     "query": "name=Schmidt,given=Anna"
   }
   ```
   **Result:** Patient ID = 789

2. **Tool Call 2:** `search-fhir-resources`
   ```json
   {
     "resourceType": "Observation",
     "query": "patient=Patient/789,category=laboratory,_sort=-date,_count=20"
   }
   ```
   **Result:** 20 most recent lab results

3. **AI Analysis:** Processes LOINC codes, values, reference ranges

4. **AI Response:**
   ```
   Anna Schmidt's Recent Labs (Last 30 days):

   ⚠️ CONCERNING TRENDS:
   - HbA1c: 8.2% (↑ from 7.8%) - Above target <7%
   - LDL Cholesterol: 145 mg/dL (↑ from 130 mg/dL)

   ✓ STABLE:
   - Creatinine: 0.9 mg/dL (normal kidney function)
   - TSH: 2.1 mIU/L (normal thyroid)

   RECOMMENDATION: Consider diabetes medication adjustment
   ```

**Impact:** Doctor gets instant clinical context without clicking through EHR

---

### USE CASE 2: SMART PATIENT ONBOARDING

**Scenario:** Receptionist registering walk-in patient

**User Query:**
```
"Register new patient: Hans Müller, born March 15, 1985,
address Hauptstraße 42, Hamburg 20095, mobile 0170-1234567"
```

**AI Workflow:**

1. **Tool Call:** `create-fhir-resource`
   ```json
   {
     "resourceType": "Patient",
     "resource": {
       "resourceType": "Patient",
       "identifier": [{
         "system": "urn:ddx:temp-id",
         "value": "TEMP-" + timestamp
       }],
       "name": [{
         "family": "Müller",
         "given": ["Hans"]
       }],
       "birthDate": "1985-03-15",
       "gender": "male",
       "address": [{
         "line": ["Hauptstraße 42"],
         "city": "Hamburg",
         "postalCode": "20095",
         "country": "DE"
       }],
       "telecom": [{
         "system": "phone",
         "value": "+49-170-1234567",
         "use": "mobile"
       }]
     },
     "headers": {
       "X-Clinic-ID": "ddx-hamburg-clinic"
     }
   }
   ```

2. **FHIR Server Response:**
   ```json
   {
     "resourceType": "Patient",
     "id": "pt-1001",
     "meta": {
       "versionId": "1",
       "lastUpdated": "2025-11-25T10:30:00Z"
     }
   }
   ```

3. **AI Response:**
   ```
   ✓ Patient registered successfully

   Patient ID: pt-1001
   MRN: Will be assigned by system
   Clinic: Hamburg (Partition 1)

   Ready for appointment scheduling.
   ```

**Impact:** Natural language registration in seconds, no form filling

---

### USE CASE 3: MEDICATION RECONCILIATION

**Scenario:** Nurse preparing patient for surgery, needs medication list

**User Query:**
```
"Show me all active medications for patient ID pt-789.
Flag any beta-blockers or anticoagulants."
```

**AI Workflow:**

1. **Tool Call:** `search-fhir-resources`
   ```json
   {
     "resourceType": "MedicationRequest",
     "query": "patient=Patient/pt-789,status=active,_include=MedicationRequest:medication"
   }
   ```

2. **AI Analysis:**
   - Parses medication codes (ATC/RxNorm)
   - Identifies drug classes
   - Flags high-risk medications

3. **AI Response:**
   ```
   ACTIVE MEDICATIONS (Patient pt-789):

   🔴 HIGH-RISK - PRE-SURGERY CONSIDERATIONS:
   1. Metoprolol 50mg PO BID (Beta-blocker)
      → CONTINUE on day of surgery
   2. Rivaroxaban 20mg PO daily (Anticoagulant)
      → ⚠️ HOLD 24h before surgery, consult anesthesia

   REGULAR MEDICATIONS:
   3. Metformin 1000mg PO BID (Diabetes)
   4. Atorvastatin 40mg PO qHS (Cholesterol)
   5. Lisinopril 10mg PO daily (Blood pressure)

   Last reconciliation: 2025-11-20 by Dr. Schmidt
   ```

**Impact:** Instant medication review with clinical decision support

---

### USE CASE 4: DISCHARGE SUMMARY GENERATION

**Scenario:** Doctor completing patient discharge, needs summary

**User Query:**
```
"Generate discharge summary for patient pt-456, encounter enc-789"
```

**AI Workflow:**

1. **Tool Call 1:** Get patient demographics
   ```json
   {"resourceType": "Patient", "id": "pt-456"}
   ```

2. **Tool Call 2:** Get encounter details
   ```json
   {"resourceType": "Encounter", "id": "enc-789"}
   ```

3. **Tool Call 3:** Get diagnoses
   ```json
   {
     "resourceType": "Condition",
     "query": "patient=Patient/pt-456,encounter=Encounter/enc-789"
   }
   ```

4. **Tool Call 4:** Get procedures
   ```json
   {
     "resourceType": "Procedure",
     "query": "patient=Patient/pt-456,encounter=Encounter/enc-789"
   }
   ```

5. **Tool Call 5:** Get discharge medications
   ```json
   {
     "resourceType": "MedicationRequest",
     "query": "patient=Patient/pt-456,authoredon=ge2025-11-20"
   }
   ```

6. **AI Synthesis:**
   ```markdown
   DISCHARGE SUMMARY

   Patient: Schmidt, Anna (Female, DOB: 1990-05-20)
   MRN: 123456
   Admission: 2025-11-20
   Discharge: 2025-11-25
   Length of Stay: 5 days

   ADMISSION DIAGNOSIS:
   Acute appendicitis (ICD-10: K35.80)

   HOSPITAL COURSE:
   Patient presented with acute right lower quadrant pain.
   CT scan confirmed acute appendicitis. Underwent laparoscopic
   appendectomy on 2025-11-21 without complications.

   PROCEDURES PERFORMED:
   - Laparoscopic appendectomy (2025-11-21, Dr. Weber)

   DISCHARGE MEDICATIONS:
   1. Ibuprofen 400mg PO Q6H PRN pain x 7 days
   2. Amoxicillin/Clavulanate 875mg PO BID x 7 days

   DISCHARGE INSTRUCTIONS:
   - Follow up with Dr. Schmidt in 1 week
   - Wound care: Keep incisions clean and dry
   - Activity: Light activity, no heavy lifting x 2 weeks

   DISCHARGE DISPOSITION: Home
   ```

**Impact:** Complete discharge summary in 30 seconds vs. 15 minutes manual

---

### USE CASE 5: APPOINTMENT CONFLICT DETECTION

**Scenario:** Scheduler booking follow-up appointment

**User Query:**
```
"Book follow-up appointment for patient Anna Schmidt with
Dr. Weber on December 1st at 10:00 AM"
```

**AI Workflow:**

1. **Tool Call 1:** Find patient
   ```json
   {
     "resourceType": "Patient",
     "query": "name=Schmidt,given=Anna"
   }
   ```

2. **Tool Call 2:** Check Dr. Weber's schedule
   ```json
   {
     "resourceType": "Appointment",
     "query": "practitioner=Practitioner/Dr-Weber,date=2025-12-01,status=booked"
   }
   ```

3. **AI Analysis:** Detects 10:00 AM slot already has 2 appointments

4. **AI Response:**
   ```
   ⚠️ SCHEDULING CONFLICT

   Dr. Weber's Schedule - December 1, 2025:

   10:00 AM - FULL (2 patients already booked)
   ├─ 10:00 - Patient: Mueller, Hans (Follow-up)
   └─ 10:30 - Patient: Becker, Lisa (New patient)

   AVAILABLE ALTERNATIVES:
   ✓ 09:00 AM - Available (30 min slot)
   ✓ 11:30 AM - Available (30 min slot)
   ✓ 14:00 PM - Available (60 min slot)

   Would you like to book one of these times?
   ```

**Impact:** Prevents double-booking, suggests alternatives automatically

---

### USE CASE 6: POPULATION HEALTH QUERIES

**Scenario:** Clinic administrator analyzing patient population

**User Query:**
```
"How many patients in Hamburg clinic have Type 2 Diabetes
and HbA1c > 8% in the last 3 months?"
```

**AI Workflow:**

1. **Tool Call 1:** Find diabetic patients
   ```json
   {
     "resourceType": "Condition",
     "query": "code=E11,clinical-status=active"
   }
   ```

2. **Tool Call 2:** Get recent HbA1c results for those patients
   ```json
   {
     "resourceType": "Observation",
     "query": "code=4548-4,date=ge2025-08-25,value-quantity=gt8"
   }
   ```

3. **AI Analysis:** Cross-references patient lists, counts unique patients

4. **AI Response:**
   ```
   DIABETES POPULATION ANALYSIS - Hamburg Clinic

   Total Type 2 Diabetes patients: 247

   Recent HbA1c Testing (Last 3 months):
   - Tested: 189 patients (76.5%)
   - Not tested: 58 patients (23.5%)

   HbA1c > 8% (Poor Control):
   - Count: 42 patients (22.2% of tested)

   ACTION ITEMS:
   1. Schedule 58 patients for HbA1c testing
   2. Review medication/lifestyle for 42 uncontrolled patients
   3. Consider diabetes education program expansion

   Detailed patient list available on request.
   ```

**Impact:** Population health insights without SQL queries or reports

---

### USE CASE 7: ADVERSE EVENT MONITORING

**Scenario:** Pharmacist checking for potential adverse drug reactions

**User Query:**
```
"Check if any patients started on statins in the last month
have reported muscle pain or elevated CK levels"
```

**AI Workflow:**

1. **Tool Call 1:** Find recent statin prescriptions
   ```json
   {
     "resourceType": "MedicationRequest",
     "query": "medication.code:text=statin,authoredon=ge2025-10-25"
   }
   ```

2. **Tool Call 2:** Check for muscle pain symptoms
   ```json
   {
     "resourceType": "Condition",
     "query": "code=M79.1,recorded-date=ge2025-10-25"
   }
   ```

3. **Tool Call 3:** Check for elevated CK labs
   ```json
   {
     "resourceType": "Observation",
     "query": "code=2157-6,date=ge2025-10-25,value-quantity=gt200"
   }
   ```

4. **AI Analysis:** Correlates medication start dates with symptom onset

5. **AI Response:**
   ```
   ⚠️ POTENTIAL ADVERSE DRUG REACTIONS DETECTED

   Statin Prescriptions (Last 30 days): 18 patients

   ALERTS (3 patients):

   1. Patient: Mueller, Hans (pt-1001)
      - Medication: Atorvastatin 80mg started 2025-11-01
      - Symptom: Muscle pain (myalgia) reported 2025-11-10
      - Lab: CK 450 U/L (↑↑ from baseline 120)
      - STATUS: ⚠️ POSSIBLE STATIN MYOPATHY
      - ACTION: Consider dose reduction or switch

   2. Patient: Becker, Lisa (pt-1005)
      - Medication: Rosuvastatin 20mg started 2025-10-28
      - Symptom: Muscle weakness reported 2025-11-15
      - Lab: CK 380 U/L (elevated)
      - STATUS: ⚠️ MONITOR CLOSELY

   3. Patient: Schmidt, Peter (pt-1012)
      - Medication: Simvastatin 40mg started 2025-11-05
      - Symptom: Fatigue reported 2025-11-18
      - Lab: CK 150 U/L (normal range)
      - STATUS: ℹ️ MONITOR, likely unrelated

   RECOMMENDATION: Review with prescribing physicians
   ```

**Impact:** Proactive adverse event detection, patient safety improvement

---

## INTEGRATION METHODS

### METHOD 1: CLAUDE DESKTOP (ANTHROPIC)

**Best For:** Manual testing, clinical staff AI assistant

**Setup:**

1. **Install Claude Desktop:**
   ```bash
   # Download from: https://claude.ai/download
   ```

2. **Configure MCP Server:**

   **File:** `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS)

   ```json
   {
     "mcpServers": {
       "dudoxx-fhir": {
         "command": "npx",
         "args": [
           "@modelcontextprotocol/server-http",
           "http://localhost:8080/mcp/message"
         ],
         "env": {
           "FHIR_AUTH_TOKEN": "ddx-api-token-2024",
           "FHIR_CLINIC_ID": "ddx-hamburg-clinic"
         }
       }
     }
   }
   ```

3. **Restart Claude Desktop**

4. **Test:**
   ```
   User: "List all tools available"
   Claude: Shows 9 FHIR tools

   User: "Search for patients named Schmidt"
   Claude: Uses search-fhir-resources tool automatically
   ```

**Use Cases:**
- Doctors using Claude for clinical decision support
- Nurses getting patient information via chat
- Administrators running population health queries

---

### METHOD 2: CUSTOM PYTHON APPLICATION

**Best For:** Data science, batch processing, custom AI workflows

**Setup:**

```python
import anthropic
import os

# Initialize Anthropic client
client = anthropic.Anthropic(
    api_key=os.environ.get("ANTHROPIC_API_KEY")
)

# Define FHIR tools (from your MCP server)
FHIR_TOOLS = [
    {
        "name": "search-fhir-resources",
        "description": "Search FHIR resources by type and query parameters",
        "input_schema": {
            "type": "object",
            "properties": {
                "resourceType": {
                    "type": "string",
                    "description": "FHIR resource type (Patient, Observation, etc.)"
                },
                "query": {
                    "type": "string",
                    "description": "Search parameters (e.g., 'name=Schmidt,birthdate=1990')"
                }
            },
            "required": ["resourceType", "query"]
        }
    },
    {
        "name": "create-fhir-resource",
        "description": "Create a new FHIR resource",
        "input_schema": {
            "type": "object",
            "properties": {
                "resourceType": {"type": "string"},
                "resource": {"type": "object"}
            },
            "required": ["resourceType", "resource"]
        }
    }
    # ... add other 7 tools
]

# Function to call FHIR MCP server
def call_fhir_tool(tool_name: str, arguments: dict) -> dict:
    """Execute FHIR tool via MCP endpoint"""
    import requests

    response = requests.post(
        "http://localhost:8080/mcp/message",
        json={
            "jsonrpc": "2.0",
            "method": "tools/call",
            "params": {
                "name": tool_name,
                "arguments": arguments
            }
        },
        headers={
            "Authorization": "Bearer ddx-api-token-2024",
            "X-Clinic-ID": "ddx-hamburg-clinic"
        }
    )
    return response.json()

# AI Agent with tool use
def ai_clinical_query(user_question: str) -> str:
    """Process clinical question with AI + FHIR tools"""

    message = client.messages.create(
        model="claude-3-5-sonnet-20241022",
        max_tokens=4096,
        tools=FHIR_TOOLS,
        messages=[
            {"role": "user", "content": user_question}
        ]
    )

    # Check if AI wants to use a tool
    if message.stop_reason == "tool_use":
        tool_use = next(block for block in message.content if block.type == "tool_use")

        # Call FHIR server via MCP
        tool_result = call_fhir_tool(tool_use.name, tool_use.input)

        # Send result back to AI
        response = client.messages.create(
            model="claude-3-5-sonnet-20241022",
            max_tokens=4096,
            tools=FHIR_TOOLS,
            messages=[
                {"role": "user", "content": user_question},
                {"role": "assistant", "content": message.content},
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "tool_result",
                            "tool_use_id": tool_use.id,
                            "content": str(tool_result)
                        }
                    ]
                }
            ]
        )

        return response.content[0].text

    return message.content[0].text

# Usage Example
if __name__ == "__main__":
    question = "Find all diabetic patients and show their latest HbA1c"
    answer = ai_clinical_query(question)
    print(answer)
```

**Output Example:**
```
Found 247 diabetic patients in Hamburg clinic.

Recent HbA1c Results (Last 3 months):
- Excellent control (<7%): 98 patients (39.7%)
- Good control (7-8%): 89 patients (36.0%)
- Poor control (>8%): 42 patients (17.0%)
- Not tested: 18 patients (7.3%)

Top 5 patients needing intervention:
1. Mueller, Hans - HbA1c 10.2% (last test: 2025-11-20)
2. Becker, Lisa - HbA1c 9.8% (last test: 2025-11-18)
...
```

---

### METHOD 3: NESTJS BACKEND INTEGRATION

**Best For:** Production API endpoints for frontend, server-side AI

**Setup:**

**File:** `dudoxx-api-clinic/src/modules/ai/fhir-ai.service.ts`

```typescript
import { Injectable } from '@nestjs/common';
import Anthropic from '@anthropic-ai/sdk';

@Injectable()
export class FhirAIService {
  private anthropic: Anthropic;

  constructor() {
    this.anthropic = new Anthropic({
      apiKey: process.env.ANTHROPIC_API_KEY,
    });
  }

  /**
   * Query FHIR data using natural language
   */
  async queryFhirWithAI(
    userPrompt: string,
    clinicId: string = 'ddx-hamburg-clinic'
  ): Promise<string> {

    const tools = this.getFhirMcpTools();

    const response = await this.anthropic.messages.create({
      model: 'claude-3-5-sonnet-20241022',
      max_tokens: 4096,
      tools: tools,
      messages: [
        {
          role: 'user',
          content: userPrompt,
        },
      ],
    });

    // Handle tool use
    if (response.stop_reason === 'tool_use') {
      const toolUse = response.content.find(
        (block) => block.type === 'tool_use'
      );

      if (toolUse && toolUse.type === 'tool_use') {
        // Call FHIR MCP endpoint
        const toolResult = await this.callFhirMcpTool(
          toolUse.name,
          toolUse.input,
          clinicId
        );

        // Send result back to AI
        const finalResponse = await this.anthropic.messages.create({
          model: 'claude-3-5-sonnet-20241022',
          max_tokens: 4096,
          tools: tools,
          messages: [
            { role: 'user', content: userPrompt },
            { role: 'assistant', content: response.content },
            {
              role: 'user',
              content: [
                {
                  type: 'tool_result',
                  tool_use_id: toolUse.id,
                  content: JSON.stringify(toolResult),
                },
              ],
            },
          ],
        });

        return finalResponse.content[0].text;
      }
    }

    return response.content[0].text;
  }

  /**
   * Call FHIR MCP tool via HTTP
   */
  private async callFhirMcpTool(
    toolName: string,
    arguments: any,
    clinicId: string
  ): Promise<any> {
    const response = await fetch('http://localhost:8080/mcp/message', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ddx-api-token-2024',
        'X-Clinic-ID': clinicId,
      },
      body: JSON.stringify({
        jsonrpc: '2.0',
        method: 'tools/call',
        params: {
          name: toolName,
          arguments: arguments,
        },
      }),
    });

    return response.json();
  }

  /**
   * Get FHIR MCP tool definitions
   */
  private getFhirMcpTools(): Anthropic.Tool[] {
    return [
      {
        name: 'search-fhir-resources',
        description: 'Search FHIR resources by type and query parameters',
        input_schema: {
          type: 'object',
          properties: {
            resourceType: {
              type: 'string',
              description: 'FHIR resource type (Patient, Observation, Condition, etc.)',
            },
            query: {
              type: 'string',
              description: 'Comma-separated search parameters (e.g., "name=Schmidt,birthdate=1990")',
            },
          },
          required: ['resourceType', 'query'],
        },
      },
      {
        name: 'create-fhir-resource',
        description: 'Create a new FHIR resource',
        input_schema: {
          type: 'object',
          properties: {
            resourceType: { type: 'string' },
            resource: { type: 'object' },
          },
          required: ['resourceType', 'resource'],
        },
      },
      // ... add other 7 tools
    ];
  }
}
```

**Controller:**

```typescript
import { Controller, Post, Body, UseGuards } from '@nestjs/common';
import { FhirAIService } from './fhir-ai.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';

@Controller('api/v1/ai/fhir')
@UseGuards(JwtAuthGuard)
export class FhirAIController {
  constructor(private readonly fhirAIService: FhirAIService) {}

  @Post('query')
  async queryFhir(
    @Body() body: { question: string; clinicId?: string }
  ) {
    return {
      answer: await this.fhirAIService.queryFhirWithAI(
        body.question,
        body.clinicId
      ),
    };
  }
}
```

**Frontend Usage (Next.js):**

```typescript
// Server Action
'use server'
export async function askFhirAI(question: string) {
  const response = await fetch('http://localhost:4100/api/v1/ai/fhir/query', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${process.env.API_TOKEN}`,
    },
    body: JSON.stringify({ question }),
  });

  return response.json();
}

// Component
'use client'
export function ClinicalAIChat() {
  const [question, setQuestion] = useState('');
  const [answer, setAnswer] = useState('');

  const handleSubmit = async () => {
    const result = await askFhirAI(question);
    setAnswer(result.answer);
  };

  return (
    <div>
      <input
        value={question}
        onChange={(e) => setQuestion(e.target.value)}
        placeholder="Ask about patients, conditions, medications..."
      />
      <button onClick={handleSubmit}>Ask AI</button>
      <div>{answer}</div>
    </div>
  );
}
```

---

### METHOD 4: DIRECT HTTP INTEGRATION (ANY LANGUAGE)

**Best For:** Non-JavaScript environments, custom integrations

**Example (cURL):**

```bash
# Step 1: Initialize MCP connection
curl -X POST http://localhost:8080/mcp/message \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ddx-api-token-2024" \
  -H "X-Clinic-ID: ddx-hamburg-clinic" \
  -d '{
    "jsonrpc": "2.0",
    "method": "initialize",
    "params": {
      "protocolVersion": "2024-11-05",
      "capabilities": {"tools": {}},
      "clientInfo": {"name": "curl-test", "version": "1.0"}
    }
  }'

# Step 2: List available tools
curl -X POST http://localhost:8080/mcp/message \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ddx-api-token-2024" \
  -H "X-Clinic-ID: ddx-hamburg-clinic" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/list"
  }'

# Step 3: Call a tool (search patients)
curl -X POST http://localhost:8080/mcp/message \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ddx-api-token-2024" \
  -H "X-Clinic-ID: ddx-hamburg-clinic" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "search-fhir-resources",
      "arguments": {
        "resourceType": "Patient",
        "query": "name=Schmidt"
      }
    }
  }'
```

---

## SECURITY CONSIDERATIONS

### ⚠️ CRITICAL: CURRENT SECURITY ISSUES

**PROBLEM:** Your MCP endpoint is currently **UNAUTHENTICATED**

**Evidence:** Code analysis shows no authentication check in `McpFhirBridge.java`

**Risks:**
```
❌ Anyone with network access can use MCP tools
❌ No clinic partition enforcement via MCP
❌ No audit trail for MCP operations
❌ Potential data leakage between clinics
❌ No rate limiting
❌ No user attribution for actions
```

### 🛡️ RECOMMENDED SECURITY ENHANCEMENTS

#### ENHANCEMENT 1: ADD MCP AUTHENTICATION INTERCEPTOR

**File:** `src/main/java/ca/uhn/fhir/jpa/starter/interceptor/McpAuthInterceptor.java`

```java
package ca.uhn.fhir.jpa.starter.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class McpAuthInterceptor {

    private static final String VALID_MCP_TOKEN = "ddx-mcp-token-2024";

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
    public void authenticateMcpRequest(RequestDetails requestDetails) {
        String path = requestDetails.getRequestPath();

        // Only intercept MCP endpoints
        if (path != null && path.startsWith("/mcp/")) {

            // Validate Authorization header
            String authHeader = requestDetails.getHeader("Authorization");
            if (authHeader == null || !authHeader.equals("Bearer " + VALID_MCP_TOKEN)) {
                throw new AuthenticationException("Invalid MCP authentication token");
            }

            // Validate X-Clinic-ID header (required for partitioning)
            String clinicId = requestDetails.getHeader("X-Clinic-ID");
            if (clinicId == null || clinicId.isEmpty()) {
                throw new AuthenticationException("Missing X-Clinic-ID header for MCP request");
            }

            // Validate clinic ID is recognized
            if (!isValidClinicId(clinicId)) {
                throw new AuthenticationException("Invalid clinic ID: " + clinicId);
            }
        }
    }

    private boolean isValidClinicId(String clinicId) {
        return clinicId.matches("^ddx-(hamburg|berlin|munich|frankfurt|cologne|shared)-clinic$");
    }
}
```

**Register Interceptor:**

**File:** `src/main/java/ca/uhn/fhir/jpa/starter/common/StarterJpaConfig.java`

```java
@Bean
public McpAuthInterceptor mcpAuthInterceptor() {
    return new McpAuthInterceptor();
}
```

---

#### ENHANCEMENT 2: AUDIT LOGGING FOR MCP OPERATIONS

**File:** `src/main/java/ca/uhn/fhir/jpa/starter/interceptor/McpAuditInterceptor.java`

```java
package ca.uhn.fhir.jpa.starter.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class McpAuditInterceptor {

    private static final Logger auditLogger = LoggerFactory.getLogger("MCP_AUDIT");

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
    public void auditMcpRequest(RequestDetails requestDetails) {
        String path = requestDetails.getRequestPath();

        if (path != null && path.startsWith("/mcp/")) {
            String clinicId = requestDetails.getHeader("X-Clinic-ID");
            String method = requestDetails.getRequestType().name();
            String ip = requestDetails.getHeader("X-Forwarded-For");

            auditLogger.info(
                "MCP_REQUEST | Clinic: {} | Method: {} | Path: {} | IP: {}",
                clinicId, method, path, ip
            );
        }
    }
}
```

**Add to Logging Database:**

Store in `ddx_clinic_fhir_logging` database:

```sql
CREATE TABLE mcp_audit_log (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    clinic_id VARCHAR(50) NOT NULL,
    tool_name VARCHAR(100),
    arguments JSONB,
    result_status VARCHAR(20),
    user_agent TEXT,
    ip_address INET,
    execution_time_ms INTEGER
);

CREATE INDEX idx_mcp_audit_clinic ON mcp_audit_log(clinic_id, timestamp);
CREATE INDEX idx_mcp_audit_tool ON mcp_audit_log(tool_name);
```

---

#### ENHANCEMENT 3: RATE LIMITING

**File:** `application.yaml`

```yaml
spring:
  ai:
    mcp:
      server:
        rate-limit:
          enabled: true
          max-requests-per-minute: 60
          max-requests-per-hour: 1000
```

**Implementation:**

```java
@Component
public class McpRateLimitInterceptor {

    private final LoadingCache<String, AtomicInteger> requestCountsPerMinute;

    public McpRateLimitInterceptor() {
        this.requestCountsPerMinute = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(new CacheLoader<String, AtomicInteger>() {
                @Override
                public AtomicInteger load(String key) {
                    return new AtomicInteger(0);
                }
            });
    }

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
    public void checkRateLimit(RequestDetails requestDetails) {
        String path = requestDetails.getRequestPath();

        if (path != null && path.startsWith("/mcp/")) {
            String clinicId = requestDetails.getHeader("X-Clinic-ID");

            try {
                AtomicInteger count = requestCountsPerMinute.get(clinicId);

                if (count.incrementAndGet() > 60) {
                    throw new TooManyRequestsException("Rate limit exceeded for clinic: " + clinicId);
                }
            } catch (ExecutionException e) {
                throw new InternalErrorException("Rate limiting error", e);
            }
        }
    }
}
```

---

#### ENHANCEMENT 4: RESTRICT MCP TO INTERNAL NETWORK

**File:** `application.yaml`

```yaml
hapi:
  fhir:
    mcp:
      allowed-ips:
        - "127.0.0.1"           # Localhost
        - "10.0.0.0/8"          # Internal network
        - "172.16.0.0/12"       # Docker network
        - "192.168.0.0/16"      # Private network
```

**Implementation:**

```java
@Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
public void validateMcpIpAddress(RequestDetails requestDetails) {
    String path = requestDetails.getRequestPath();

    if (path != null && path.startsWith("/mcp/")) {
        String clientIp = requestDetails.getHeader("X-Real-IP");

        if (!isAllowedIp(clientIp)) {
            throw new ForbiddenOperationException("MCP access denied from IP: " + clientIp);
        }
    }
}
```

---

#### ENHANCEMENT 5: TOOL-LEVEL PERMISSIONS

**File:** `src/main/java/ca/uhn/fhir/jpa/starter/mcp/McpToolPermissions.java`

```java
public enum McpToolPermissions {

    // Read-only tools (safe for all users)
    READ_RESOURCE("read-fhir-resource", "READ"),
    SEARCH_RESOURCES("search-fhir-resources", "READ"),

    // Write tools (require elevated permissions)
    CREATE_RESOURCE("create-fhir-resource", "WRITE"),
    UPDATE_RESOURCE("update-fhir-resource", "WRITE"),
    DELETE_RESOURCE("delete-fhir-resource", "DELETE"),

    // Dangerous tools (admin only)
    TRANSACTION("create-fhir-transaction", "ADMIN");

    private final String toolName;
    private final String requiredRole;

    // Check if user has permission
    public static boolean canUseTool(String toolName, String userRole) {
        // Implementation
    }
}
```

---

### SECURITY CHECKLIST FOR PRODUCTION

- [ ] **Authentication:** Implement `McpAuthInterceptor`
- [ ] **Authorization:** Tool-level permissions by user role
- [ ] **Audit Logging:** Log all MCP operations to database
- [ ] **Rate Limiting:** Prevent abuse (60 req/min per clinic)
- [ ] **IP Whitelisting:** Restrict to internal network
- [ ] **HTTPS Only:** Force SSL/TLS for MCP endpoint
- [ ] **Token Rotation:** Change MCP token regularly
- [ ] **Partition Validation:** Ensure X-Clinic-ID matches user's clinic
- [ ] **Input Validation:** Sanitize all tool arguments
- [ ] **Output Filtering:** Redact sensitive data (SSN, etc.)

---

## TESTING WITH MCP INSPECTOR

### INSTALL MCP INSPECTOR

```bash
# Install globally
npm install -g @modelcontextprotocol/inspector

# Or run directly
npx @modelcontextprotocol/inspector
```

### LAUNCH INSPECTOR

```bash
npx @modelcontextprotocol/inspector
```

**Browser Opens:** http://localhost:5173

### CONNECT TO FHIR SERVER

**In Inspector UI:**

1. **Transport:** Select "HTTP Streamable"
2. **URL:** Enter `http://localhost:8080/mcp/message`
3. **Headers:**
   ```
   Authorization: Bearer ddx-api-token-2024
   X-Clinic-ID: ddx-hamburg-clinic
   ```
4. **Click:** "Connect"

### INSPECT CAPABILITIES

**Inspector Shows:**

```
✓ Connected to HAPI FHIR MCP Server

Capabilities:
├─ Tools: 9 available
├─ Resources: Enabled
├─ Prompts: Enabled
└─ Notifications: Enabled

Tools:
├─ create-fhir-resource
├─ read-fhir-resource
├─ update-fhir-resource
├─ conditional-update-fhir-resource
├─ delete-fhir-resource
├─ patch-fhir-resource
├─ conditional-patch-fhir-resource
├─ search-fhir-resources
└─ create-fhir-transaction
```

### TEST TOOL EXECUTION

**Example 1: Search Patients**

```json
{
  "tool": "search-fhir-resources",
  "arguments": {
    "resourceType": "Patient",
    "query": "name=Schmidt"
  }
}
```

**Result:**
```json
{
  "status": "success",
  "content": {
    "resourceType": "Bundle",
    "total": 3,
    "entry": [...]
  }
}
```

**Example 2: Create Patient**

```json
{
  "tool": "create-fhir-resource",
  "arguments": {
    "resourceType": "Patient",
    "resource": {
      "resourceType": "Patient",
      "name": [{"family": "Test", "given": ["Inspector"]}],
      "gender": "unknown"
    }
  }
}
```

**Result:**
```json
{
  "status": "success",
  "content": {
    "resourceType": "Patient",
    "id": "test-123",
    "meta": {"versionId": "1"}
  }
}
```

---

## TUTORIAL: BUILD AI MEDICAL ASSISTANT

### STEP 1: SETUP ENVIRONMENT

```bash
# Clone repository
cd dudoxx-fhir-server

# Ensure server is running
./start-server.sh

# Verify MCP endpoint
curl http://localhost:8080/mcp/message
```

### STEP 2: INSTALL DEPENDENCIES

```bash
# Install Python dependencies
pip install anthropic requests python-dotenv

# Or Node.js
npm install @anthropic-ai/sdk node-fetch
```

### STEP 3: CREATE AI ASSISTANT

**File:** `ai-medical-assistant.py`

```python
#!/usr/bin/env python3
"""
Dudoxx AI Medical Assistant
Uses MCP to interact with HAPI FHIR server
"""

import os
import anthropic
import requests
from typing import Dict, Any

# Configuration
ANTHROPIC_API_KEY = os.getenv("ANTHROPIC_API_KEY")
FHIR_MCP_ENDPOINT = "http://localhost:8080/mcp/message"
FHIR_AUTH_TOKEN = "ddx-api-token-2024"
CLINIC_ID = "ddx-hamburg-clinic"

# Initialize Anthropic client
client = anthropic.Anthropic(api_key=ANTHROPIC_API_KEY)

# FHIR Tool Definitions
FHIR_TOOLS = [
    {
        "name": "search-fhir-resources",
        "description": "Search for FHIR resources (patients, observations, conditions, etc.)",
        "input_schema": {
            "type": "object",
            "properties": {
                "resourceType": {
                    "type": "string",
                    "description": "Type of resource to search (Patient, Observation, Condition, MedicationRequest, etc.)"
                },
                "query": {
                    "type": "string",
                    "description": "Search parameters separated by commas (e.g., 'name=Schmidt,birthdate=1990')"
                }
            },
            "required": ["resourceType", "query"]
        }
    },
    {
        "name": "read-fhir-resource",
        "description": "Read a specific FHIR resource by ID",
        "input_schema": {
            "type": "object",
            "properties": {
                "resourceType": {"type": "string"},
                "id": {"type": "string"}
            },
            "required": ["resourceType", "id"]
        }
    },
    {
        "name": "create-fhir-resource",
        "description": "Create a new FHIR resource",
        "input_schema": {
            "type": "object",
            "properties": {
                "resourceType": {"type": "string"},
                "resource": {"type": "object"}
            },
            "required": ["resourceType", "resource"]
        }
    }
]

def call_fhir_tool(tool_name: str, arguments: Dict[str, Any]) -> Dict[str, Any]:
    """Call FHIR MCP tool via HTTP"""
    response = requests.post(
        FHIR_MCP_ENDPOINT,
        json={
            "jsonrpc": "2.0",
            "method": "tools/call",
            "params": {
                "name": tool_name,
                "arguments": arguments
            }
        },
        headers={
            "Authorization": f"Bearer {FHIR_AUTH_TOKEN}",
            "X-Clinic-ID": CLINIC_ID,
            "Content-Type": "application/json"
        }
    )
    return response.json()

def ask_medical_question(question: str) -> str:
    """Process medical question with AI + FHIR tools"""

    messages = [{"role": "user", "content": question}]

    # First AI call
    response = client.messages.create(
        model="claude-3-5-sonnet-20241022",
        max_tokens=4096,
        tools=FHIR_TOOLS,
        messages=messages
    )

    # Handle tool use loop
    while response.stop_reason == "tool_use":
        # Find tool use block
        tool_use = next(
            (block for block in response.content if block.type == "tool_use"),
            None
        )

        if not tool_use:
            break

        # Execute tool
        print(f"🔧 Using tool: {tool_use.name}")
        tool_result = call_fhir_tool(tool_use.name, tool_use.input)

        # Add assistant response and tool result to messages
        messages.append({"role": "assistant", "content": response.content})
        messages.append({
            "role": "user",
            "content": [{
                "type": "tool_result",
                "tool_use_id": tool_use.id,
                "content": str(tool_result)
            }]
        })

        # Get next AI response
        response = client.messages.create(
            model="claude-3-5-sonnet-20241022",
            max_tokens=4096,
            tools=FHIR_TOOLS,
            messages=messages
        )

    # Extract final text response
    return next(
        (block.text for block in response.content if hasattr(block, "text")),
        "No response generated"
    )

def main():
    """Interactive medical assistant"""
    print("🏥 Dudoxx AI Medical Assistant")
    print("=" * 60)
    print(f"Connected to: {CLINIC_ID}")
    print("Type 'exit' to quit\n")

    while True:
        question = input("You: ").strip()

        if question.lower() in ['exit', 'quit']:
            print("Goodbye!")
            break

        if not question:
            continue

        try:
            answer = ask_medical_question(question)
            print(f"\n🤖 Assistant: {answer}\n")
        except Exception as e:
            print(f"❌ Error: {str(e)}\n")

if __name__ == "__main__":
    main()
```

### STEP 4: RUN ASSISTANT

```bash
# Set API key
export ANTHROPIC_API_KEY="sk-ant-..."

# Run assistant
python ai-medical-assistant.py
```

### STEP 5: TEST QUERIES

```
🏥 Dudoxx AI Medical Assistant
============================================================
Connected to: ddx-hamburg-clinic
Type 'exit' to quit

You: Find all patients named Schmidt

🔧 Using tool: search-fhir-resources

🤖 Assistant: I found 3 patients with the last name Schmidt in Hamburg clinic:

1. Schmidt, Anna (Female, DOB: 1990-05-20)
   - Patient ID: pt-789
   - MRN: 123456

2. Schmidt, Peter (Male, DOB: 1975-03-12)
   - Patient ID: pt-1012
   - MRN: 123789

3. Schmidt, Maria (Female, DOB: 2010-11-30)
   - Patient ID: pt-2045
   - MRN: 124001

Would you like more details about any of these patients?

You: Show me Anna Schmidt's recent lab results

🔧 Using tool: search-fhir-resources

🤖 Assistant: Here are Anna Schmidt's recent laboratory results:

**Most Recent Labs (Last 30 days):**

1. HbA1c - 8.2% (2025-11-20)
   ⚠️ Above target (<7% for diabetics)

2. Fasting Glucose - 145 mg/dL (2025-11-18)
   ⚠️ Elevated (target: 70-130 mg/dL)

3. Total Cholesterol - 210 mg/dL (2025-11-15)
   ℹ️ Borderline high

4. Creatinine - 0.9 mg/dL (2025-11-15)
   ✓ Normal (kidney function good)

**Clinical Recommendations:**
- Consider adjusting diabetes medication
- Dietary counseling recommended
- Follow-up HbA1c in 3 months

Would you like to see her full medication list?
```

---

## ADVANCED FEATURES

### FEATURE 1: PROMPT TEMPLATES

**What:** Pre-defined clinical workflows

**Example Configuration:**

**File:** `src/main/java/ca/uhn/fhir/rest/server/McpFhirPrompts.java`

```java
@Component
public class McpFhirPrompts implements McpPromptProvider {

    @Override
    public List<Prompt> getPrompts() {
        return List.of(

            // Discharge Summary Prompt
            Prompt.builder()
                .name("generate-discharge-summary")
                .description("Generate comprehensive discharge summary")
                .arguments(List.of(
                    PromptArgument.builder()
                        .name("patientId")
                        .description("Patient ID (e.g., Patient/123)")
                        .required(true)
                        .build(),
                    PromptArgument.builder()
                        .name("encounterId")
                        .description("Encounter ID (e.g., Encounter/456)")
                        .required(true)
                        .build()
                ))
                .template("""
                    Generate a discharge summary for:
                    - Patient: {{patientId}}
                    - Encounter: {{encounterId}}

                    Include:
                    1. Patient demographics
                    2. Admission/discharge dates
                    3. Diagnoses (ICD-10)
                    4. Procedures performed
                    5. Discharge medications
                    6. Follow-up instructions

                    Use search-fhir-resources to gather all required data.
                    Format as professional medical document.
                """)
                .build(),

            // Medication Reconciliation Prompt
            Prompt.builder()
                .name("medication-reconciliation")
                .description("Compare home medications with hospital medications")
                .arguments(List.of(
                    PromptArgument.builder()
                        .name("patientId")
                        .required(true)
                        .build()
                ))
                .template("""
                    Perform medication reconciliation for patient {{patientId}}:

                    1. List all active home medications
                    2. List all hospital medications
                    3. Identify discrepancies
                    4. Flag high-risk medications (anticoagulants, insulin, etc.)
                    5. Recommend actions
                """)
                .build(),

            // Population Health Query
            Prompt.builder()
                .name("diabetes-population-analysis")
                .description("Analyze diabetic patient population")
                .template("""
                    Generate diabetes population health report:

                    1. Find all patients with Type 2 Diabetes (E11)
                    2. Get recent HbA1c results (last 3 months)
                    3. Categorize by control level:
                       - Excellent: HbA1c < 7%
                       - Good: HbA1c 7-8%
                       - Poor: HbA1c > 8%
                    4. Identify patients missing recent HbA1c
                    5. Generate action items
                """)
                .build()
        );
    }
}
```

**Usage:**

```python
# Call prompt instead of writing complex query
response = call_mcp_prompt(
    "generate-discharge-summary",
    {"patientId": "Patient/789", "encounterId": "Encounter/1234"}
)
```

---

### FEATURE 2: RESOURCE STREAMING

**What:** Stream large result sets instead of loading all at once

**Implementation:**

```java
@Component
public class McpResourceProvider {

    public Stream<Resource> streamPatients(String query) {
        // Return patient iterator
        return patientRepository.findByQuery(query)
            .stream()
            .map(this::toFhirResource);
    }
}
```

**Client Side:**

```python
# Stream 10,000 patients without loading all into memory
for patient_batch in stream_fhir_resources("Patient", "active=true", batch_size=50):
    process_batch(patient_batch)
```

---

### FEATURE 3: CDS HOOKS INTEGRATION

**What:** Clinical Decision Support hooks triggered by context

**Available (Commented Out):** `ToolFactory.java:202-249`

**Uncomment to Enable:**

```java
public static Tool callCdsHook() throws JsonProcessingException {
    return new Tool.Builder()
        .name("call-cds-hook")
        .description("Call a CDS Hook for clinical decision support")
        .inputSchema(mapper.readValue(CALL_CDS_HOOK_SCHEMA_2_0_1, McpSchema.JsonSchema.class))
        .build();
}
```

**Usage Example:**

```json
{
  "tool": "call-cds-hook",
  "arguments": {
    "service": "medication-prescribe",
    "hook": "medication-prescribe",
    "hookInstance": "uuid-123",
    "hookContext": {
      "userId": "Practitioner/Dr-Schmidt",
      "patientId": "Patient/789",
      "medications": [
        {"resourceType": "MedicationRequest", "medicationCodeableConcept": {...}}
      ]
    }
  }
}
```

**Response:**

```json
{
  "cards": [
    {
      "summary": "Drug-Drug Interaction Detected",
      "indicator": "warning",
      "detail": "Warfarin + Aspirin: Increased bleeding risk",
      "source": {"label": "Clinical Decision Support"},
      "suggestions": [
        {"label": "Consider alternative to Aspirin"}
      ]
    }
  ]
}
```

---

### FEATURE 4: CUSTOM CLINICAL WORKFLOWS

**Example:** Diabetic Retinopathy Screening Workflow

```java
@Component
public class DiabeticWorkflows {

    @McpTool(
        name = "check-diabetic-screening-due",
        description = "Check if diabetic patient is due for retinopathy screening"
    )
    public McpToolResult checkScreeningDue(
        @McpArgument(name = "patientId") String patientId
    ) {
        // 1. Verify patient has diabetes diagnosis
        // 2. Check last eye exam date
        // 3. Calculate if >1 year since last exam
        // 4. Return recommendation

        return McpToolResult.success(
            "Patient due for retinopathy screening. Last exam: 2024-05-15 (18 months ago)"
        );
    }
}
```

---

## PERFORMANCE TUNING

### OPTIMIZATION 1: ENABLE RESPONSE CACHING

**File:** `application.yaml`

```yaml
spring:
  ai:
    mcp:
      server:
        cache:
          enabled: true
          max-size: 1000
          ttl: 300  # 5 minutes
```

**Impact:** Repeated queries return cached results instantly

---

### OPTIMIZATION 2: CONNECTION POOLING

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # Increase for MCP load
      minimum-idle: 10
```

---

### OPTIMIZATION 3: SEARCH RESULT PAGINATION

**Modify Search Tool:**

```java
// Limit default search results
private static final String SEARCH_FHIR_RESOURCES_SCHEMA = """
{
  "properties": {
    "resourceType": {"type": "string"},
    "query": {"type": "string"},
    "count": {
      "type": "integer",
      "description": "Max results to return",
      "default": 20,
      "maximum": 100
    }
  }
}
""";
```

---

### OPTIMIZATION 4: ASYNC TOOL EXECUTION

```java
@Async
public CompletableFuture<McpToolResult> executeToolAsync(
    String toolName,
    Map<String, Object> arguments
) {
    return CompletableFuture.supplyAsync(() ->
        executeTool(toolName, arguments)
    );
}
```

---

## TROUBLESHOOTING

### ISSUE 1: MCP ENDPOINT NOT RESPONDING

**Symptoms:**
```
curl: (7) Failed to connect to localhost:8080
```

**Solutions:**

```bash
# Check if HAPI FHIR is running
lsof -i :8080

# Check logs
tail -f logs/hapi-fhir.log

# Verify MCP is enabled
grep "mcp.server.enabled" src/main/resources/application.yaml

# Restart server
./stop-server.sh && ./start-server.sh
```

---

### ISSUE 2: TOOLS NOT APPEARING

**Symptoms:**
```json
{
  "tools": []
}
```

**Solutions:**

```bash
# Check startup logs for errors
grep "McpFhirBridge" logs/hapi-fhir.log

# Verify Spring Bean registration
grep "@Component" src/main/java/ca/uhn/fhir/rest/server/McpFhirBridge.java

# Check tool factory
grep "public static Tool" src/main/java/ca/uhn/fhir/jpa/starter/mcp/ToolFactory.java
```

---

### ISSUE 3: AUTHENTICATION ERRORS

**Symptoms:**
```json
{
  "error": "Invalid MCP authentication token"
}
```

**Solutions:**

```bash
# Verify token in request
curl -H "Authorization: Bearer ddx-api-token-2024" \
     -H "X-Clinic-ID: ddx-hamburg-clinic" \
     http://localhost:8080/mcp/message

# Check interceptor is registered
grep "McpAuthInterceptor" src/main/java/.../StarterJpaConfig.java

# Verify token constant
grep "VALID_MCP_TOKEN" src/main/java/.../McpAuthInterceptor.java
```

---

### ISSUE 4: PARTITION ERRORS

**Symptoms:**
```
HAPI-1220: This server is not configured to support search against all partitions
```

**Solutions:**

```yaml
# application.yaml
hapi:
  fhir:
    partitioning:
      allow_references_across_partitions: true
      partitioning_include_in_search_hashes: true
```

**Verify Partitions Exist:**

```bash
psql -U dudoxx_user -d ddx_hapifhir -c "SELECT * FROM HFJ_PARTITION;"
```

**Expected Output:**
```
 part_id | part_name  | part_desc
---------+------------+--------------------
       0 | DEFAULT    | System partition
       1 | HAMBURG    | Hamburg Clinic
       2 | BERLIN     | Berlin Clinic
       ...
```

---

### ISSUE 5: TOOL EXECUTION FAILURES

**Symptoms:**
```json
{
  "error": "Unexpected error: NullPointerException"
}
```

**Debug Steps:**

```bash
# Enable debug logging
# File: src/main/resources/logback.xml
<logger name="ca.uhn.fhir.jpa.starter.mcp" level="DEBUG"/>

# Check tool arguments
# Add validation in RequestBuilder.java
if (arguments.get("resourceType") == null) {
    throw new IllegalArgumentException("Missing resourceType");
}

# Test tool directly
curl -X POST http://localhost:8080/fhir/Patient?name=Schmidt
```

---

## WHEN TO DISABLE MCP

### DISABLE IF:

1. **Not Using AI Features**
   - No LLM integrations planned
   - Traditional REST API sufficient

2. **Security Concerns**
   - Cannot implement proper authentication
   - Compliance forbids AI access to PHI
   - Network security insufficient

3. **Performance Constraints**
   - Server resources limited
   - High load from existing FHIR API
   - Cannot handle additional overhead

4. **Compliance Requirements**
   - HIPAA/GDPR forbids AI processing
   - Data residency restrictions
   - Audit trail insufficient

### HOW TO DISABLE:

**File:** `application.yaml`

```yaml
spring:
  ai:
    mcp:
      server:
        enabled: false
```

**Or Remove MCP Dependencies:**

**File:** `pom.xml`

```xml
<!-- Comment out MCP dependencies -->
<!--
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-server</artifactId>
</dependency>
-->
```

**Rebuild:**

```bash
mvn clean package -DskipTests
./stop-server.sh && ./start-server.sh
```

**Verify Disabled:**

```bash
curl http://localhost:8080/mcp/message
# Should return 404
```

---

## REFERENCES

### OFFICIAL DOCUMENTATION

1. **Model Context Protocol (MCP)**
   - Official Site: https://modelcontextprotocol.io/
   - Specification: https://spec.modelcontextprotocol.io/

2. **Spring AI MCP Integration**
   - Introduction Blog: https://spring.io/blog/2025/09/16/spring-ai-mcp-intro-blog/
   - Release Notes (1.1.0-M2): https://spring.io/blog/2025/09/19/spring-ai-1-1-0-M2-mcp-focused/
   - Reference Docs: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html
   - Getting Started: https://docs.spring.io/spring-ai/reference/guides/getting-started-mcp.html
   - GitHub SDK: https://github.com/spring-projects-experimental/spring-ai-mcp

3. **HAPI FHIR**
   - Official Site: https://hapifhir.io/
   - JPA Server Starter: https://github.com/hapifhir/hapi-fhir-jpaserver-starter
   - Documentation: https://hapifhir.io/hapi-fhir/docs/

### TUTORIALS & GUIDES

4. **MCP Architecture & Concepts**
   - Complete Tutorial: https://medium.com/@nimritakoul01/the-model-context-protocol-mcp-a-complete-tutorial-a3abe8a7f4ef
   - Overview: https://www.philschmid.de/mcp-introduction
   - How It Works: https://www.descope.com/learn/post/mcp
   - Deep Dive: https://www.getknit.dev/blog/mcp-architecture-deep-dive-tools-resources-and-prompts-explained
   - Full Blueprint: https://www.dailydoseofds.com/model-context-protocol-crash-course-part-4/
   - Features Guide: https://workos.com/blog/mcp-features-guide

5. **Spring AI Implementation**
   - Using MCP with Spring AI: https://piotrminkowski.com/2025/03/17/using-model-context-protocol-mcp-with-spring-ai/
   - Creating MCP Server: https://mydeveloperplanet.com/2025/11/05/creating-an-mcp-server-with-spring-ai/
   - Baeldung Tutorial: https://www.baeldung.com/spring-ai-model-context-protocol-mcp

### HEALTHCARE-SPECIFIC

6. **FHIR + MCP Integration**
   - FHIR MCP Server (Python): https://pypi.org/project/fhir-mcp-server/
   - Clinical Decision Support Paper: https://arxiv.org/html/2506.13800v1
   - Natural Language Interface: https://www.themomentum.ai/blog/introducing-fhir-mcp-server-natural-language-interface-for-healthcare-data
   - Use Cases for Developers: https://dev.to/momentumai/fhir-mcp-server-use-cases-for-healthcare-developers-4c5i

7. **Healthcare AI Applications**
   - MCP in Healthcare AI: https://ideausher.com/blog/mcp-in-healthcare-ai/
   - Universal Port for Healthcare: https://www.cabotsolutions.com/blog/the-model-context-protocol-a-universal-port-for-ai-in-healthcare
   - FHIR & GenAI: https://medium.com/@harish.vadada/bridging-the-gap-how-fhir-and-the-model-context-protocol-mcp-power-generative-ai-in-healthcare-6e894ddae6b7
   - Healthcare MCP (HMCP): https://innovaccer.com/blogs/introducing-hmcp-a-universal-open-standard-for-ai-in-healthcare

### CODE REPOSITORIES

8. **GitHub Examples**
   - Spring AI MCP SDK: https://github.com/spring-projects-experimental/spring-ai-mcp
   - HAPI FHIR Starter: https://github.com/hapifhir/hapi-fhir-jpaserver-starter

### STANDARDS & SPECIFICATIONS

9. **FHIR R4 Specification**
   - HL7 FHIR R4: https://hl7.org/fhir/R4/
   - Search Parameters: https://hl7.org/fhir/R4/search.html
   - REST API: https://hl7.org/fhir/R4/http.html

10. **MCP Protocol**
    - JSON-RPC 2.0: https://www.jsonrpc.org/specification
    - Server-Sent Events: https://html.spec.whatwg.org/multipage/server-sent-events.html

---

## APPENDIX: QUICK REFERENCE

### MCP ENDPOINT

```
URL:      http://localhost:8080/mcp/message
Protocol: HTTP Streamable (SSE)
Auth:     Bearer ddx-api-token-2024
Header:   X-Clinic-ID: ddx-hamburg-clinic
```

### 9 TOOLS SUMMARY

| # | Tool | Action | Example |
|---|------|--------|---------|
| 1 | `create-fhir-resource` | POST | Create patient |
| 2 | `read-fhir-resource` | GET | Get patient/123 |
| 3 | `update-fhir-resource` | PUT | Replace patient |
| 4 | `conditional-update-fhir-resource` | PUT + Query | Update where MRN=X |
| 5 | `delete-fhir-resource` | DELETE | Remove resource |
| 6 | `patch-fhir-resource` | PATCH | Update phone only |
| 7 | `conditional-patch-fhir-resource` | PATCH + Query | Patch where name=X |
| 8 | `search-fhir-resources` | GET + Query | Find diabetics |
| 9 | `create-fhir-transaction` | POST Bundle | Batch create |

### COMMON QUERIES

```bash
# List tools
curl -X POST http://localhost:8080/mcp/message \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list"}'

# Search patients
curl -X POST http://localhost:8080/mcp/message \
  -H "Authorization: Bearer ddx-api-token-2024" \
  -H "X-Clinic-ID: ddx-hamburg-clinic" \
  -d '{
    "jsonrpc":"2.0",
    "method":"tools/call",
    "params":{
      "name":"search-fhir-resources",
      "arguments":{"resourceType":"Patient","query":"name=Schmidt"}
    }
  }'
```

### FILE LOCATIONS

| Component | File Path |
|-----------|-----------|
| MCP Bridge | `src/main/java/ca/uhn/fhir/rest/server/McpFhirBridge.java` |
| Tool Factory | `src/main/java/ca/uhn/fhir/jpa/starter/mcp/ToolFactory.java` |
| Request Builder | `src/main/java/ca/uhn/fhir/jpa/starter/mcp/RequestBuilder.java` |
| MCP Config | `src/main/java/ca/uhn/fhir/jpa/starter/mcp/McpServerConfig.java` |
| Partition Interceptor | `src/main/java/ca/uhn/fhir/jpa/starter/interceptor/ClinicPartitionInterceptor.java` |

---

**END OF DOCUMENT**

**Version:** 1.0.0
**Last Updated:** November 25, 2025
**Maintained By:** Dudoxx UG
**Contact:** Walid Boudabbous (CTO)

---

```
╔═══════════════════════════════════════════════════════════════════╗
║  MCP enables AI to interact with clinical data using natural      ║
║  language. Use responsibly with proper authentication and         ║
║  audit logging. Always validate AI-generated clinical decisions.  ║
╚═══════════════════════════════════════════════════════════════════╝
```
