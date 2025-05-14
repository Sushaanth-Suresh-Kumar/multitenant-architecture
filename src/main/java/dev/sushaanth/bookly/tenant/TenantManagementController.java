package dev.sushaanth.bookly.tenant;

import dev.sushaanth.bookly.tenant.dto.TenantCreateRequest;
import dev.sushaanth.bookly.tenant.dto.TenantResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
public class TenantManagementController {
    private static final Logger logger = LoggerFactory.getLogger(TenantManagementController.class);

    private final TenantService tenantService;

    public TenantManagementController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    public List<TenantResponse> getAllTenants() {
        logger.info("Retrieving all tenants");
        return tenantService.getAllTenants();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse createTenant(@Valid @RequestBody TenantCreateRequest request) {
        logger.info("Received request to create new tenant");
        return tenantService.createTenant(request);
    }
}