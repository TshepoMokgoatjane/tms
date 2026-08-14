package za.co.tms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.tms.domain.GateRemote;
import za.co.tms.repository.GateRemoteRepository;
import za.co.tms.service.GateRemoteService;

import java.util.List;

@RestController
@RequestMapping("/gate-remotes")
public class GateRemoteController {

    private final GateRemoteService gateRemoteService;
    private final GateRemoteRepository gateRemoteRepository;

    @Autowired
    public GateRemoteController(GateRemoteService gateRemoteService, GateRemoteRepository gateRemoteRepository) {
        this.gateRemoteService = gateRemoteService;
        this.gateRemoteRepository = gateRemoteRepository;
    }

    @GetMapping("/find/all")
    public ResponseEntity<List<GateRemote>> retrieveGateRemotes() {
        return ResponseEntity.ok(gateRemoteService.findAll());
    }

    @GetMapping("/find/by/{id}")
    public ResponseEntity<GateRemote> retrieveGateRemoteById(@PathVariable Long id) {
        return ResponseEntity.ok(gateRemoteService.findGateRemoteById(id));
    }

    @GetMapping("/find/by-tenant/{tenantId}")
    @Operation(summary = "Find gate remote assigned to a tenant", description = "Returns the gate remote currently issued to the specified tenant")
    public ResponseEntity<GateRemote> retrieveGateRemoteByTenantId(@PathVariable Integer tenantId) {
        return gateRemoteRepository.findByIssuedToTenantId(tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/find/by-room/{roomId}")
    @Operation(summary = "Find gate remotes assigned to a room")
    public ResponseEntity<List<GateRemote>> retrieveGateRemotesByRoomId(@PathVariable Long roomId) {
        return ResponseEntity.ok(gateRemoteRepository.findByRoomId(roomId));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<GateRemote> updateGateRemote(@PathVariable Long id, @RequestBody GateRemote gateRemote) {
        return ResponseEntity.ok(gateRemoteService.updateGateRemote(gateRemote, id));
    }

    @PostMapping("/create")
    @Operation(summary = "Create a new gate remote", description = "Registers a new gate remote to be assigned to the room/tenant")
    @ApiResponse(responseCode = "200", description = "Gate Remote record created successfully")
    public ResponseEntity<GateRemote> createGateRemote(@RequestBody GateRemote gateRemote) {
        return ResponseEntity.ok(gateRemoteService.addGateRemote(gateRemote));
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Soft-delete a gate remote", description = "Sets the gate remote status to INACTIVE")
    public ResponseEntity<Void> deleteGateRemote(@PathVariable Long id) {
        gateRemoteService.deleteGateRemote(id);
        return ResponseEntity.noContent().build();
    }
}
