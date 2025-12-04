package uk.gov.moj.cp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.moj.cp.dto.UpdateUserDto;
import uk.gov.moj.cp.dto.UserCreationResponseDto;
import uk.gov.moj.cp.dto.UserDto;
import uk.gov.moj.cp.dto.UserResponseDto;
import uk.gov.moj.cp.entity.User;
import uk.gov.moj.cp.model.UserCreationStatus;
import uk.gov.moj.cp.model.UserRole;
import uk.gov.moj.cp.model.UserStatus;
import uk.gov.moj.cp.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Optional<User> findByEmailLookup(final String email) {
        try {
            String trimmedEmail = email.toLowerCase().trim();
            return userRepository.findByEmailLookup(trimmedEmail);
        } catch (Exception e) {
            log.error("Error while loading user by email lookup", e);
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public Optional<User> findActiveUserByEmail(final String email) {
        try {
            String trimmedEmail = email.toLowerCase().trim();
            Optional<User> userOptional = userRepository.findByEmailLookupAndStatus(trimmedEmail, UserStatus.ACTIVE);
            if (userOptional.isPresent()) {
                return userOptional;
            }
        } catch (Exception e) {
            log.error("Error while loading active user by email lookup", e);
        }
        return Optional.empty();
    }

    @Transactional
    public List<UserCreationResponseDto> addUsers(final List<UserDto> userDtos) {
        return userDtos.stream().map(this::validateAndCreateUser).toList();
    }

    private UserCreationResponseDto validateAndCreateUser(UserDto userDto) {
        try {
            validateEmail(userDto.getEmail());
            User user = new User(userDto.getEmail().toLowerCase().trim());
            Optional<User> emailOptional = findByEmailLookup(user.getEmail());
            if (emailOptional.isEmpty()) {
                User savedUser = userRepository.save(user);
                return UserCreationResponseDto.builder()
                    .email(savedUser.getEmail())
                    .status(UserCreationStatus.CREATED)
                    .build();
            } else {
                return UserCreationResponseDto.builder()
                    .email(user.getEmail())
                    .status(UserCreationStatus.FAILED)
                    .reason("Email already exists")
                    .build();
            }
        } catch (IllegalArgumentException e) {
            return UserCreationResponseDto.builder()
                .email(userDto.getEmail())
                .status(UserCreationStatus.FAILED)
                .reason("User email validation failed")
                .build();
        } catch (Exception e) {
            return UserCreationResponseDto.builder()
                .email(userDto.getEmail())
                .status(UserCreationStatus.FAILED)
                .reason("Invalid user data")
                .build();
        }
    }

    private static void validateEmail(String email) {
        if (StringUtils.isBlank(email) || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email: " + email);
        }
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUser(final String email) {
        try {
            validateEmail(email);
            Optional<User> userOptional = findByEmailLookup(email);
            if (userOptional.isPresent()) {
                return getUserResponseDto(userOptional.get());
            }
        } catch (Exception e) {
            log.error("Error while loading user by email", e);
        }
        return null;
    }

    @Transactional
    public UserResponseDto updateUser(final UpdateUserDto updateUserDto) {
        try {
            final String email = updateUserDto.getEmail();
            validateEmail(email);
            Optional<User> userOptional = findByEmailLookup(email);
            if (userOptional.isPresent()) {
                User originalUser = userOptional.get();
                UserRole role = updateUserDto.getRole();
                if (role != null) {
                    originalUser.setRole(role);
                }
                UserStatus status = updateUserDto.getStatus();
                if (status != null) {
                    originalUser.setStatus(status);
                }
                User updatedUser = userRepository.save(originalUser);
                return getUserResponseDto(updatedUser);
            }
        } catch (Exception e) {
            log.error("Error while loading user by email", e);
        }
        return null;
    }

    @Transactional
    public UserResponseDto deleteUser(final UserDto userDto) {
        try {
            validateEmail(userDto.getEmail());
            Optional<User> userOptional = findByEmailLookup(userDto.getEmail());
            if (userOptional.isPresent()) {
                User originalUser = userOptional.get();
                originalUser.setStatus(UserStatus.DELETED);
                User updatedUser = userRepository.save(originalUser);
                return getUserResponseDto(updatedUser);
            }
        } catch (Exception e) {
            log.error("Error while loading user by email", e);
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        List<User> allUser = userRepository.findAll();
        return allUser.stream()
            .map(UserService::getUserResponseDto)
            .toList();
    }

    private static UserResponseDto getUserResponseDto(User user) {
        return UserResponseDto.builder()
            .email(user.getEmail())
            .role(user.getRole())
            .status(user.getStatus())
            .updated(user.getUpdated())
            .build();
    }
}
