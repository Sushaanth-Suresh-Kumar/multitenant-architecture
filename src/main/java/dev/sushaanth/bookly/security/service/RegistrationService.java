package dev.sushaanth.bookly.security.service;

import dev.sushaanth.bookly.exception.BooklyException;
import dev.sushaanth.bookly.exception.BooklyException.ErrorCode;
import dev.sushaanth.bookly.multitenancy.context.TenantContext;
import dev.sushaanth.bookly.security.dto.RegistrationRequest;
import dev.sushaanth.bookly.security.dto.VerificationRequest;
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
    public void initiateLibraryRegistration(RegistrationRequest request) {
        // Validate request
        if (request.libraryName() == null || request.libraryName().isEmpty()) {
            throw new BooklyException(ErrorCode.INVALID_CREDENTIALS, "Library name is required");
        }

        // Check for existing email
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BooklyException(ErrorCode.INVALID_CREDENTIALS, "Email already registered");
        }

        // Check for existing library name
        if (tenantRepository.findByDisplayName(request.libraryName()).isPresent()) {
            throw new BooklyException(ErrorCode.TENANT_ALREADY_EXISTS, "Library name already taken");
        }

        // Generate OTP
        String otp = generateOtp();

        // Create verification token
        VerificationToken token = new VerificationToken();
        token.setEmail(request.email());
        token.setToken(otp);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(10));
        token.setRegistrationType("LIBRARY");
        token.setRegistrationData(request);

        tokenRepository.save(token);

        // Send OTP email
        emailService.sendOtp(request.email(), otp);

        logger.info("Library registration initiated for: {}", request.email());
    }

    @Transactional
    public void initiateEmployeeRegistration(RegistrationRequest request) {
        // Validate email is not already registered
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BooklyException(ErrorCode.INVALID_CREDENTIALS, "Email already registered");
        }

        // Check for valid invitation
        EmployeeInvitation invitation = invitationRepository.findByEmailAndUsedFalse(request.email())
                .orElseThrow(() -> new BooklyException(
                        ErrorCode.INVALID_CREDENTIALS,
                        "No valid invitation found for this email"
                ));

        // Check if invitation expired
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BooklyException(
                    ErrorCode.EXPIRED_INVITATION,
                    "Invitation has expired"
            );
        }

        // Generate OTP
        String otp = generateOtp();

        // Create verification token
        VerificationToken token = new VerificationToken();
        token.setEmail(request.email());
        token.setToken(otp);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(10));
        token.setRegistrationType("EMPLOYEE");
        token.setTenantId(invitation.getTenantId());
        token.setRegistrationData(request);

        tokenRepository.save(token);

        // Send OTP email
        emailService.sendOtp(request.email(), otp);

        logger.info("Employee registration initiated for: {}", request.email());
    }

    @Transactional
    public void verifyAndCompleteRegistration(VerificationRequest request) {
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

        // Complete registration based on type
        if ("LIBRARY".equals(token.getRegistrationType())) {
            completeLibraryRegistration(token);
        } else if ("EMPLOYEE".equals(token.getRegistrationType())) {
            completeEmployeeRegistration(token);
        } else {
            throw new BooklyException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Unknown registration type: " + token.getRegistrationType()
            );
        }

        logger.info("Registration completed for: {}", request.email());
    }

    @Transactional
    public void resendOtp(String email) {
        // Find existing token
        VerificationToken token = tokenRepository.findByEmail(email)
                .orElseThrow(() -> new BooklyException(
                        ErrorCode.INVALID_CREDENTIALS,
                        "No pending registration found for this email"
                ));

        // Generate new OTP
        String otp = generateOtp();
        token.setToken(otp);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(10));
        tokenRepository.save(token);

        // Send new OTP
        emailService.sendOtp(email, otp);

        logger.info("OTP resent for: {}", email);
    }

    private void completeLibraryRegistration(VerificationToken token) {
        RegistrationRequest request = token.getRegistrationData();

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
        } catch (Exception e) {
            logger.error("Failed to complete library registration", e);
            throw new BooklyException(
                    ErrorCode.TENANT_CREATION_FAILED,
                    "Failed to complete registration: " + e.getMessage()
            );
        }
    }

    private void completeEmployeeRegistration(VerificationToken token) {
        RegistrationRequest request = token.getRegistrationData();

        try {
            // Find invitation
            EmployeeInvitation invitation = invitationRepository.findByEmailAndUsedFalse(request.email())
                    .orElseThrow(() -> new BooklyException(
                            ErrorCode.INVALID_CREDENTIALS,
                            "No valid invitation found"
                    ));

            // Verify invitation hasn't expired
            if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new BooklyException(
                        ErrorCode.EXPIRED_INVITATION,
                        "Invitation has expired"
                );
            }

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

    private void createUserProfileInTenant(LibraryUser user, String schemaName) {
        try {
            // Set tenant context
            TenantContext.setTenantId(schemaName);

            // TODO: need to implement this
            // SQL operations to create user profile in tenant schema
            // This would typically be done through a repository or JDBC template
            // For example, using a TenantAwareUserRepository to save a UserProfile entity

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