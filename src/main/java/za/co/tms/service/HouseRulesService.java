package za.co.tms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import za.co.tms.domain.HouseRules;
import za.co.tms.domain.HouseRulesAcknowledgement;
import za.co.tms.domain.Tenant;
import za.co.tms.repository.HouseRulesAcknowledgementRepository;
import za.co.tms.repository.HouseRulesRepository;
import za.co.tms.repository.TenantRepository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HouseRulesService {

    private final HouseRulesRepository houseRulesRepository;
    private final HouseRulesAcknowledgementRepository acknowledgementRepository;
    private final TenantRepository tenantRepository;

    /**
     * Upload a new house rules PDF. Deactivates any previously active version.
     */
    @Transactional
    public HouseRules uploadHouseRules(MultipartFile file, String description, String uploadedBy) throws IOException {
        // Determine next version number
        Integer nextVersion = houseRulesRepository.findTopByOrderByVersionDesc()
                .map(hr -> hr.getVersion() + 1)
                .orElse(1);

        // Deactivate all existing active versions
        houseRulesRepository.findTopByActiveIsTrueOrderByVersionDesc()
                .ifPresent(existing -> {
                    existing.setActive(false);
                    houseRulesRepository.save(existing);
                });

        // Create new house rules record
        HouseRules houseRules = new HouseRules();
        houseRules.setVersion(nextVersion);
        houseRules.setFileName(file.getOriginalFilename());
        houseRules.setFileData(file.getBytes());
        houseRules.setContentType(file.getContentType());
        houseRules.setUploadedBy(uploadedBy);
        houseRules.setDescription(description);
        houseRules.setActive(true);

        HouseRules saved = houseRulesRepository.save(houseRules);
        log.info("House Rules v{} uploaded by {}: {}", nextVersion, uploadedBy, file.getOriginalFilename());
        return saved;
    }

    /**
     * Get the current active house rules version.
     */
    public Optional<HouseRules> getCurrentHouseRules() {
        return houseRulesRepository.findTopByActiveIsTrueOrderByVersionDesc();
    }

    /**
     * Get all house rules versions (history).
     */
    public List<HouseRules> getAllVersions() {
        return houseRulesRepository.findAllByOrderByVersionDesc();
    }

    /**
     * Get a specific house rules version by ID.
     */
    public Optional<HouseRules> getById(Long id) {
        return houseRulesRepository.findById(id);
    }

    /**
     * Acknowledge the current house rules for a tenant.
     */
    @Transactional
    public HouseRulesAcknowledgement acknowledge(Integer tenantId, String ipAddress) {
        HouseRules current = getCurrentHouseRules()
                .orElseThrow(() -> new RuntimeException("No active house rules found"));

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found with ID: " + tenantId));

        // Check if already acknowledged
        if (acknowledgementRepository.existsByTenantIdAndHouseRulesId(tenantId, current.getId())) {
            throw new RuntimeException("Tenant has already acknowledged this version");
        }

        HouseRulesAcknowledgement acknowledgement = new HouseRulesAcknowledgement();
        acknowledgement.setTenant(tenant);
        acknowledgement.setHouseRules(current);
        acknowledgement.setIpAddress(ipAddress);

        HouseRulesAcknowledgement saved = acknowledgementRepository.save(acknowledgement);
        log.info("Tenant {} {} acknowledged House Rules v{}", tenant.getName(), tenant.getSurname(), current.getVersion());
        return saved;
    }

    /**
     * Check if a tenant has acknowledged the current active house rules.
     */
    public boolean hasAcknowledgedCurrent(Integer tenantId) {
        return getCurrentHouseRules()
                .map(hr -> acknowledgementRepository.existsByTenantIdAndHouseRulesId(tenantId, hr.getId()))
                .orElse(true); // If no house rules exist, consider it acknowledged
    }

    /**
     * Get all acknowledgements for a specific house rules version.
     */
    public List<HouseRulesAcknowledgement> getAcknowledgementsForVersion(Long houseRulesId) {
        return acknowledgementRepository.findByHouseRulesId(houseRulesId);
    }

    /**
     * Get all acknowledgements for a specific tenant.
     */
    public List<HouseRulesAcknowledgement> getAcknowledgementsForTenant(Integer tenantId) {
        return acknowledgementRepository.findByTenantId(tenantId);
    }
}
