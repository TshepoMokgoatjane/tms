package za.co.tms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.tms.model.Tenant;
import za.co.tms.service.TenantService;

import java.util.List;

@RestController
@RequestMapping("/tenants")
public class TenantController {

    private final TenantService tenantService;

    @Autowired
    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping(path="/find/all")
	public List<Tenant> retrieveTenants() {
		return tenantService.findAllTenants();
	}
	
	@GetMapping(path="/find/{name}")
	public Tenant retrieveTenant(@PathVariable String name) {
		return tenantService.findTenantByName(name);
	}
	
	@GetMapping(path="/find/by/{id}")
	public Tenant retrieveTenantById(@PathVariable int id) {
		return tenantService.findTenantById(id);
	}
	
	@DeleteMapping(path="/delete/{id}")
	public ResponseEntity<Void> deleteTenantById(@PathVariable int id) {
		tenantService.deleteTenantById(id);
		return ResponseEntity.noContent().build();
	}
	
	@PutMapping(path="/update/{id}")
	public Tenant updateTenant(@PathVariable int id, @RequestBody Tenant tenant) {
		tenantService.updateTenant(tenant);
		
		return tenant;
	}
	
	@PostMapping("/create")
	public Tenant createTodo(@RequestBody Tenant tenant) {
		Tenant createdTenant = tenantService.addTenant(tenant);
		
		return createdTenant;
	}
	
	@GetMapping("/basicauth")
	public String basicAuthChec() {
		return "Success";
	}
}