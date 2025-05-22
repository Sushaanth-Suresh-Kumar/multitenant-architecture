package dev.sushaanth.bookly.security.service;

import dev.sushaanth.bookly.exception.BooklyException;
import dev.sushaanth.bookly.exception.BooklyException.ErrorCode;
import dev.sushaanth.bookly.multitenancy.context.TenantContext;
import dev.sushaanth.bookly.security.dto.*;
import dev.sushaanth.bookly.security.model.EmployeeInvitation;
import dev.sushaanth.bookly.security.model.LibraryUser;
import dev.sushaanth.bookly.security.model.Role;
import dev.sushaanth.bookly.security.model.VerificationToken;
import dev.sushaanth.bookly.security.repository.EmployeeInvitationRepository;
import dev.sushaanth.bookly.security.repository.LibraryUserRepository;
import dev.sushaanth.bookly.security.repository.VerificationTokenRepository;
import dev.sushaanth.bookly.tenant.Tenant;
import dev.sushaanth.bookly.tenant.TenantRepository;
import dev.sushaanth.bookly.tenant.TenantService;
import dev.sushaanth.bookly.tenant.dto.TenantCreateRequest;
import dev.sushaanth.bookly.tenant.dto.TenantResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
public class RegistrationService {
    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    private final VerificationTokenRepository tokenRepository;
    private final LibraryUserRepository userRepository;
    private final EmployeeInvitationRepository invitationRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TenantService tenantService;

    public RegistrationService(
            VerificationTokenRepository tokenRepository,
            LibraryUserRepository userRepository,
            EmployeeInvitationRepository invitationRepository,
            TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            TenantService tenantService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.tenantService = tenantService;
    }

    @Transactional
    public EmailVerificationResponse initiateEmailVerification(InitialRegistrationRequest request) {
        // Validate email is not already registered
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BooklyException(ErrorCode.INVALID_CREDENTIALS, "Email already registered");
        }

        // This is now ONLY for library admin registration - no need to check invitation
        // Remove all the employee registration logic from here

        // Generate OTP
        String otp = generateOtp();

        // Create or update verification token
        VerificationToken token = tokenRepository.findByEmail(request.email())
                .orElse(new VerificationToken());

        token.setEmail(request.email());
        token.setToken(otp);
        token.setVerified(false);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(10));
        token.setCreatedAt(LocalDateTime.now());

        tokenRepository.save(token);

        // Send OTP email
        emailService.sendOtp(request.email(), otp);

        logger.info("Library admin email verification initiated for: {}", request.email());

        return new EmailVerificationResponse(
                request.email(),
                "Verification code sent to your email",
                token.getExpiryDate()
        );
    }

    @Transactional
    public EmailVerificationResult verifyEmail(VerificationRequest request) {
        // Find verification token
        VerificationToken token = tokenRepository.findByEmailAndToken(request.email(), request.otp())
                .orElseThrow(() -> new BooklyException(ErrorCode.INVALID_OTP, "Invalid OTP"));

        // Validate token
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BooklyException(ErrorCode.EXPIRED_OTP, "OTP expired");
        }

        if (token.isVerified()) {
            throw new BooklyException(ErrorCode.ALREADY_USED_OTP, "OTP already used");
        }

        // Mark token as verified
        token.setVerified(true);
        tokenRepository.save(token);

        logger.info("Email verified for: {}", request.email());

        return new EmailVerificationResult(
                true,
                request.email(),
                "Email verified successfully"
        );
    }

    @Transactional
    public RegistrationResponse completeRegistration(CompleteRegistrationRequest request) {
        // Find verification token
        VerificationToken token = tokenRepository.findByEmail(request.email())
                .orElseThrow(() -> new BooklyException(
                        ErrorCode.INVALID_CREDENTIALS,
                        "No verification found for this email"
                ));

        // Validate the token is verified
        if (!token.isVerified()) {
            throw new BooklyException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Email not verified yet"
            );
        }

        // Validate unique username
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new BooklyException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Username already taken"
            );
        }

        // This endpoint is now ONLY for library admin registration
        return completeLibraryRegistration(request);
    }

    private RegistrationResponse completeLibraryRegistration(CompleteRegistrationRequest request) {
        // Check for existing library name
        if (tenantRepository.findByDisplayName(request.libraryName()).isPresent()) {
            throw new BooklyException(
                    ErrorCode.TENANT_ALREADY_EXISTS,
                    "Library name already taken"
            );
        }

        try {
            // Create tenant
            TenantCreateRequest tenantRequest = new TenantCreateRequest(
                    request.libraryName(),
                    "Library tenant for " + request.libraryName()
            );

            TenantResponse tenantResponse = tenantService.createTenant(tenantRequest);

            // Create library admin user
            LibraryUser user = new LibraryUser();
            user.setUsername(request.username());
            user.setEmail(request.email());
            user.setFirstName(request.firstName());
            user.setLastName(request.lastName());
            user.setPassword(passwordEncoder.encode(request.password()));
            user.setRole(Role.ROLE_LIBRARY_ADMIN);
            user.setTenantId(tenantResponse.id());

            userRepository.save(user);

            // Create user profile in tenant schema
            createUserProfileInTenant(user, tenantResponse.schemaName());

            logger.info("Library tenant created: {} with admin: {}",
                    tenantResponse.displayName(), user.getUsername());

            return new RegistrationResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole().name(),
                    user.getTenantId()
            );
        } catch (Exception e) {
            logger.error("Failed to complete library registration", e);
            throw new BooklyException(
                    ErrorCode.TENANT_CREATION_FAILED,
                    "Failed to complete registration: " + e.getMessage()
            );
        }
    }

    private RegistrationResponse completeEmployeeRegistration(
            CompleteRegistrationRequest request, EmployeeInvitation invitation) {
        try {
            // Get tenant
            Tenant tenant = tenantRepository.findById(invitation.getTenantId())
                    .orElseThrow(() -> new BooklyException(
                            ErrorCode.TENANT_NOT_FOUND,
                            "Tenant not found"
                    ));

            // Create employee user
            LibraryUser user = new LibraryUser();
            user.setUsername(request.username());
            user.setEmail(request.email());
            user.setFirstName(request.firstName());
            user.setLastName(request.lastName());
            user.setPassword(passwordEncoder.encode(request.password()));
            user.setRole(Role.ROLE_EMPLOYEE);
            user.setTenantId(invitation.getTenantId());

            userRepository.save(user);

            // Create user profile in tenant schema
            createUserProfileInTenant(user, tenant.getSchemaName());

            // Mark invitation as used
            invitation.setUsed(true);
            invitationRepository.save(invitation);

            logger.info("Employee registration completed for: {} in tenant: {}",
                    user.getEmail(), tenant.getDisplayName());

            return new RegistrationResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole().name(),
                    user.getTenantId()
            );
        } catch (BooklyException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to complete employee registration", e);
            throw new BooklyException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Failed to complete registration: " + e.getMessage()
            );
        }
    }

    @Transactional(readOnly = true)
    public InvitationValidationResponse validateInvitation(UUID invitationId) {
        try {
            EmployeeInvitation invitation = invitationRepository.findById(invitationId)
                    .orElseThrow(() -> new BooklyException(
                            ErrorCode.INVALID_CREDENTIALS,
                            "Invalid or expired invitation"
                    ));

            // Check if invitation is still valid
            if (invitation.isUsed()) {
                throw new BooklyException(
                        ErrorCode.INVALID_CREDENTIALS,
                        "This invitation has already been used"
                );
            }

            if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new BooklyException(
                        ErrorCode.EXPIRED_INVITATION,
                        "This invitation has expired"
                );
            }

            // Check if email is already registered
            if (userRepository.findByEmail(invitation.getEmail()).isPresent()) {
                throw new BooklyException(
                        ErrorCode.INVALID_CREDENTIALS,
                        "An account with this email already exists"
                );
            }

            // Get tenant and inviter info
            Tenant tenant = tenantRepository.findById(invitation.getTenantId())
                    .orElseThrow(() -> new BooklyException(
                            ErrorCode.TENANT_NOT_FOUND,
                            "Library not found"
                    ));

            String invitedByName = "Unknown";
            if (invitation.getInvitedBy() != null) {
                invitedByName = userRepository.findById(invitation.getInvitedBy())
                        .map(user -> user.getFirstName() + " " + user.getLastName())
                        .orElse("Unknown");
            }

            return new InvitationValidationResponse(
                    true,
                    invitation.getEmail(),
                    tenant.getDisplayName(),
                    invitedByName,
                    invitation.getExpiresAt(),
                    "Invitation is valid"
            );
        } catch (BooklyException e) {
            return new InvitationValidationResponse(
                    false,
                    null,
                    null,
                    null,
                    null,
                    e.getMessage()
            );
        }
    }

    @Transactional
    public RegistrationResponse registerEmployeeDirectly(DirectEmployeeRegistrationRequest request) {
        // Validate invitation
        EmployeeInvitation invitation = invitationRepository.findById(request.invitationId())
                .orElseThrow(() -> new BooklyException(
                        ErrorCode.INVALID_CREDENTIALS,
                        "Invalid invitation ID"
                ));

        // Check if invitation is still valid
        if (invitation.isUsed()) {
            throw new BooklyException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "This invitation has already been used"
            );
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BooklyException(
                    ErrorCode.EXPIRED_INVITATION,
                    "This invitation has expired"
            );
        }

        // Check if email is already registered
        if (userRepository.findByEmail(invitation.getEmail()).isPresent()) {
            throw new BooklyException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "An account with this email already exists"
            );
        }

        // Validate unique username
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new BooklyException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Username is already taken"
            );
        }

        try {
            // Get tenant
            Tenant tenant = tenantRepository.findById(invitation.getTenantId())
                    .orElseThrow(() -> new BooklyException(
                            ErrorCode.TENANT_NOT_FOUND,
                            "Library not found"
                    ));

            // Create employee user
            LibraryUser user = new LibraryUser();
            user.setUsername(request.username());
            user.setEmail(invitation.getEmail()); // Use email from invitation
            user.setFirstName(request.firstName());
            user.setLastName(request.lastName());
            user.setPassword(passwordEncoder.encode(request.password()));
            user.setRole(Role.ROLE_EMPLOYEE);
            user.setTenantId(invitation.getTenantId());

            userRepository.save(user);

            // Create user profile in tenant schema
            createUserProfileInTenant(user, tenant.getSchemaName());

            // Mark invitation as used
            invitation.setUsed(true);
            invitationRepository.save(invitation);

            logger.info("Direct employee registration completed for: {} in tenant: {}",
                    user.getEmail(), tenant.getDisplayName());

            return new RegistrationResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole().name(),
                    user.getTenantId()
            );
        } catch (BooklyException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to complete direct employee registration", e);
            throw new BooklyException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Failed to complete registration: " + e.getMessage()
            );
        }
    }

    @Transactional
    public EmailVerificationResponse resendOtp(String email) {
        // Find existing token or create new one
        VerificationToken token = tokenRepository.findByEmail(email)
                .orElseThrow(() -> new BooklyException(
                        ErrorCode.INVALID_CREDENTIALS,
                        "No pending registration found for this email"
                ));

        // Generate new OTP
        String otp = generateOtp();
        token.setToken(otp);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(10));
        token.setVerified(false); // Reset verification status
        tokenRepository.save(token);

        // Send new OTP
        emailService.sendOtp(email, otp);

        logger.info("OTP resent for: {}", email);

        return new EmailVerificationResponse(
                email,
                "Verification code resent to your email",
                token.getExpiryDate()
        );
    }

    private void createUserProfileInTenant(LibraryUser user, String schemaName) {
        try {
            // Set tenant context
            TenantContext.setTenantId(schemaName);

            // TODO: need to implement this
            // SQL operations to create user profile in tenant schema

            logger.info("Created user profile in tenant schema: {}", schemaName);
        } finally {
            // Always clear tenant context
            TenantContext.clear();
        }
    }

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}