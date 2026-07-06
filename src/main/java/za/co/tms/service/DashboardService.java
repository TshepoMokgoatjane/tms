package za.co.tms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.tms.domain.*;
import za.co.tms.dto.DashboardDTO;
import za.co.tms.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    private final TenantRepository tenantRepository;
    private final RoomRepository roomRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;

    @Autowired
    public DashboardService(TenantRepository tenantRepository,
                            RoomRepository roomRepository,
                            PaymentRepository paymentRepository,
                            TicketRepository ticketRepository) {
        this.tenantRepository = tenantRepository;
        this.roomRepository = roomRepository;
        this.paymentRepository = paymentRepository;
        this.ticketRepository = ticketRepository;
    }

    public DashboardDTO getDashboardData() {
        DashboardDTO dto = new DashboardDTO();

        // Tenant stats
        List<Tenant> allTenants = tenantRepository.findAll();
        int activeTenants = (int) allTenants.stream()
                .filter(t -> t.getTenantStatus() == TenantStatus.ACTIVE)
                .count();
        int inactiveTenants = allTenants.size() - activeTenants;

        dto.setTotalTenants(allTenants.size());
        dto.setActiveTenants(activeTenants);
        dto.setInactiveTenants(inactiveTenants);

        // Room stats
        List<Room> allRooms = roomRepository.findAll();
        int occupiedRooms = (int) allRooms.stream().filter(Room::isOccupied).count();
        int availableRooms = allRooms.size() - occupiedRooms;
        int occupancyRate = allRooms.isEmpty() ? 0 : Math.round((float) occupiedRooms / allRooms.size() * 100);

        dto.setTotalRooms(allRooms.size());
        dto.setOccupiedRooms(occupiedRooms);
        dto.setAvailableRooms(availableRooms);
        dto.setOccupancyRatePercent(occupancyRate);

        // Monthly revenue target = sum of all occupied rooms' rental amounts
        BigDecimal monthlyTarget = allRooms.stream()
                .filter(Room::isOccupied)
                .map(Room::getRentalAmount)
                .filter(r -> r != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setMonthlyRevenueTarget(monthlyTarget);

        // Payment stats for current month
        List<Payment> allPayments = paymentRepository.findAll();
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        BigDecimal collectedThisMonth = allPayments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PAID)
                .filter(p -> p.getPaymentDate() != null
                        && !p.getPaymentDate().isBefore(monthStart)
                        && !p.getPaymentDate().isAfter(monthEnd))
                .map(Payment::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setCollectedThisMonth(collectedThisMonth);

        // Payment status counts (all time)
        int paidCount = (int) allPayments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PAID).count();
        int failedCount = (int) allPayments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.FAILED).count();
        int pendingCount = allPayments.size() - paidCount - failedCount;

        dto.setPaidCount(paidCount);
        dto.setFailedCount(failedCount);
        dto.setPendingCount(pendingCount);

        // Ticket stats
        List<Ticket> allTickets = ticketRepository.findAll();
        int resolvedTickets = (int) allTickets.stream()
                .filter(t -> t.getStatus() == Status.COMPLETED || t.getStatus() == Status.CLOSED)
                .count();
        int openTickets = allTickets.size() - resolvedTickets;

        dto.setOpenTickets(openTickets);
        dto.setResolvedTickets(resolvedTickets);

        // Monthly revenue chart: last 6 months
        List<DashboardDTO.MonthlyRevenue> monthlyRevenue = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            LocalDateTime start = month.atDay(1).atStartOfDay();
            LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);

            BigDecimal collected = allPayments.stream()
                    .filter(p -> p.getPaymentStatus() == PaymentStatus.PAID)
                    .filter(p -> p.getPaymentDate() != null
                            && !p.getPaymentDate().isBefore(start)
                            && !p.getPaymentDate().isAfter(end))
                    .map(Payment::getAmount)
                    .filter(a -> a != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            monthlyRevenue.add(new DashboardDTO.MonthlyRevenue(
                    month.getMonth().name().substring(0, 3),
                    collected,
                    monthlyTarget
            ));
        }
        dto.setMonthlyRevenue(monthlyRevenue);

        return dto;
    }
}
