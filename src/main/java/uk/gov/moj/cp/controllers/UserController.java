package uk.gov.moj.cp.controllers;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.moj.cp.config.ApiPaths;
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
@RequestMapping(ApiPaths.PATH_API_USERS)
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<Object> getAllUsers(@RequestParam(name = "email", required = false) final String email) {
        if (StringUtils.isNotEmpty(email)) {
            final String decodedEmail = URLDecoder.decode(email, StandardCharsets.UTF_8).toLowerCase().trim();
            UserResponseDto userResponseDto = userService.getUser(decodedEmail);
            if (userResponseDto != null) {
                return ResponseEntity.ok(userResponseDto);
            }
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDto("User not found by email: " + decodedEmail));
        }
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/create")
    public ResponseEntity<List<UserCreationResponseDto>> createUsers(@RequestBody final List<UserDto> users) {
        List<UserCreationResponseDto> responses = userService.addUsers(users);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/edit")
    public ResponseEntity<Object> updateUser(@RequestBody final UpdateUserDto user) {
        UserResponseDto userResponseDto = userService.updateUser(user);
        if (userResponseDto != null) {
            return ResponseEntity.ok(userResponseDto);
        }
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponseDto("User not found by email: " + user.getEmail()));
    }

    @DeleteMapping("/delete")
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
