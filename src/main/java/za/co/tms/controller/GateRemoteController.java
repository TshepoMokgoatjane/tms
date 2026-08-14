package za.co.tms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.tms.domain.GateRemote;
import za.co.tms.service.GateRemoteService;

import java.util.List;

@RestController
@RequestMapping("/gate-remotes")
public class GateRemoteController {

    private final GateRemoteService gateRemoteService;

    @Autowired
    public GateRemoteController(GateRemoteService gateRemoteService) {
        this.gateRemoteService = gateRemoteService;
    }

    @GetMapping("/find/all")
    @Operation(summary = "Get all gate remotes")
    public ResponseEntity<List<GateRemote>> retrieveGateRemotes() {
        return ResponseEntity.ok(gateRemoteService.findAll());
    }

    @GetMapping("/find/by/{id}")
    @Operation(summary = "Get gate remote by ID")
    public ResponseEntity<GateRemote> retrieveGateRemoteById(@PathVariable Long id) {
        return ResponseEntity.ok(gateRemoteService.findGateRemoteById(id));
    }

    @GetMapping("/find/by-tenant/{tenantId}")
    @Operation(summary = "Find all gate remotes assigned to a tenant",
               description = "Returns a list of gate remotes issued to the specified tenant. Empty list if none assigned.")
    public ResponseEntity<List<GateRemote>> retrieveGateRemotesByTenantId(@PathVariable Integer tenantId) {
        List<GateRemote> remotes = gateRemoteService.findByTenantId(tenantId);
        return ResponseEntity.ok(remotes);
    }

    @PostMapping("/create")
    @Operation(summary = "Create a new gate remote", description = "Registers a new gate remote assigned to a tenant")
    @ApiResponse(responseCode = "200", description = "Gate Remote created successfully")
    public ResponseEntity<GateRemote> createGateRemote(@RequestBody GateRemote gateRemote) {
        return ResponseEntity.ok(gateRemoteService.addGateRemote(gateRemote));
    }

    @PutMapping("/update/{id}")
    @Operation(summary = "Update a gate remote")
    public ResponseEntity<GateRemote> updateGateRemote(@PathVariable Long id, @RequestBody GateRemote gateRemote) {
        return ResponseEntity.ok(gateRemoteService.updateGateRemote(gateRemote, id));
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Soft-delete a gate remote", description = "Sets the gate remote status to INACTIVE")
    public ResponseEntity<Void> deleteGateRemote(@PathVariable Long id) {
        gateRemoteService.deleteGateRemote(id);
        return ResponseEntity.noContent().build();
    }
}
