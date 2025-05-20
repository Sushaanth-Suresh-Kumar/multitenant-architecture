package dev.sushaanth.bookly.security.service;

import dev.sushaanth.bookly.exception.BooklyException;
import dev.sushaanth.bookly.exception.BooklyException.ErrorCode;
import dev.sushaanth.bookly.security.dto.InvitationResponse;
import dev.sushaanth.bookly.security.model.EmployeeInvitation;
import dev.sushaanth.bookly.security.model.LibraryUser;
import dev.sushaanth.bookly.security.model.Role;
import dev.sushaanth.bookly.security.repository.EmployeeInvitationRepository;
import dev.sushaanth.bookly.security.repository.LibraryUserRepository;
import dev.sushaanth.bookly.tenant.Tenant;
import dev.sushaanth.bookly.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InvitationService {
    private static final Logger logger = LoggerFactory.getLogger(InvitationService.class);

    private final EmployeeInvitationRepository invitationRepository;
    private final LibraryUserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final EmailService emailService;

    public InvitationService(
            EmployeeInvitationRepository invitationRepository,
            LibraryUserRepository userRepository,
            TenantRepository tenantRepository,
            EmailService emailService) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.emailService = emailService;
    }

    public List<InvitationResponse> getPendingInvitations(UUID adminId) {
        // Get admin's tenant ID
        LibraryUser admin = validateAdmin(adminId);

        return invitationRepository.findByTenantIdAndUsedFalse(admin.getTenantId())
                .stream()
                .map(this::mapToInvitationResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public InvitationResponse createEmployeeInvitation(String email, UUID adminId) {
        // Validate admin and tenant
        LibraryUser admin = validateAdmin(adminId);

        // Check if email already exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BooklyException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Email already registered in the system"
            );
        }

        // Check if invitation already exists
        if (invitationRepository.findByEmailAndUsedFalse(email).isPresent()) {
            throw new BooklyException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Invitation already exists for this email"
            );
        }

        Tenant tenant = tenantRepository.findById(admin.getTenantId())
                .orElseThrow(() -> new BooklyException(
                        ErrorCode.TENANT_NOT_FOUND,
                        "Tenant not found"
                ));

        // Create invitation
        EmployeeInvitation invitation = new EmployeeInvitation();
        invitation.setEmail(email);
        invitation.setTenantId(admin.getTenantId());
        invitation.setInvitedBy(adminId);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));

        invitation = invitationRepository.save(invitation);

        // Send invitation email
        emailService.sendEmployeeInvitation(
                email,
                admin.getUsername(),
                tenant.getDisplayName(),
                invitation.getId().toString()
        );

        logger.info("Created invitation for {} in tenant {}",
                email, tenant.getDisplayName());

        return mapToInvitationResponse(invitation);
    }

    @Transactional
    public InvitationResponse resendInvitation(UUID invitationId, UUID adminId) {
        // Validate admin
        LibraryUser admin = validateAdmin(adminId);

        // Find invitation
        EmployeeInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new BooklyException(
                        ErrorCode.INVALID_CREDENTIALS,
                        "Invitation not found"
                ));

        // Verify admin has permission (same tenant)
        if (!invitation.getTenantId().equals(admin.getTenantId())) {
            throw new BooklyException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Not authorized to manage this invitation"
            );
        }

        // Verify invitation is still valid
        if (invitation.isUsed()) {
            throw new BooklyException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Invitation has already been used"
            );
        }

        // Update expiration date
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        invitation = invitationRepository.save(invitation);

        // Get tenant info
        Tenant tenant = tenantRepository.findById(invitation.getTenantId())
                .orElseThrow(() -> new BooklyException(
                        ErrorCode.TENANT_NOT_FOUND,
                        "Tenant not found"
                ));

        // Resend email
        emailService.sendEmployeeInvitation(
                invitation.getEmail(),
                admin.getUsername(),
                tenant.getDisplayName(),
                invitation.getId().toString()
        );

        logger.info("Resent invitation to {} for tenant {}",
                invitation.getEmail(), tenant.getDisplayName());

        return mapToInvitationResponse(invitation);
    }

    @Transactional
    public void deleteInvitation(UUID invitationId, UUID adminId) {
        // Validate admin
        LibraryUser admin = validateAdmin(adminId);

        // Find invitation
        EmployeeInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new BooklyException(
                        ErrorCode.INVALID_CREDENTIALS,
                        "Invitation not found"
                ));

        // Verify admin has permission (same tenant)
        if (!invitation.getTenantId().equals(admin.getTenantId())) {
            throw new BooklyException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Not authorized to manage this invitation"
            );
        }

        // Delete invitation
        invitationRepository.delete(invitation);

        logger.info("Deleted invitation for email: {}", invitation.getEmail());
    }

    // Validate admin and return user object
    private LibraryUser validateAdmin(UUID adminId) {
        LibraryUser admin = userRepository.findById(adminId)
                .orElseThrow(() -> new BooklyException(
                        ErrorCode.INVALID_CREDENTIALS,
                        "Admin user not found"
                ));

        // Verify user is a library admin
        if (admin.getRole() != Role.ROLE_LIBRARY_ADMIN) {
            throw new BooklyException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Only library administrators can manage invitations"
            );
        }

        return admin;
    }

    // Map entity to response DTO
    private InvitationResponse mapToInvitationResponse(EmployeeInvitation invitation) {
        String invitedByName = "Unknown";

        // Try to get the admin's name
        if (invitation.getInvitedBy() != null) {
            invitedByName = userRepository.findById(invitation.getInvitedBy())
                    .map(user -> user.getFirstName() + " " + user.getLastName())
                    .orElse("Unknown");
        }

        boolean expired = invitation.getExpiresAt().isBefore(LocalDateTime.now());
        InvitationResponse.InvitationStatus status;

        if (invitation.isUsed()) {
            status = InvitationResponse.InvitationStatus.USED;
        } else if (expired) {
            status = InvitationResponse.InvitationStatus.EXPIRED;
        } else {
            status = InvitationResponse.InvitationStatus.PENDING;
        }

        return new InvitationResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getInvitedBy(),
                invitedByName,
                invitation.getCreatedAt(),
                invitation.getExpiresAt(),
                invitation.isUsed(),
                expired,
                status
        );
    }
}