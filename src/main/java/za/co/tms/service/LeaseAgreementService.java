package za.co.tms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import za.co.tms.domain.LeaseAgreement;
import za.co.tms.domain.Tenant;
import za.co.tms.repository.LeaseAgreementRepository;
import za.co.tms.repository.TenantRepository;

import java.io.IOException;
import java.util.List;

@Service
public class LeaseAgreementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeaseAgreementService.class);

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String ALLOWED_CONTENT_TYPE = "application/pdf";

    private final LeaseAgreementRepository leaseAgreementRepository;
    private final TenantRepository tenantRepository;

    @Autowired
    public LeaseAgreementService(LeaseAgreementRepository leaseAgreementRepository, TenantRepository tenantRepository) {
        this.leaseAgreementRepository = leaseAgreementRepository;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Upload or replace a lease agreement PDF for a specific tenant.
     */
    @Transactional
    public LeaseAgreement uploadLeaseAgreement(Integer tenantId, MultipartFile file) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File must be 10MB or smaller.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPE.equals(contentType)) {
            throw new IllegalArgumentException("Only PDF files are allowed.");
        }

        // Validate tenant exists
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant with ID " + tenantId + " not found."));

        // Check if a lease already exists for this tenant — replace it
        LeaseAgreement leaseAgreement = leaseAgreementRepository.findByTenantId(tenantId)
                .orElse(new LeaseAgreement());

        leaseAgreement.setTenant(tenant);
        leaseAgreement.setOriginalFilename(file.getOriginalFilename());
        leaseAgreement.setContentType(contentType);
        leaseAgreement.setFileSize(file.getSize());
        leaseAgreement.setPdfData(file.getBytes());

        LeaseAgreement saved = leaseAgreementRepository.save(leaseAgreement);
        LOGGER.info("Lease agreement uploaded for tenant ID {}: {} ({} bytes)",
                tenantId, file.getOriginalFilename(), file.getSize());

        return saved;
    }

    /**
     * Download (retrieve) the lease agreement for a tenant.
     */
    public LeaseAgreement getLeaseAgreementByTenantId(Integer tenantId) {
        return leaseAgreementRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("No lease agreement found for tenant ID " + tenantId));
    }

    /**
     * Check if a lease agreement exists for a tenant.
     */
    public boolean existsForTenant(Integer tenantId) {
        return leaseAgreementRepository.existsByTenantId(tenantId);
    }

    /**
     * Retrieve all lease agreements (without PDF data for listing).
     */
    public List<LeaseAgreement> getAllLeaseAgreements() {
        LOGGER.info("Retrieving all lease agreements");
        return leaseAgreementRepository.findAll();
    }

    /**
     * Delete a lease agreement for a specific tenant.
     */
    @Transactional
    public void deleteLeaseAgreement(Integer tenantId) {
        LeaseAgreement leaseAgreement = leaseAgreementRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("No lease agreement found for tenant ID " + tenantId));

        leaseAgreementRepository.delete(leaseAgreement);
        LOGGER.info("Lease agreement deleted for tenant ID {}", tenantId);
    }
}
