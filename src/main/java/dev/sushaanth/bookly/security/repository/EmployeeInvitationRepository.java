package dev.sushaanth.bookly.security.repository;

import dev.sushaanth.bookly.security.model.EmployeeInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeInvitationRepository extends JpaRepository<EmployeeInvitation, UUID> {
    Optional<EmployeeInvitation> findByEmailAndUsedFalse(String email);
}