package za.co.tms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.tms.domain.*;
import za.co.tms.dto.RevenueDTO;
import za.co.tms.repository.PaymentRepository;
import za.co.tms.repository.RoomRepository;
import za.co.tms.repository.TenantRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RevenueService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RevenueService.class);

    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;

    @Autowired
    public RevenueService(PaymentRepository paymentRepository,
                          RoomRepository roomRepository,
                          TenantRepository tenantRepository) {
        this.paymentRepository = paymentRepository;
        this.roomRepository = roomRepository;
        this.tenantRepository = tenantRepository;
    }

    public RevenueDTO getRevenueData() {
        LOGGER.info("Generating revenue data");

        RevenueDTO dto = new RevenueDTO();
        List<Payment> allPayments = paymentRepository.findAll();
        List<Room> allRooms = roomRepository.findAll();

        YearMonth currentMonth = YearMonth.now();
        YearMonth lastMonth = currentMonth.minusMonths(1);

        // Monthly target = sum of all occupied rooms' rental amounts
        BigDecimal monthlyTarget = allRooms.stream()
                .filter(Room::isOccupied)
                .map(Room::getRentalAmount)
                .filter(r -> r != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setMonthlyTarget(monthlyTarget);

        // This month's income
        BigDecimal thisMonthIncome = getCollectedForMonth(allPayments, currentMonth);
        dto.setThisMonthIncome(thisMonthIncome);

        // Last month's income
        BigDecimal lastMonthIncome = getCollectedForMonth(allPayments, lastMonth);
        dto.setLastMonthIncome(lastMonthIncome);

        // Year-to-date income
        LocalDateTime yearStart = currentMonth.atDay(1).withMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        BigDecimal ytdIncome = allPayments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PAID)
                .filter(p -> p.getPaymentDate() != null
                        && !p.getPaymentDate().isBefore(yearStart)
                        && !p.getPaymentDate().isAfter(now))
                .map(Payment::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setYearToDateIncome(ytdIncome);

        // Collection rate this month
        double collectionRate = monthlyTarget.compareTo(BigDecimal.ZERO) > 0
                ? thisMonthIncome.divide(monthlyTarget, 4, RoundingMode.HALF_UP).doubleValue() * 100
                : 0;
        dto.setCollectionRatePercent(collectionRate);

        // Monthly income for last 12 months
        List<RevenueDTO.MonthlyData> monthlyIncome = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            BigDecimal collected = getCollectedForMonth(allPayments, month);
            double rate = monthlyTarget.compareTo(BigDecimal.ZERO) > 0
                    ? collected.divide(monthlyTarget, 4, RoundingMode.HALF_UP).doubleValue() * 100
                    : 0;

            monthlyIncome.add(new RevenueDTO.MonthlyData(
                    month.getMonth().name().substring(0, 3),
                    month.getYear(),
                    collected,
                    monthlyTarget,
                    rate
            ));
        }
        dto.setMonthlyIncome(monthlyIncome);

        // Revenue by room
        List<Tenant> activeTenants = tenantRepository.findAll().stream()
                .filter(t -> t.getTenantStatus() == TenantStatus.ACTIVE)
                .collect(Collectors.toList());

        Map<Integer, BigDecimal> tenantPayments = allPayments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PAID)
                .filter(p -> p.getTenant() != null && p.getTenant().getId() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getTenant().getId(),
                        Collectors.reducing(BigDecimal.ZERO, p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO, BigDecimal::add)
                ));

        List<RevenueDTO.RoomRevenue> revenueByRoom = new ArrayList<>();
        for (Room room : allRooms) {
            BigDecimal totalCollected = activeTenants.stream()
                    .filter(t -> t.getRoom() != null && t.getRoom().getId().equals(room.getId()))
                    .map(t -> tenantPayments.getOrDefault(t.getId(), BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            revenueByRoom.add(new RevenueDTO.RoomRevenue(
                    room.getCode(),
                    room.getDescription(),
                    room.getRentalAmount(),
                    totalCollected,
                    room.isOccupied()
            ));
        }
        // Sort by total collected descending
        revenueByRoom.sort((a, b) -> b.getTotalCollected().compareTo(a.getTotalCollected()));
        dto.setRevenueByRoom(revenueByRoom);

        return dto;
    }

    private BigDecimal getCollectedForMonth(List<Payment> payments, YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);

        return payments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PAID)
                .filter(p -> p.getPaymentDate() != null
                        && !p.getPaymentDate().isBefore(start)
                        && !p.getPaymentDate().isAfter(end))
                .map(Payment::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
