package ca.uhn.fhir.jpa.starter.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Tenant Administration
 *
 * Provides endpoints for NestJS to manage the HAPI FHIR tenant cache.
 * This enables dynamic tenant creation without requiring server restarts.
 *
 * Endpoints:
 * - POST /admin/tenants/refresh - Force refresh the entire tenant cache
 * - POST /admin/tenants/register - Register a new tenant immediately
 * - GET /admin/tenants - List all cached tenants
 * - GET /admin/tenants/{slug} - Check if a tenant exists in cache
 *
 * Authentication: Uses the same FHIR API token as the main FHIR endpoints.
 */
@RestController
@RequestMapping("/admin/tenants")
public class TenantAdminController {

    private static final Logger log = LoggerFactory.getLogger(TenantAdminController.class);

    @Autowired
    private TenantRegistryService tenantRegistry;

    @Value("${hapi.fhir.auth.api_token:ddx-api-token-2024}")
    private String apiToken;

    @Value("${hapi.fhir.auth.enabled:true}")
    private boolean authEnabled;

    /**
     * Validate the API token from the request
     */
    private boolean validateToken(String authHeader) {
        if (!authEnabled) {
            return true;
        }
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authHeader.substring(7);
        return apiToken.equals(token);
    }

    /**
     * Force refresh the entire tenant cache from the database.
     * Call this after creating/modifying tenants in NestJS.
     *
     * POST /admin/tenants/refresh
     * Authorization: Bearer <api-token>
     *
     * @return Success status with cache statistics
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshCache(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (!validateToken(authHeader)) {
            log.warn("Unauthorized cache refresh attempt");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or missing API token"));
        }

        log.info("Received cache refresh request from NestJS");

        try {
            // Get count before refresh
            int beforeCount = tenantRegistry.getAllClinics().size();

            // Force refresh from database
            tenantRegistry.forceRefresh();

            // Get count after refresh
            Map<String, Integer> clinics = tenantRegistry.getAllClinics();
            int afterCount = clinics.size();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tenant cache refreshed successfully");
            response.put("tenantsBefore", beforeCount);
            response.put("tenantsAfter", afterCount);
            response.put("tenantsAdded", afterCount - beforeCount);
            response.put("clinics", clinics);

            log.info("Cache refreshed: {} tenants before, {} tenants after", beforeCount, afterCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to refresh tenant cache", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to refresh cache: " + e.getMessage()
                    ));
        }
    }

    /**
     * Register a new tenant directly into the cache.
     * This is faster than a full refresh for single tenant creation.
     *
     * POST /admin/tenants/register
     * Authorization: Bearer <api-token>
     * Content-Type: application/json
     * Body: { "slug": "ddx-new-clinic", "partitionId": 12 }
     *
     * @param payload The tenant registration details
     * @return Success status
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerTenant(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> payload) {

        if (!validateToken(authHeader)) {
            log.warn("Unauthorized tenant registration attempt");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or missing API token"));
        }

        String slug = (String) payload.get("slug");
        Integer partitionId = payload.get("partitionId") instanceof Number
                ? ((Number) payload.get("partitionId")).intValue()
                : null;

        if (slug == null || slug.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing required field: slug"));
        }

        if (partitionId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing required field: partitionId"));
        }

        log.info("Registering tenant: {} -> Partition {}", slug, partitionId);

        try {
            tenantRegistry.registerTenant(slug, partitionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tenant registered successfully");
            response.put("slug", slug);
            response.put("partitionId", partitionId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to register tenant: {}", slug, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to register tenant: " + e.getMessage()
                    ));
        }
    }

    /**
     * Remove a tenant from the cache (for deactivation).
     *
     * DELETE /admin/tenants/{slug}
     * Authorization: Bearer <api-token>
     *
     * @param slug The tenant slug to remove
     * @return Success status
     */
    @DeleteMapping("/{slug}")
    public ResponseEntity<Map<String, Object>> removeTenant(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String slug) {

        if (!validateToken(authHeader)) {
            log.warn("Unauthorized tenant removal attempt");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or missing API token"));
        }

        log.info("Removing tenant from cache: {}", slug);

        try {
            tenantRegistry.removeTenant(slug);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tenant removed from cache");
            response.put("slug", slug);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to remove tenant: {}", slug, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to remove tenant: " + e.getMessage()
                    ));
        }
    }

    /**
     * List all tenants currently in the cache.
     *
     * GET /admin/tenants
     * Authorization: Bearer <api-token>
     *
     * @return Map of all cached tenants (slug -> partitionId)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listTenants(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (!validateToken(authHeader)) {
            log.warn("Unauthorized tenant list attempt");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or missing API token"));
        }

        Map<String, Integer> clinics = tenantRegistry.getAllClinics();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", clinics.size());
        response.put("tenants", clinics);

        return ResponseEntity.ok(response);
    }

    /**
     * Check if a specific tenant exists in the cache.
     *
     * GET /admin/tenants/{slug}
     * Authorization: Bearer <api-token>
     *
     * @param slug The tenant slug to check
     * @return Tenant details if found
     */
    @GetMapping("/{slug}")
    public ResponseEntity<Map<String, Object>> getTenant(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String slug) {

        if (!validateToken(authHeader)) {
            log.warn("Unauthorized tenant lookup attempt");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or missing API token"));
        }

        Integer partitionId = tenantRegistry.getPartitionId(slug);

        if (partitionId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "success", false,
                            "error", "Tenant not found in cache",
                            "slug", slug
                    ));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("slug", slug);
        response.put("partitionId", partitionId);
        response.put("exists", true);

        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint for the tenant admin API.
     *
     * GET /admin/tenants/health
     * No authentication required
     *
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "TenantAdminController");
        response.put("cachedTenants", tenantRegistry.getAllClinics().size());

        return ResponseEntity.ok(response);
    }
}
