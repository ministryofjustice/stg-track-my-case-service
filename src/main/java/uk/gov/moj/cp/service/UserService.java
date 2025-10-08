package uk.gov.moj.cp.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import uk.gov.moj.cp.dto.UserResponseDto;
import uk.gov.moj.cp.model.ActiveStatus;
import uk.gov.moj.cp.model.Roles;
import uk.gov.moj.cp.model.User;
import uk.gov.moj.cp.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(final User user) {
        validateUser(user);
        try {
            return userRepository.save(user);
        } catch (Exception exception) {
            log.error("Error creating user: {}", exception.getMessage());
            throw new RuntimeException("Error creating user", exception);
        }
    }

    private static void validateUser(final User user) {
        if (!EnumUtils.isValidEnumIgnoreCase(Roles.class, user.getRole())) {
            throw new IllegalArgumentException("Invalid role: " + user.getRole());
        }

        if (!EnumUtils.isValidEnumIgnoreCase(ActiveStatus.class, String.valueOf(user.isActive()).toUpperCase())) {
            throw new IllegalArgumentException("Invalid active status: " + user.isActive());
        }
    }

    public User updateUser(final UUID id, final User updatedUser) {
        validateUser(updatedUser);
        return userRepository.findById(id).map(user -> {
            user.setEmail(updatedUser.getEmail());
            user.setRole(updatedUser.getRole());
            user.setActive(updatedUser.isActive());
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void deleteUser(UUID id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setActive(Boolean.valueOf(ActiveStatus.FALSE.name()));
            userRepository.save(user);
        });
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUser(final String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    public List<UserResponseDto> addUsers(final List<User> users) {
        List<UserResponseDto> responseList = new ArrayList<>();

        for (User user : users) {
            try {
                validateUser(user);
                userRepository.save(user);
                responseList.add(new UserResponseDto(user.getEmail(), "CREATED"));
            } catch (DataIntegrityViolationException e) {
                responseList.add(new UserResponseDto(user.getEmail(), "FAILED", "Email already exists"));
            } catch (IllegalArgumentException e) {
                responseList.add(new UserResponseDto(user.getEmail(), "FAILED", e.getMessage()));
            } catch (Exception e) {
                responseList.add(new UserResponseDto(user.getEmail(), "FAILED", e.getMessage()));
            }
        }
        return responseList;
    }
}
