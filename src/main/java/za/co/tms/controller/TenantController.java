package za.co.tms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import za.co.tms.dto.TenantResponseDTO;
import za.co.tms.model.Tenant;
import za.co.tms.service.TenantService;

import java.util.List;
import java.util.stream.Collectors;

@Validated
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/tenants")
public class TenantController {

    private final TenantService tenantService;

    @Autowired
    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping(path="/find/all")
	public ResponseEntity<List<Tenant>> retrieveTenants() {
        return ResponseEntity.ok(tenantService.findAllTenants());
	}
	
	@GetMapping(path="/find/by-name/{name}")
	public ResponseEntity<Tenant> retrieveTenant(@PathVariable String name) {
		return ResponseEntity.ok(tenantService.findTenantByName(name));
	}
	
	@GetMapping(path="/find/by-id/{id}")
	public ResponseEntity<Tenant> retrieveTenantById(@PathVariable Integer id) {
		return ResponseEntity.ok(tenantService.findTenantById(id));
	}
	
	@DeleteMapping(path="/delete/{id}")
	public ResponseEntity<Void> deleteTenantById(@PathVariable Integer id) {
		tenantService.deleteTenantById(id);
		return ResponseEntity.noContent().build();
	}
	
	@PutMapping(path="/update/{id}")
	public ResponseEntity<Tenant> updateTenant(@PathVariable int id, @RequestBody Tenant tenant) {
		Tenant updatedTenant = tenantService.updateTenant(id, tenant);
        return ResponseEntity.ok(updatedTenant);
	}
	
	@PostMapping("/create")
    @Operation(summary = "Create a new tenant", description = "Registers a new tenant with lease and contact details")
    @ApiResponse(responseCode = "200", description = "Tenant created successfully")
	public ResponseEntity<Tenant> createTenant(@Valid @RequestBody Tenant tenant) {
		Tenant createdTenant = tenantService.addTenant(tenant);
		
		return ResponseEntity.ok(createdTenant);
	}
	
	@GetMapping("/auth/check")
	public ResponseEntity<String> basicAuthCheck() {

        return ResponseEntity.ok("Success");
	}

    @GetMapping("rent-due-today")
    public ResponseEntity<List<TenantResponseDTO>> getTenantDueToday() {
        List<Tenant> allTenants = tenantService.findAllTenants();
        List<Tenant> dueToday = tenantService.getTenantsWithRentDueToday(allTenants);
        List<TenantResponseDTO> response = dueToday.stream().map(TenantResponseDTO:: new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}