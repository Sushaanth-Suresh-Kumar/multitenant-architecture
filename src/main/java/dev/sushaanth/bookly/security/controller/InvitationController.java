package dev.sushaanth.bookly.security.controller;

import dev.sushaanth.bookly.security.dto.InvitationRequest;
import dev.sushaanth.bookly.security.dto.InvitationResponse;
import dev.sushaanth.bookly.security.model.EmployeeInvitation;
import dev.sushaanth.bookly.security.model.LibraryUser;
import dev.sushaanth.bookly.security.repository.EmployeeInvitationRepository;
import dev.sushaanth.bookly.security.repository.LibraryUserRepository;
import dev.sushaanth.bookly.security.service.AuthService;
import dev.sushaanth.bookly.tenant.exception.TenantNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {
    private static final Logger logger = LoggerFactory.getLogger(InvitationController.class);

    private final EmployeeInvitationRepository invitationRepository;
    private final LibraryUserRepository userRepository;
    private final AuthService authService;

    public InvitationController(EmployeeInvitationRepository invitationRepository,
                                LibraryUserRepository userRepository,
                                AuthService authService) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
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
                .orElseThrow(() -> new TenantNotFoundException("Invitation not found"));

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
                .orElseThrow(() -> new TenantNotFoundException("Invitation not found"));

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

    /**
     * Gets the tenant ID of the currently authenticated user
     * @return UUID of the user's tenant
     * @throws AccessDeniedException if user is not authenticated
     */
    private UUID getCurrentUserTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            return userRepository.findByUsername(username)
                    .map(LibraryUser::getTenantId)
                    .orElseThrow(() -> new AccessDeniedException("User has no associated tenant"));
        }
        throw new AccessDeniedException("Not authenticated");
    }

    /**
     * Gets the ID of the currently authenticated user
     * @return UUID of the user
     * @throws AccessDeniedException if user is not authenticated
     */
    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            return userRepository.findByUsername(username)
                    .map(LibraryUser::getId)
                    .orElseThrow(() -> new AccessDeniedException("User not found"));
        }
        throw new AccessDeniedException("Not authenticated");
    }

    /**
     * Maps an EmployeeInvitation entity to an InvitationResponse DTO
     * @param invitation The invitation entity
     * @return The response DTO
     */
    private InvitationResponse mapToResponse(EmployeeInvitation invitation) {
        String invitedByName = "Unknown";

        // Try to get the name of the admin who sent the invitation
        if (invitation.getInvitedBy() != null) {
            invitedByName = userRepository.findById(invitation.getInvitedBy())
                    .map(user -> user.getFirstName() + " " + user.getLastName())
                    .orElse("Unknown");
        }

        return new InvitationResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getInvitedBy(),
                invitedByName,
                invitation.getCreatedAt(),
                invitation.getExpiresAt(),
                invitation.isUsed()
        );
    }
}