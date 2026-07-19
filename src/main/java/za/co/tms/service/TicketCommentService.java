package za.co.tms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.tms.domain.TicketComment;
import za.co.tms.repository.TicketCommentRepository;

import java.util.List;

@Service
public class TicketCommentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TicketCommentService.class);

    public final TicketCommentRepository ticketCommentRepository;

    @Autowired
    public TicketCommentService(TicketCommentRepository ticketCommentRepository) {
        this.ticketCommentRepository = ticketCommentRepository;
    }

    public List<TicketComment> retrieveCommentsById(Long ticketId) {
        LOGGER.info("Retrieve Comments By Id: {}", ticketId);
        return ticketCommentRepository.findByTicketIdOrderByCreatedAtDesc(ticketId);
    }

    public TicketComment createComment(Long ticketId, TicketComment ticketComment) {
        LOGGER.info("Create Comment: {}", ticketComment);
        return ticketCommentRepository.save(ticketComment);
    }
}
