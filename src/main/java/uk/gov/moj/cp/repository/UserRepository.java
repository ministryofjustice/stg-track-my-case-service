package uk.gov.moj.cp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.moj.cp.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}
