package za.co.tms.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.co.tms.domain.Status;
import za.co.tms.domain.Ticket;
import za.co.tms.repository.TicketCommentRepository;
import za.co.tms.repository.TicketImageRepository;
import za.co.tms.repository.TicketRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Space-saving retention policy for maintenance tickets.
 *
 * <p>Once a day, hard-deletes tickets that have been in a terminal state (CLOSED or COMPLETED)
 * for longer than the configured retention window ({@code tickets.retention-months}, default 3),
 * along with their attached photos (the real storage consumer) and comment history.
 *
 * <p>OPEN and WORK_IN_PROGRESS tickets are never touched, regardless of age. This is a
 * destructive, irreversible operation, so it is:
 * <ul>
 *   <li>scoped strictly to terminal tickets past the window,</li>
 *   <li>gated behind {@code tickets.retention-enabled} (default true) so it can be switched off,</li>
 *   <li>and logs what it removes.</li>
 * </ul>
 */
@Slf4j
@Component
public class TicketRetentionScheduler {

    private static final List<Status> TERMINAL_STATUSES = List.of(Status.CLOSED, Status.COMPLETED);

    private final TicketRepository ticketRepository;
    private final TicketImageRepository ticketImageRepository;
    private final TicketCommentRepository ticketCommentRepository;

    /** Months a terminal ticket is kept before it is purged. */
    @Value("${tickets.retention-months:3}")
    private int retentionMonths;

    /** Master switch for the destructive purge. */
    @Value("${tickets.retention-enabled:true}")
    private boolean retentionEnabled;

    public TicketRetentionScheduler(TicketRepository ticketRepository,
                                    TicketImageRepository ticketImageRepository,
                                    TicketCommentRepository ticketCommentRepository) {
        this.ticketRepository = ticketRepository;
        this.ticketImageRepository = ticketImageRepository;
        this.ticketCommentRepository = ticketCommentRepository;
    }

    // Runs daily at 03:00, off-peak.
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTickets() {
        if (!retentionEnabled) {
            log.info("Ticket retention is disabled (tickets.retention-enabled=false); skipping purge.");
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusMonths(retentionMonths);
        List<Ticket> expired = ticketRepository
                .findByStatusInAndStatusChangedAtLessThanEqual(TERMINAL_STATUSES, cutoff);

        if (expired.isEmpty()) {
            log.info("Ticket retention: no CLOSED/COMPLETED tickets older than {} month(s).", retentionMonths);
            return;
        }

        int tickets = 0;
        long images = 0;
        for (Ticket ticket : expired) {
            Long id = ticket.getId();
            try {
                long imageCount = ticketImageRepository.countByTicketId(id);
                // Remove dependents first (photos then comments), then the ticket itself.
                ticketImageRepository.deleteByTicketId(id);
                ticketCommentRepository.deleteByTicketId(id);
                ticketRepository.delete(ticket);
                tickets++;
                images += imageCount;
            } catch (Exception e) {
                log.error("Ticket retention: failed to purge ticket {} (#{}): {}",
                        id, ticket.getTicketNumber(), e.getMessage(), e);
            }
        }

        log.info("Ticket retention purge complete: removed {} ticket(s) and {} photo(s) " +
                "(terminal and older than {} month(s), cutoff {}).", tickets, images, retentionMonths, cutoff);
    }
}
