package uk.gov.moj.cp.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import uk.gov.moj.cp.model.ActiveStatus;
import uk.gov.moj.cp.model.Roles;
import uk.gov.moj.cp.model.User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.flyway.enabled=false"
})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should save and fetch user by email")
    void shouldFindUserByEmail() {
        final User user = new User();
        user.setEmail("test@example.com");
        user.setActive(Boolean.valueOf(ActiveStatus.TRUE.name()));
        user.setRole(Roles.ADMIN.name());

        userRepository.saveAndFlush(user);
        entityManager.clear();

        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getRole()).isEqualTo("ADMIN");
    }




    @Test
    @DisplayName("Should return empty when user is not found by email")
    void shouldReturnEmptyWhenUserNotFoundByEmail() {
        Optional<User> retrievedUser = userRepository.findByEmail("nonexistent@example.com");

        assertThat(retrievedUser).isEmpty();
    }
}
