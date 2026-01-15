package uk.gov.moj.cp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import uk.gov.moj.cp.model.UserRole;
import uk.gov.moj.cp.model.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tmc_user")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, unique = true)
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    @Size(max = 200, message = "Email must be 200 characters max")
    @Convert(converter = AttributeAesEncryptor.class)
    private String email;

    @Column(name = "email_lookup", nullable = false, unique = true, updatable = false)
    @Size(max = 200, message = "Email lookup must be 200 characters max")
    @Convert(converter = AttributeHmacEncryptor.class)
    private String emailLookup;

    @Setter
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Setter
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime created;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updated;

    public User() {
    }

    public User(String email) {
        this.email = email;
        this.emailLookup = email;
        this.status = UserStatus.ACTIVE;
        this.role = UserRole.USER;
    }

    public String getEmailLookup() {
        return null;
    }
}
