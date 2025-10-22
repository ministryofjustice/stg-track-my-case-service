package uk.gov.moj.cp.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import uk.gov.moj.cp.entity.User;
import uk.gov.moj.cp.model.UserRole;
import uk.gov.moj.cp.model.UserStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.moj.cp.util.CryptoUtils.ENCRYPTION_PREFIX;

@DataJpaTest
@Import(TestCryptoConfig.class)
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    TestEntityManager testEntityManager;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("alice@example.com");
        user.setRole(UserRole.ADMIN);
        user.setStatus(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should save user successfully")
    void testSaveUser() {
        userRepository.save(user);
        User saved = userRepository.findByEmailLookup(user.getEmail()).get();

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getEmailLookup()).isNull();
        assertThat(saved.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getCreated()).isNotNull();
        assertThat(saved.getUpdated()).isNotNull();

        Object[] raw = (Object[]) testEntityManager.getEntityManager()
            .createNativeQuery("select email, email_lookup from tmc_user where id = :id")
            .setParameter("id", saved.getId())
            .getSingleResult();

        assertThat(raw).isNotNull();
        String email = (String) raw[0];
        String emailLookup = (String) raw[1];
        assertThat(email).startsWith(ENCRYPTION_PREFIX);
        assertThat(email).doesNotContain("alice@example.com");

        assertThat(emailLookup).isNotEmpty();
        assertThat(emailLookup).doesNotContain("alice@example.com");
    }

    @Test
    @DisplayName("Should fetch user by email successfully")
    void testFindByEmailIgnoreCaseSuccess() {
        entityManager.persistAndFlush(user);

        Optional<User> found = userRepository.findByEmailLookup("alice@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alice@example.com");
        assertThat(found.get().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(found.get().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should return empty when user not found by email")
    void testFindByEmailIgnoreCaseNotFound() {
        Optional<User> found = userRepository.findByEmailLookup("bob@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find all users")
    void testFindAllUsers() {
        User user1 = new User("user1@example.com");
        user1.setRole(UserRole.USER);
        user1.setStatus(UserStatus.ACTIVE);

        User user2 = new User("user2@example.com");
        user2.setRole(UserRole.ADMIN);
        user2.setStatus(UserStatus.ACTIVE);

        entityManager.persistAndFlush(user1);
        entityManager.persistAndFlush(user2);

        List<User> allUsers = userRepository.findAll();

        assertThat(allUsers).hasSize(2);
        assertThat(allUsers).extracting(User::getEmail)
                .containsExactlyInAnyOrder("user1@example.com", "user2@example.com");
    }

    @Test
    @DisplayName("Should find user by ID")
    void testFindById() {
        User saved = userRepository.save(user);

        Optional<User> found = userRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alice@example.com");
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("Should delete user by ID")
    void testDeleteById() {
        User saved = userRepository.save(user);

        userRepository.deleteById(saved.getId());

        Optional<User> found = userRepository.findById(saved.getId());
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should update user")
    void testUpdateUser() {
        User saved = userRepository.save(user);
        saved.setRole(UserRole.USER);
        saved.setStatus(UserStatus.DELETED);

        userRepository.save(saved);
        User updated = userRepository.findByEmailLookup(saved.getEmail()).get();

        assertThat(updated.getRole()).isEqualTo(UserRole.USER);
        assertThat(updated.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.getUpdated()).isAfter(saved.getCreated());
    }

    @Test
    @DisplayName("Should not handle case insensitive email lookup")
    void testFindByEmailIgnoreCaseCaseInsensitive() {
        entityManager.persistAndFlush(user);

        Optional<User> found = userRepository.findByEmailLookup("ALICE@EXAMPLE.COM");

        assertThat(found).isEmpty();
    }
}
