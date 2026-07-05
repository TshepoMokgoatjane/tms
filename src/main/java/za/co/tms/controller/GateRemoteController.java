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
    public ResponseEntity<List<GateRemote>> retrieveGateRemotes() {
        return ResponseEntity.ok(gateRemoteService.findAll());
    }

    @GetMapping("/find/by/{id}")
    public ResponseEntity<GateRemote> retrieveGateRemoteById(@PathVariable Long id) {
        return ResponseEntity.ok(gateRemoteService.findGateRemoteById(id));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<GateRemote> updateGateRemote(@PathVariable Long id, @RequestBody GateRemote gateRemote) {
        return ResponseEntity.ok(gateRemoteService.updateGateRemote(gateRemote, id));
    }

    @PostMapping("/create")
    @Operation(summary = "Create a new gate remote", description = "Registers a new gate remote to be assigned to the remote/tenant")
    @ApiResponse(responseCode = "200", description = "Gate Remote record created successfully")
    public ResponseEntity<GateRemote> createGateRemote(@RequestBody GateRemote gateRemote) {
        return ResponseEntity.ok(gateRemoteService.addGateRemote(gateRemote));
    }
}
