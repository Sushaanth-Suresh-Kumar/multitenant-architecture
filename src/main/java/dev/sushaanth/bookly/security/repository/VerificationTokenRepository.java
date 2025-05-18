package dev.sushaanth.bookly.security.repository;

import dev.sushaanth.bookly.security.model.VerificationToken;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    Optional<VerificationToken> findByEmailAndToken(String email, String token);
    Optional<VerificationToken> findByEmail(String email);
    @Modifying
    @Transactional
    int deleteAllByExpiryDateBefore(LocalDateTime dateTime);
}