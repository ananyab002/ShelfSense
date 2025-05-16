package com.shelf_sense_backend.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shelf_sense_backend.model.UserEntity;
import com.shelf_sense_backend.model.LoginRequest;
import com.shelf_sense_backend.repo.UserRepository;
import com.shelf_sense_backend.security.JwtUtil;
import com.shelf_sense_backend.service.UserService;

@RestController
@RequestMapping("auth")
@CrossOrigin(origins = "http://localhost:5173") // Allow requests from React
public class UserController {

    @Autowired
    private UserService userservice;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<Object> registerUser(@RequestBody UserEntity userData) {
        UserEntity data = userservice.registerUser(userData);
        Map<String, String> message = new HashMap<>();
        message.put("message", "User already exists. Please login!");
        if (data != null) {
            return new ResponseEntity<>(data, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(message, HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Object> loginUser(@RequestBody LoginRequest loginData) {

        String email = loginData.getEmail();
        String password = loginData.getPassword();

        Optional<UserEntity> optionalUser = userRepository.findByEmail(email);
        Map<String, String> message = new HashMap<>();
        if (optionalUser.isEmpty()) {
            message.put("message", "User not found. Please check your email or register");
            return new ResponseEntity<>(message, HttpStatus.NOT_FOUND);
        }

        UserEntity userData = optionalUser.get();
        if (!(passwordEncoder.matches(password, userData.getPassword()))) {

            message.put("message", "Incorrect password. Please try again.");
            return new ResponseEntity<>(message, HttpStatus.UNAUTHORIZED);
        }

        String newToken = jwtUtil.generateToken(email);
        Map<String, Object> token = new HashMap<>();
        token.put("token", newToken);
        token.put("userData", userData);
        return ResponseEntity.ok(token);

    }

    /*
     * @DeleteMapping("/{id}")
     * public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
     * try {
     * userservice.deleteUserById(id);
     * return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204 - No Content
     * } catch (Exception e) {
     * return new ResponseEntity<>(HttpStatus.NOT_FOUND); // 404 - Not Found
     * }
     * }
     */
}