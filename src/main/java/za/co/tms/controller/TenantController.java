package za.co.tms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.tms.model.Tenant;
import za.co.tms.service.TenantService;

import java.util.List;

@RestController
@RequestMapping("/tenant")
public class TenantController {

    private final TenantService tenantService;

    @Autowired
    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<Tenant>> getAllTenants() {
        List<Tenant> tenants = this.tenantService.findAllTenants();
        return new ResponseEntity<>(tenants, HttpStatus.OK);
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<Tenant> getTenantById(@PathVariable("id") Long id) {
        Tenant tenant = this.tenantService.findTenantById(id);
        return new ResponseEntity<>(tenant, HttpStatus.OK);
    }

    @PostMapping("/add")
    public ResponseEntity<Tenant> addTenant(@RequestBody Tenant tenant) {
        Tenant newTenant = this.tenantService.addTenant(tenant);
        return new ResponseEntity<>(newTenant, HttpStatus.CREATED);
    }

    @PutMapping("/update")
    public ResponseEntity<Tenant> updateTenant(@RequestBody Tenant tenant) {
        Tenant updateTenant = this.tenantService.updateTenant(tenant);
        return new ResponseEntity<>(updateTenant, HttpStatus.OK);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteTenant(@PathVariable("id") Long id) {
        this.tenantService.deleteTenant(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
