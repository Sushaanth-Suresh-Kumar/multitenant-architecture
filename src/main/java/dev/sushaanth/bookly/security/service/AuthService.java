package dev.sushaanth.bookly.security.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sushaanth.bookly.security.JwtTokenUtil;
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
import dev.sushaanth.bookly.tenant.TenantService;
import dev.sushaanth.bookly.tenant.dto.TenantCreateRequest;
import dev.sushaanth.bookly.tenant.dto.TenantResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final LibraryUserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final EmployeeInvitationRepository employeeInvitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TenantService tenantService;
    private final JwtTokenUtil jwtTokenUtil;
    private final ObjectMapper objectMapper;

    public AuthService(AuthenticationManager authenticationManager,
                       LibraryUserRepository userRepository,
                       VerificationTokenRepository tokenRepository,
                       EmployeeInvitationRepository employeeInvitationRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       TenantService tenantService,
                       JwtTokenUtil jwtTokenUtil,
                       ObjectMapper objectMapper) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.employeeInvitationRepository = employeeInvitationRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.tenantService = tenantService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.objectMapper = objectMapper;
    }

    /**
     * Authenticate user and generate JWT token
     */
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.username(),
                        loginRequest.password()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Get the user to retrieve tenant information
        LibraryUser user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String jwt = jwtTokenUtil.generateToken(
                user.getUsername(),
                user.getTenantId(),
                user.getRole()
        );

        return new JwtResponse(jwt, user.getUsername(), user.getRole().name());
    }

    /**
     * Start library registration process
     */
    @Transactional
    public void initiateLibraryRegistration(RegistrationRequest request) {
        // Check if email already exists
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email already in use");
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
     * Start employee registration process
     */
    @Transactional
    public void initiateEmployeeRegistration(RegistrationRequest request) {
        // Check if email already exists
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email already in use");
        }

        // Check if the email has a valid invitation
        employeeInvitationRepository.findByEmailAndUsedFalse(request.email())
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
     * Resend OTP
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
     * Verify OTP and complete registration
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
            }

            logger.info("Registration completed for email: {}", request.email());
        } catch (Exception e) {
            logger.error("Failed to complete registration", e);
            throw new RuntimeException("Failed to complete registration", e);
        }
    }

    /**
     * Complete library registration after verification
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
    }

    /**
     * Complete employee registration after verification
     */
    private void completeEmployeeRegistration(VerificationToken token) throws Exception {
        // Deserialize registration data
        RegistrationRequest request = objectMapper.readValue(
                token.getRegistrationData(), RegistrationRequest.class);

        // Find the employee invitation
        EmployeeInvitation invitation = employeeInvitationRepository
                .findByEmailAndUsedFalse(request.email())
                .orElseThrow(() -> new RuntimeException("No valid invitation found for this email"));

        // Create employee user
        LibraryUser user = new LibraryUser();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.ROLE_EMPLOYEE);

        // Set tenant ID from the invitation
        user.setTenantId(invitation.getTenantId());

        // Mark invitation as used
        invitation.setUsed(true);
        employeeInvitationRepository.save(invitation);

        // Save the new employee user
        userRepository.save(user);

        logger.info("Employee registration completed for email: {}", request.email());
    }

    /**
     * Create employee invitation (called by library admin)
     */
    @Transactional
    public void createEmployeeInvitation(String email, UUID libraryAdminId) {
        // Check if email already has an invitation
        if (employeeInvitationRepository.findByEmailAndUsedFalse(email).isPresent()) {
            throw new RuntimeException("Invitation already exists for this email");
        }

        // Get admin's tenant
        LibraryUser admin = userRepository.findById(libraryAdminId)
                .orElseThrow(() -> new RuntimeException("Admin user not found"));

        if (!admin.getRole().equals(Role.ROLE_LIBRARY_ADMIN)) {
            throw new RuntimeException("Only library admins can create invitations");
        }

        // Create invitation
        EmployeeInvitation invitation = new EmployeeInvitation();
        invitation.setEmail(email);
        invitation.setTenantId(admin.getTenantId());
        invitation.setInvitedBy(libraryAdminId);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));

        employeeInvitationRepository.save(invitation);
        logger.info("Employee invitation created for email: {}", email);
    }

    /**
     * Generate 6-digit OTP
     */
    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}