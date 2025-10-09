package uk.gov.moj.cp.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.moj.cp.dto.ErrorResponseDto;
import uk.gov.moj.cp.dto.UpdateUserDto;
import uk.gov.moj.cp.dto.UserCreationResponseDto;
import uk.gov.moj.cp.dto.UserDto;
import uk.gov.moj.cp.dto.UserResponseDto;
import uk.gov.moj.cp.service.UserService;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/users")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/user")
    public ResponseEntity<UserCreationResponseDto> createUser(@RequestBody final UserDto user) {
        return ResponseEntity.ok(userService.createUser(user));
    }

    @PostMapping("/users")
    public ResponseEntity<List<UserCreationResponseDto>> createUsers(@RequestBody final List<UserDto> users) {
        List<UserCreationResponseDto> responses = userService.addUsers(users);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/user")
    public ResponseEntity<Object> getUserByEmail(@RequestParam(name = "email", required = false) final String email) {
        if (email == null) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDto("Should provide a valid email using query param /user?email=name@example.com"));
        }
        final String decodedEmail = URLDecoder.decode(email, StandardCharsets.UTF_8);
        UserResponseDto userResponseDto = userService.getUser(decodedEmail);
        if (userResponseDto != null) {
            return ResponseEntity.ok(userResponseDto);
        }
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponseDto("User not found by email: " + decodedEmail));
    }

    @PutMapping("/user")
    public ResponseEntity<Object> updateUser(@RequestBody final UpdateUserDto user) {
        UserResponseDto userResponseDto = userService.updateUser(user);
        if (userResponseDto != null) {
            return ResponseEntity.ok(userResponseDto);
        }
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponseDto("User not found by email: " + user.getEmail()));
    }

    @DeleteMapping("/user")
    public ResponseEntity<Object> deleteUser(@RequestBody final UserDto user) {
        UserResponseDto userResponseDto = userService.deleteUser(user);
        if (userResponseDto != null) {
            return ResponseEntity.ok(userResponseDto);
        }
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponseDto("User not found by email: " + user.getEmail()));
    }
}
