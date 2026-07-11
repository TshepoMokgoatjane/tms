package za.co.tms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.tms.domain.*;
import za.co.tms.dto.SalesPipelineDTO;
import za.co.tms.repository.ContactUsRepository;
import za.co.tms.repository.RoomRepository;
import za.co.tms.repository.TenantRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SalesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SalesService.class);

    private final ContactUsRepository contactUsRepository;
    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;

    @Autowired
    public SalesService(ContactUsRepository contactUsRepository,
                        RoomRepository roomRepository,
                        TenantRepository tenantRepository) {
        this.contactUsRepository = contactUsRepository;
        this.roomRepository = roomRepository;
        this.tenantRepository = tenantRepository;
    }

    public SalesPipelineDTO getPipelineStats() {
        LOGGER.info("Generating sales pipeline stats");

        long total = contactUsRepository.count();
        long newLeads = contactUsRepository.countByLeadStatus(LeadStatus.NEW);
        long contacted = contactUsRepository.countByLeadStatus(LeadStatus.CONTACTED);
        long viewingScheduled = contactUsRepository.countByLeadStatus(LeadStatus.VIEWING_SCHEDULED);
        long leaseSigned = contactUsRepository.countByLeadStatus(LeadStatus.LEASE_SIGNED);
        long lost = contactUsRepository.countByLeadStatus(LeadStatus.LOST);
        long vacantRooms = roomRepository.findByOccupiedFalse().size();

        double conversionRate = total > 0 ? (double) leaseSigned / total * 100 : 0;

        return new SalesPipelineDTO(total, newLeads, contacted, viewingScheduled, leaseSigned, lost, vacantRooms, conversionRate);
    }

    public List<ContactUs> getAllInquiries() {
        return contactUsRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<ContactUs> getInquiriesByStatus(LeadStatus status) {
        return contactUsRepository.findByLeadStatus(status);
    }

    public ContactUs updateLeadStatus(Integer id, LeadStatus newStatus) {
        LOGGER.info("Updating lead status for inquiry {} to {}", id, newStatus);
        ContactUs contactUs = contactUsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inquiry with ID " + id + " not found"));
        contactUs.setLeadStatus(newStatus);
        return contactUsRepository.save(contactUs);
    }

    public List<Room> getVacantRooms() {
        return roomRepository.findByOccupiedFalse();
    }

    public List<Tenant> getRecentConversions() {
        LocalDate ninetyDaysAgo = LocalDate.now().minusDays(90);
        return tenantRepository.findAll().stream()
                .filter(t -> t.getTenantStatus() == TenantStatus.ACTIVE)
                .filter(t -> t.getLeaseStartDate() != null && t.getLeaseStartDate().isAfter(ninetyDaysAgo))
                .collect(Collectors.toList());
    }
}
