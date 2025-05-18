package dev.sushaanth.bookly.security.repository;

import dev.sushaanth.bookly.security.model.EmployeeInvitation;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeInvitationRepository extends JpaRepository<EmployeeInvitation, UUID> {
    Optional<EmployeeInvitation> findByEmailAndUsedFalse(String email);
    @Modifying
    @Transactional
    int deleteAllByExpiresAtBeforeAndUsedFalse(LocalDateTime dateTime);
    List<EmployeeInvitation> findByTenantIdAndUsedFalse(UUID tenantId);
}