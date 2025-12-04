package uk.gov.moj.cp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.moj.cp.entity.User;
import uk.gov.moj.cp.model.UserStatus;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailLookup(String emailLookup);

    Optional<User> findByEmailLookupAndStatus(String emailLookup, UserStatus status);
}
