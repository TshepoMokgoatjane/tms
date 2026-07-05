package za.co.tms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.tms.domain.GateRemote;
import za.co.tms.domain.GateRemoteStatus;
import za.co.tms.exception.GateRemoteNotFoundException;
import za.co.tms.repository.GateRemoteRepository;

import java.util.List;

@Service
public class GateRemoteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GateRemoteService.class);

    private final GateRemoteRepository gateRemoteRepository;

    @Autowired
    public GateRemoteService(GateRemoteRepository gateRemoteRepository) {
        this.gateRemoteRepository = gateRemoteRepository;
    }

    public List<GateRemote> findAll() {
        return gateRemoteRepository.findAll();
    }

    public GateRemote findGateRemoteById(Long id) {

        LOGGER.info("Finding Gate Remote with id {}", id);

        return gateRemoteRepository.findById(id).orElseThrow(() -> new GateRemoteNotFoundException("Gate Remote with id '" + id + "' was not found!"));
    }

    public GateRemote addGateRemote(GateRemote gateRemote) {

        LOGGER.info("Register a new gate remote");

        gateRemote.setId(null); // Ensures it is a new gate remote

        return gateRemoteRepository.save(gateRemote);
    }

    public void deleteGateRemote(Long id) {

        LOGGER.info("Delete Gate Remote with id {}", id);

        GateRemote gateRemote = findGateRemoteById(id);
        gateRemote.setGateRemoteStatus(GateRemoteStatus.INACTIVE);

        gateRemoteRepository.save(gateRemote);
    }

    public GateRemote updateGateRemote(GateRemote gateRemote, Long id) {

        LOGGER.info("Update Gate Remote with id {}", id);

        GateRemote existingGateRemote = findGateRemoteById(id);

        existingGateRemote.setSerialNumber(gateRemote.getSerialNumber());
        existingGateRemote.setRoom(gateRemote.getRoom());
        existingGateRemote.setBrand(gateRemote.getBrand());
        existingGateRemote.setGateRemoteStatus(gateRemote.getGateRemoteStatus());
        existingGateRemote.setIssuedToTenant(gateRemote.getIssuedToTenant());
        existingGateRemote.setIssuedDate(gateRemote.getIssuedDate());
        existingGateRemote.setReturnedDate(gateRemote.getReturnedDate());

        return gateRemoteRepository.save(existingGateRemote);
    }
}
