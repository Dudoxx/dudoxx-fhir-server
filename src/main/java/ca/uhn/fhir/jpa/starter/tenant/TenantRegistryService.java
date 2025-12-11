package ca.uhn.fhir.jpa.starter.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic Tenant Registry Service
 *
 * Manages the mapping between clinic slugs and HAPI FHIR partition IDs.
 * Reads from the NestJS PostgreSQL database (ddx_clinic_fhir) and caches locally.
 *
 * Database tables used (from NestJS ddx_clinic_fhir):
 * - organizations: Contains slug and fhirPartitionId
 * - global_config: Contains system settings like fhir.api_token
 * - partition_sequence: Contains next available partition ID
 */
@Service
public class TenantRegistryService {

    private static final Logger log = LoggerFactory.getLogger(TenantRegistryService.class);

    // Cache for clinic slug -> partition ID mapping
    private final Map<String, Integer> clinicPartitionCache = new ConcurrentHashMap<>();

    // Cache for global config values
    private final Map<String, String> globalConfigCache = new ConcurrentHashMap<>();

    @Autowired
    @Qualifier("tenantJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Value("${hapi.fhir.auth.api_token:ddx-api-token-2024}")
    private String defaultApiToken;

    @Value("${hapi.fhir.tenant.cache_refresh_seconds:60}")
    private int cacheRefreshSeconds;

    /**
     * Initialize cache on startup
     */
    @PostConstruct
    public void init() {
        log.info("Initializing Tenant Registry Service...");
        refreshCache();
        log.info("Tenant Registry initialized with {} clinics", clinicPartitionCache.size());
    }

    /**
     * Refresh cache periodically (default: every 60 seconds)
     */
    @Scheduled(fixedDelayString = "${hapi.fhir.tenant.cache_refresh_seconds:60}000")
    public void refreshCache() {
        try {
            refreshClinicPartitionCache();
            refreshGlobalConfigCache();
        } catch (Exception e) {
            log.error("Failed to refresh tenant cache: {}", e.getMessage());
        }
    }

    /**
     * Refresh clinic-to-partition mapping from database
     */
    private void refreshClinicPartitionCache() {
        try {
            String sql = "SELECT slug, \"fhirPartitionId\" FROM organizations WHERE slug IS NOT NULL AND \"fhirPartitionId\" IS NOT NULL AND \"isActive\" = true";

            Map<String, Integer> newCache = new ConcurrentHashMap<>();

            // Add default partition
            newCache.put("default", 0);

            jdbcTemplate.query(sql, rs -> {
                String slug = rs.getString("slug");
                int partitionId = rs.getInt("fhirPartitionId");
                newCache.put(slug.toLowerCase(), partitionId);
                log.debug("Loaded tenant: {} -> Partition {}", slug, partitionId);
            });

            clinicPartitionCache.clear();
            clinicPartitionCache.putAll(newCache);

            log.debug("Refreshed clinic partition cache with {} entries", clinicPartitionCache.size());
        } catch (Exception e) {
            log.error("Failed to refresh clinic partition cache: {}", e.getMessage());
        }
    }

    /**
     * Refresh global config from database
     */
    private void refreshGlobalConfigCache() {
        try {
            String sql = "SELECT key, value FROM global_config WHERE \"isActive\" = true";

            Map<String, String> newCache = new ConcurrentHashMap<>();

            jdbcTemplate.query(sql, rs -> {
                String key = rs.getString("key");
                String value = rs.getString("value");
                newCache.put(key, value);
                log.debug("Loaded config: {} = {}", key, key.contains("token") ? "***" : value);
            });

            globalConfigCache.clear();
            globalConfigCache.putAll(newCache);

            log.debug("Refreshed global config cache with {} entries", globalConfigCache.size());
        } catch (Exception e) {
            log.error("Failed to refresh global config cache: {}", e.getMessage());
        }
    }

    /**
     * Get partition ID for a clinic slug.
     *
     * IMPORTANT: If not found in cache, this method will try ONE database refresh
     * before returning null. This handles the case where a tenant was just created
     * in NestJS and the cache hasn't been refreshed yet.
     *
     * @param clinicSlug The clinic slug (e.g., "ddx-hamburg-clinic")
     * @return The partition ID, or null if not found
     */
    public Integer getPartitionId(String clinicSlug) {
        if (clinicSlug == null || clinicSlug.isEmpty()) {
            return null;
        }

        String normalizedSlug = clinicSlug.toLowerCase();

        // First try: check cache
        Integer partitionId = clinicPartitionCache.get(normalizedSlug);
        if (partitionId != null) {
            return partitionId;
        }

        // Cache miss - try ONE refresh from database (handles newly created tenants)
        log.info("Cache miss for tenant '{}' - attempting database refresh", clinicSlug);
        refreshCache();

        // Second try: check cache again after refresh
        partitionId = clinicPartitionCache.get(normalizedSlug);
        if (partitionId != null) {
            log.info("Found tenant '{}' after cache refresh -> Partition {}", clinicSlug, partitionId);
        } else {
            log.warn("Tenant '{}' not found even after cache refresh", clinicSlug);
        }

        return partitionId;
    }

    /**
     * Check if a clinic exists in the registry
     *
     * @param clinicSlug The clinic slug
     * @return true if the clinic exists
     */
    public boolean clinicExists(String clinicSlug) {
        return getPartitionId(clinicSlug) != null;
    }

    /**
     * Get all registered clinics
     *
     * @return Map of clinic slugs to partition IDs
     */
    public Map<String, Integer> getAllClinics() {
        return new ConcurrentHashMap<>(clinicPartitionCache);
    }

    /**
     * Get a global config value
     *
     * @param key The config key (e.g., "fhir.api_token")
     * @return The config value, or null if not found
     */
    public String getConfigValue(String key) {
        return globalConfigCache.get(key);
    }

    /**
     * Get a global config value with default fallback
     *
     * @param key The config key
     * @param defaultValue The default value if not found
     * @return The config value or default
     */
    public String getConfigValue(String key, String defaultValue) {
        return globalConfigCache.getOrDefault(key, defaultValue);
    }

    /**
     * Get the FHIR API token (from DB with env fallback)
     *
     * @return The API token
     */
    public String getFhirApiToken() {
        String dbToken = getConfigValue("fhir.api_token");
        return dbToken != null ? dbToken : defaultApiToken;
    }

    /**
     * Check if FHIR auth is enabled
     *
     * @return true if auth is enabled
     */
    public boolean isFhirAuthEnabled() {
        String enabled = getConfigValue("fhir.auth_enabled", "true");
        return Boolean.parseBoolean(enabled);
    }

    /**
     * Get the next available partition ID for new tenant creation
     * Atomically increments the sequence
     *
     * @return The next partition ID
     */
    public int getNextPartitionId() {
        try {
            String sql = "UPDATE partition_sequence SET \"currentValue\" = \"currentValue\" + 1, \"updatedAt\" = NOW() RETURNING \"currentValue\"";
            Integer nextId = jdbcTemplate.queryForObject(sql, Integer.class);
            return nextId != null ? nextId : 7;
        } catch (Exception e) {
            log.error("Failed to get next partition ID: {}", e.getMessage());
            // Fallback: find max partition ID and add 1
            Integer maxId = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(\"fhirPartitionId\"), 6) FROM organizations",
                Integer.class
            );
            return (maxId != null ? maxId : 6) + 1;
        }
    }

    /**
     * Register a new tenant dynamically
     *
     * @param clinicSlug The clinic slug
     * @param partitionId The assigned partition ID
     */
    public void registerTenant(String clinicSlug, int partitionId) {
        clinicPartitionCache.put(clinicSlug.toLowerCase(), partitionId);
        log.info("Registered new tenant in cache: {} -> Partition {}", clinicSlug, partitionId);
    }

    /**
     * Remove a tenant from the cache (for deactivation)
     *
     * @param clinicSlug The clinic slug to remove
     */
    public void removeTenant(String clinicSlug) {
        clinicPartitionCache.remove(clinicSlug.toLowerCase());
        log.info("Removed tenant from cache: {}", clinicSlug);
    }

    /**
     * Force refresh the cache (e.g., after manual DB changes)
     */
    public void forceRefresh() {
        log.info("Force refreshing tenant registry cache...");
        refreshCache();
    }
}
