package dev.sushaanth.bookly.security.repository;

import dev.sushaanth.bookly.security.model.LibraryUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LibraryUserRepository extends JpaRepository<LibraryUser, UUID> {
    Optional<LibraryUser> findByUsername(String username);
    Optional<LibraryUser> findByEmail(String email);
    boolean existsByUsernameAndTenantId(String username, UUID tenantId);
}
