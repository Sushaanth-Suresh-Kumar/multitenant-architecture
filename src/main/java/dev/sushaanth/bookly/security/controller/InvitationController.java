package dev.sushaanth.bookly.security.controller;

import dev.sushaanth.bookly.security.dto.InvitationRequest;
import dev.sushaanth.bookly.security.dto.InvitationResponse;
import dev.sushaanth.bookly.security.model.EmployeeInvitation;
import dev.sushaanth.bookly.security.repository.EmployeeInvitationRepository;
import dev.sushaanth.bookly.security.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private final EmployeeInvitationRepository invitationRepository;
    private final AuthService authService;

    public InvitationController(EmployeeInvitationRepository invitationRepository,
                                AuthService authService) {
        this.invitationRepository = invitationRepository;
        this.authService = authService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_LIBRARY_ADMIN')")
    public List<InvitationResponse> getPendingInvitations() {
        // Get current user's tenant ID from security context
        UUID tenantId = getCurrentUserTenantId();

        return invitationRepository.findByTenantIdAndUsedFalse(tenantId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_LIBRARY_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationResponse createInvitation(@Valid @RequestBody InvitationRequest request) {
        // Get current user's ID from security context
        UUID adminId = getCurrentUserId();

        authService.createEmployeeInvitation(request.email(), adminId);

        // Return the created invitation
        return invitationRepository.findByEmailAndUsedFalse(request.email())
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Failed to retrieve created invitation"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_LIBRARY_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInvitation(@PathVariable UUID id) {
        // Get current user's tenant ID
        UUID tenantId = getCurrentUserTenantId();

        // Find invitation and verify it belongs to the user's tenant
        EmployeeInvitation invitation = invitationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invitation not found"));

        if (!invitation.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("You don't have permission to delete this invitation");
        }

        invitationRepository.delete(invitation);
    }

    @PostMapping("/resend/{id}")
    @PreAuthorize("hasRole('ROLE_LIBRARY_ADMIN')")
    public InvitationResponse resendInvitation(@PathVariable UUID id) {
        // Get current user's tenant ID
        UUID tenantId = getCurrentUserTenantId();

        // Find invitation and verify it belongs to the user's tenant
        EmployeeInvitation invitation = invitationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invitation not found"));

        if (!invitation.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("You don't have permission to resend this invitation");
        }

        // Update expiration date
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        invitationRepository.save(invitation);

        // Resend the invitation email
        authService.resendEmployeeInvitation(invitation);

        return mapToResponse(invitation);
    }

    // Helper methods and DTOs...

}
