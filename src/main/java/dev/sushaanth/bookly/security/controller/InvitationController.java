package dev.sushaanth.bookly.security.controller;

import dev.sushaanth.bookly.security.dto.InvitationRequest;
import dev.sushaanth.bookly.security.dto.InvitationResponse;
import dev.sushaanth.bookly.security.service.InvitationService;
import dev.sushaanth.bookly.security.utils.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {
    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_LIBRARY_ADMIN')")
    public List<InvitationResponse> getPendingInvitations() {
        UUID adminId = SecurityUtils.getCurrentUserId();
        return invitationService.getPendingInvitations(adminId);
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_LIBRARY_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationResponse createInvitation(@Valid @RequestBody InvitationRequest request) {
        UUID adminId = SecurityUtils.getCurrentUserId();
        return invitationService.createEmployeeInvitation(request.email(), adminId);
    }

    @PostMapping("/resend/{id}")
    @PreAuthorize("hasRole('ROLE_LIBRARY_ADMIN')")
    public InvitationResponse resendInvitation(@PathVariable UUID id) {
        UUID adminId = SecurityUtils.getCurrentUserId();
        return invitationService.resendInvitation(id, adminId);
    }
}