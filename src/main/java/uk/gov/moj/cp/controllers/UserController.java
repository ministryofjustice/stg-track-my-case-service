package uk.gov.moj.cp.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.moj.cp.dto.UserResponseDto;
import uk.gov.moj.cp.model.User;
import uk.gov.moj.cp.service.UserService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserService userService;

    public UserController(final UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/user")
    public ResponseEntity<User> createUser(@RequestBody final User user) {
        return ResponseEntity.ok(userService.createUser(user));
    }

    @PostMapping("/users")
    public ResponseEntity<List<UserResponseDto>> createUsers(@RequestBody final List<User> users) {
        List<UserResponseDto> responses = userService.addUsers(users);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/user/{email}")
    public ResponseEntity<User> getUser(@PathVariable final String email) {
        return ResponseEntity.ok(userService.getUser(email));
    }

    @PutMapping("/user")
    public ResponseEntity<User> updateUser(@RequestBody final User user) {
        if (!(user.getId().toString().isEmpty())) {
            return ResponseEntity.ok(userService.updateUser(user.getId(), user));
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable final UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
