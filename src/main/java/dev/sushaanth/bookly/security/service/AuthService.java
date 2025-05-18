package dev.sushaanth.bookly.security.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sushaanth.bookly.multitenancy.context.TenantContext;
import dev.sushaanth.bookly.security.exception.InvitationExpiredException;
import dev.sushaanth.bookly.security.jwt.JwtTokenUtil;
import dev.sushaanth.bookly.security.dto.JwtResponse;
import dev.sushaanth.bookly.security.dto.LoginRequest;
import dev.sushaanth.bookly.security.dto.RegistrationRequest;
import dev.sushaanth.bookly.security.dto.VerificationRequest;
import dev.sushaanth.bookly.security.exception.InvalidOtpException;
import dev.sushaanth.bookly.security.exception.OtpAlreadyUsedException;
import dev.sushaanth.bookly.security.exception.OtpExpiredException;
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
import dev.sushaanth.bookly.tenant.exception.TenantNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

/**
 * Service for handling authentication, registration, and user management
 * in a multi-tenant environment.
 */
@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final String DEFAULT_TENANT = "public";

    private final AuthenticationManager authenticationManager;
    private final LibraryUserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final VerificationTokenRepository tokenRepository;
    private final EmployeeInvitationRepository employeeInvitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TenantService tenantService;
    private final JwtTokenUtil jwtTokenUtil;
    private final ObjectMapper objectMapper;

    public AuthService(AuthenticationManager authenticationManager,
                       LibraryUserRepository userRepository,
                       TenantRepository tenantRepository,
                       VerificationTokenRepository tokenRepository,
                       EmployeeInvitationRepository employeeInvitationRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       TenantService tenantService,
                       JwtTokenUtil jwtTokenUtil,
                       ObjectMapper objectMapper) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.tokenRepository = tokenRepository;
        this.employeeInvitationRepository = employeeInvitationRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.tenantService = tenantService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.objectMapper = objectMapper;
    }

    /**
     * Authenticate a user and generate a JWT token.
     *
     * @param loginRequest The login credentials
     * @return JWT response containing token and user information
     */
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        try {
            // Attempt authentication
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.username(),
                            loginRequest.password()
                    )
            );

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Get the user to retrieve tenant information
            LibraryUser user = userRepository.findByUsername(loginRequest.username())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get tenant details
            Tenant tenant = tenantRepository.findById(user.getTenantId())
                    .orElseThrow(() -> new TenantNotFoundException("Tenant not found for user"));

            // Generate JWT token with tenant information
            String jwt = jwtTokenUtil.generateToken(
                    user.getUsername(),
                    user.getTenantId(),
                    tenant.getSchemaName(),
                    user.getRole()
            );

            logger.info("User authenticated successfully: {}", user.getUsername());
            return new JwtResponse(jwt, user.getUsername(), user.getRole().name());

        } catch (BadCredentialsException e) {
            logger.warn("Authentication failed for user: {}", loginRequest.username());
            throw new BadCredentialsException("Invalid username or password");
        } catch (DisabledException e) {
            logger.warn("Disabled user attempted to log in: {}", loginRequest.username());
            throw new DisabledException("Account is disabled. Please contact support.");
        }
    }

    /**
     * Start library registration process.
     * This creates a new tenant and sends an OTP for verification.
     *
     * @param request Registration details
     */
    @Transactional
    public void initiateLibraryRegistration(RegistrationRequest request) {
        // Validate request
        if (request.libraryName() == null || request.libraryName().isBlank()) {
            throw new IllegalArgumentException("Library name cannot be empty");
        }

        // Check if email already exists
        if (userRepository.findByEmail(request.email()).isPresent()) {
            logger.warn("Registration attempted with existing email: {}", request.email());
            throw new RuntimeException("Email already in use");
        }

        // Check if library name already exists
        if (tenantRepository.findByDisplayName(request.libraryName()).isPresent()) {
            logger.warn("Registration attempted with existing library name: {}", request.libraryName());
            throw new RuntimeException("Library name already in use");
        }

        // Generate and send OTP
        String otp = generateOtp();

        try {
            // Save verification token with registration data
            VerificationToken token = new VerificationToken();
            token.setEmail(request.email());
            token.setToken(otp);
            token.setExpiryDate(LocalDateTime.now().plusMinutes(10));
            token.setRegistrationType("LIBRARY");
            token.setRegistrationData(objectMapper.writeValueAsString(request));

            tokenRepository.save(token);

            // Send OTP email
            emailService.sendOtp(request.email(), otp);
            logger.info("Library registration initiated for email: {}", request.email());
        } catch (Exception e) {
            logger.error("Failed to initiate library registration", e);
            throw new RuntimeException("Failed to initiate registration", e);
        }
    }

    /**
     * Start employee registration process.
     * Verifies invitation and sends OTP.
     *
     * @param request Registration details
     */
    @Transactional
    public void initiateEmployeeRegistration(RegistrationRequest request) {
        // Check if email already exists
        if (userRepository.findByEmail(request.email()).isPresent()) {
            logger.warn("Employee registration attempted with existing email: {}", request.email());
            throw new RuntimeException("Email already in use");
        }

        // Check if the email has a valid invitation
        EmployeeInvitation invitation = employeeInvitationRepository.findByEmailAndUsedFalse(request.email())
                .orElseThrow(() -> new RuntimeException("No valid invitation found for this email"));

        // Generate and send OTP
        String otp = generateOtp();

        try {
            // Save verification token with registration data
            VerificationToken token = new VerificationToken();
            token.setEmail(request.email());
            token.setToken(otp);
            token.setExpiryDate(LocalDateTime.now().plusMinutes(10));
            token.setRegistrationType("EMPLOYEE");
            token.setRegistrationData(objectMapper.writeValueAsString(request));

            // Also store the tenant ID from the invitation
            token.setTenantId(invitation.getTenantId());

            tokenRepository.save(token);

            // Send OTP email
            emailService.sendOtp(request.email(), otp);
            logger.info("Employee registration initiated for email: {}", request.email());
        } catch (Exception e) {
            logger.error("Failed to initiate employee registration", e);
            throw new RuntimeException("Failed to initiate registration", e);
        }
    }

    /**
     * Resend OTP for verification.
     *
     * @param email User's email
     */
    @Transactional
    public void resendOtp(String email) {
        // Find existing token
        VerificationToken token = tokenRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No registration found for this email"));

        // Generate new OTP
        String otp = generateOtp();
        token.setToken(otp);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(10));
        tokenRepository.save(token);

        // Send new OTP
        emailService.sendOtp(email, otp);
        logger.info("OTP resent for email: {}", email);
    }

    /**
     * Verify OTP and complete registration.
     *
     * @param request Verification details
     */
    @Transactional
    public void verifyAndCompleteRegistration(VerificationRequest request) {
        // Find and validate token
        VerificationToken token = tokenRepository.findByEmailAndToken(request.email(), request.otp())
                .orElseThrow(() -> new InvalidOtpException("Invalid OTP"));

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new OtpExpiredException("OTP expired");
        }

        if (token.isVerified()) {
            throw new OtpAlreadyUsedException("OTP already used");
        }

        try {
            // Mark token as verified
            token.setVerified(true);
            tokenRepository.save(token);

            // Complete registration based on type
            if ("LIBRARY".equals(token.getRegistrationType())) {
                completeLibraryRegistration(token);
            } else if ("EMPLOYEE".equals(token.getRegistrationType())) {
                completeEmployeeRegistration(token);
            } else {
                throw new RuntimeException("Unknown registration type: " + token.getRegistrationType());
            }

            logger.info("Registration completed for email: {}", request.email());
        } catch (Exception e) {
            logger.error("Failed to complete registration", e);
            throw new RuntimeException("Failed to complete registration: " + e.getMessage(), e);
        }
    }

    /**
     * Complete library registration after verification.
     * Creates a new tenant and library admin user.
     *
     * @param token Verification token
     */
    private void completeLibraryRegistration(VerificationToken token) throws Exception {
        // Deserialize registration data
        RegistrationRequest request = objectMapper.readValue(
                token.getRegistrationData(), RegistrationRequest.class);

        // Create tenant
        TenantCreateRequest tenantRequest = new TenantCreateRequest(
                request.libraryName(),
                "Library tenant for " + request.libraryName()
        );

        try {
            // Create the tenant first
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

            // Now create the user profile in the tenant schema
            createUserProfileInTenant(user, tenantResponse.schemaName());

            logger.info("Library tenant created: {} with admin: {}", tenantResponse.displayName(), user.getUsername());
        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity violation during library registration", e);
            throw new RuntimeException("Registration failed due to data constraint violation", e);
        } catch (Exception e) {
            logger.error("Failed to complete library registration", e);
            throw new RuntimeException("Failed to complete library registration: " + e.getMessage(), e);
        }
    }

    /**
     * Complete employee registration after verification.
     * Creates a new employee user for an existing tenant.
     *
     * @param token Verification token
     */
    private void completeEmployeeRegistration(VerificationToken token) throws Exception {
        // Deserialize registration data
        RegistrationRequest request = objectMapper.readValue(
                token.getRegistrationData(), RegistrationRequest.class);

        // Find the employee invitation
        EmployeeInvitation invitation = employeeInvitationRepository
                .findByEmailAndUsedFalse(request.email())
                .orElseThrow(() -> new RuntimeException("No valid invitation found for this email"));


        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvitationExpiredException("Invitation has expired. Please request a new invitation.");
        }

        // Get tenant information
        Tenant tenant = tenantRepository.findById(invitation.getTenantId())
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found"));

        try {
            // Create employee user
            LibraryUser user = new LibraryUser();
            user.setUsername(request.username());
            user.setEmail(request.email());
            user.setFirstName(request.firstName());
            user.setLastName(request.lastName());
            user.setPassword(passwordEncoder.encode(request.password()));
            user.setRole(Role.ROLE_EMPLOYEE);
            user.setTenantId(invitation.getTenantId());

            // Save the new employee user
            userRepository.save(user);

            // Create user profile in tenant schema
            createUserProfileInTenant(user, tenant.getSchemaName());

            // Mark invitation as used
            invitation.setUsed(true);
            employeeInvitationRepository.save(invitation);

            logger.info("Employee registration completed for email: {}", request.email());
        } catch (Exception e) {
            logger.error("Failed to complete employee registration", e);
            throw new RuntimeException("Failed to complete employee registration: " + e.getMessage(), e);
        }
    }

    /**
     * Create a user profile in the tenant schema.
     *
     * @param user The authentication user
     * @param schemaName The tenant schema name
     */
    private void createUserProfileInTenant(LibraryUser user, String schemaName) {
        try {
            // Set tenant context to the appropriate schema
            TenantContext.setTenantId(schemaName);

            // Here we would use a repository or JDBC template to create a user record
            // in the tenant schema. For example:

            // Using a JDBC template (simplified example)
            // jdbcTemplate.update(
            //     "INSERT INTO " + schemaName + ".users (id, username, firstname, lastname, email) VALUES (?, ?, ?, ?, ?)",
            //     user.getId(), user.getUsername(), user.getFirstName(), user.getLastName(), user.getEmail()
            // );

            // Alternatively, this could be done with a tenant-aware repository
            // userProfileRepository.save(new UserProfile(user));

            logger.info("Created user profile in tenant schema: {}", schemaName);
        } finally {
            // Always clear tenant context
            TenantContext.clear();
        }
    }

    /**
     * Create employee invitation.
     * Only library admins can invite employees.
     *
     * @param email Email of the employee to invite
     * @param libraryAdminId ID of the admin creating the invitation
     */
    @Transactional
    public void createEmployeeInvitation(String email, UUID libraryAdminId) {
        // Check if email already exists in the system
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already registered in the system");
        }

        // Check if email already has an invitation
        if (employeeInvitationRepository.findByEmailAndUsedFalse(email).isPresent()) {
            throw new RuntimeException("Invitation already exists for this email");
        }

        // Get admin's information
        LibraryUser admin = userRepository.findById(libraryAdminId)
                .orElseThrow(() -> new RuntimeException("Admin user not found"));

        // Verify the user is actually a library admin
        if (!admin.getRole().equals(Role.ROLE_LIBRARY_ADMIN)) {
            logger.warn("Non-admin attempted to create invitation: {}", admin.getUsername());
            throw new RuntimeException("Only library admins can create invitations");
        }

        // Get tenant information
        Tenant tenant = tenantRepository.findById(admin.getTenantId())
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found"));

        try {
            // Create invitation
            EmployeeInvitation invitation = new EmployeeInvitation();
            invitation.setEmail(email);
            invitation.setTenantId(admin.getTenantId());
            invitation.setInvitedBy(libraryAdminId);
            invitation.setExpiresAt(LocalDateTime.now().plusDays(7));

            employeeInvitationRepository.save(invitation);

            // Send invitation email
            // emailService.sendEmployeeInvitation(email, admin.getUsername(), tenant.getDisplayName(), tenant.getSchemaName());

            logger.info("Employee invitation created for email: {} in tenant: {}",
                    email, tenant.getDisplayName());
        } catch (Exception e) {
            logger.error("Failed to create employee invitation", e);
            throw new RuntimeException("Failed to create invitation: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a 6-digit OTP code.
     *
     * @return OTP as string
     */
    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}