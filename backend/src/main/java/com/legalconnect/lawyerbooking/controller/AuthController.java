package com.legalconnect.lawyerbooking.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;

import com.legalconnect.lawyerbooking.dto.LoginRequest;
import com.legalconnect.lawyerbooking.dto.LoginResponse;
import com.legalconnect.lawyerbooking.dto.RegistrationRequest;
import com.legalconnect.lawyerbooking.dto.RegistrationResponse;
import com.legalconnect.lawyerbooking.dto.LawyerRegistrationRequest;
import com.legalconnect.lawyerbooking.repository.UserRepository;
import com.legalconnect.lawyerbooking.repository.LawyerRepository;
import com.legalconnect.lawyerbooking.entity.User;
import com.legalconnect.lawyerbooking.entity.Lawyer;
import com.legalconnect.lawyerbooking.service.PasswordService;
import com.legalconnect.lawyerbooking.util.JwtUtil;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LawyerRepository lawyerRepository;
    
    @Autowired
    private PasswordService passwordService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/user/login")
    public ResponseEntity<LoginResponse> userLogin(@RequestBody LoginRequest request) {
        try {
            System.out.println("User login attempt - Username: " + request.getUsername());
            
            if (request.getUsername() == null || request.getPassword() == null) {
                System.out.println("Login failed: Username or password is null");
                return ResponseEntity.badRequest()
                    .body(new LoginResponse(false, "Username and password are required"));
            }

            // Find user by username
            Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
            
            if (userOpt.isEmpty()) {
                System.out.println("Login failed: User not found");
                return ResponseEntity.status(401)
                    .body(new LoginResponse(false, "Invalid username or password"));
            }
            
            User user = userOpt.get();
            
            // Verify password (supports both hashed and plain text for backward compatibility)
            boolean passwordValid;
            if (passwordService.isHashed(user.getPassword())) {
                // Password is hashed, use BCrypt verification
                passwordValid = passwordService.verifyPassword(request.getPassword(), user.getPassword());
            } else {
                // Password is plain text (legacy), compare directly
                passwordValid = user.getPassword().equals(request.getPassword());
                // If login successful with plain text, hash it for future use
                if (passwordValid) {
                    user.setPassword(passwordService.hashPassword(request.getPassword()));
                    userRepository.save(user);
                    System.out.println("Password hashed for user: " + user.getUsername());
                }
            }
            
            if (passwordValid) {
                // Generate JWT token
                String token = jwtUtil.generateToken(user.getId(), user.getUsername(), "user");
                
                LoginResponse response = new LoginResponse(true, "Login successful");
                response.setUserType("user");
                response.setUsername(user.getUsername());
                response.setFullName(user.getFullName());
                response.setId(user.getId());
                response.setToken(token);
                System.out.println("Login successful for user: " + user.getUsername());
                return ResponseEntity.ok(response);
            } else {
                System.out.println("Login failed: Invalid password");
                return ResponseEntity.status(401)
                    .body(new LoginResponse(false, "Invalid username or password"));
            }
        } catch (Exception e) {
            System.err.println("Error in user login: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(new LoginResponse(false, "Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/lawyer/login")
    public ResponseEntity<LoginResponse> lawyerLogin(@RequestBody LoginRequest request) {
        try {
            System.out.println("Lawyer login attempt - Username: " + request.getUsername());
            
            if (request.getUsername() == null || request.getPassword() == null) {
                System.out.println("Login failed: Username or password is null");
                return ResponseEntity.badRequest()
                    .body(new LoginResponse(false, "Username and password are required"));
            }

            // Find lawyer by username
            Optional<Lawyer> lawyerOpt = lawyerRepository.findByUsername(request.getUsername());
            
            if (lawyerOpt.isEmpty()) {
                System.out.println("Login failed: Lawyer not found");
                return ResponseEntity.status(401)
                    .body(new LoginResponse(false, "Invalid username or password"));
            }
            
            Lawyer lawyer = lawyerOpt.get();
            
            // Verify password (supports both hashed and plain text for backward compatibility)
            boolean passwordValid;
            if (passwordService.isHashed(lawyer.getPassword())) {
                // Password is hashed, use BCrypt verification
                passwordValid = passwordService.verifyPassword(request.getPassword(), lawyer.getPassword());
            } else {
                // Password is plain text (legacy), compare directly
                passwordValid = lawyer.getPassword().equals(request.getPassword());
                // If login successful with plain text, hash it for future use
                if (passwordValid) {
                    lawyer.setPassword(passwordService.hashPassword(request.getPassword()));
                    lawyerRepository.save(lawyer);
                    System.out.println("Password hashed for lawyer: " + lawyer.getUsername());
                }
            }
            
            if (passwordValid) {
                // Generate JWT token
                String token = jwtUtil.generateToken(lawyer.getId(), lawyer.getUsername(), "lawyer");
                
                LoginResponse response = new LoginResponse(true, "Login successful");
                response.setUserType("lawyer");
                response.setUsername(lawyer.getUsername());
                response.setFullName(lawyer.getFullName());
                response.setId(lawyer.getId());
                response.setToken(token);
                System.out.println("Login successful for lawyer: " + lawyer.getUsername());
                return ResponseEntity.ok(response);
            } else {
                System.out.println("Login failed: Invalid password");
                return ResponseEntity.status(401)
                    .body(new LoginResponse(false, "Invalid username or password"));
            }
        } catch (Exception e) {
            System.err.println("Error in lawyer login: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(new LoginResponse(false, "Internal server error: " + e.getMessage()));
        }
    }
    
    @PostMapping("/user/register")
    public ResponseEntity<RegistrationResponse> userRegister(@Valid @RequestBody RegistrationRequest request) {
        try {
            System.out.println("User registration attempt - Username: " + request.getUsername());
            
            // Validate password strength
            if (!passwordService.isPasswordStrong(request.getPassword())) {
                System.out.println("Registration failed: Password does not meet strength requirements");
                return ResponseEntity.badRequest()
                    .body(new RegistrationResponse(false, passwordService.getPasswordStrengthErrorMessage()));
            }
            
            // Check if username already exists
            if (userRepository.existsByUsername(request.getUsername())) {
                System.out.println("Registration failed: Username already exists");
                return ResponseEntity.status(409)
                    .body(new RegistrationResponse(false, "Username already exists. Please choose a different username."));
            }
            
            // Check if email already exists (if email is provided)
            if (request.getEmail() != null && !request.getEmail().isEmpty()) {
                Optional<User> existingUser = userRepository.findByUsername(request.getEmail());
                // Note: We'd need an email field in repository for proper check, but for now we'll proceed
            }
            
            // Create new user
            User newUser = new User();
            newUser.setUsername(request.getUsername());
            newUser.setPassword(passwordService.hashPassword(request.getPassword()));
            newUser.setFullName(request.getFullName());
            newUser.setEmail(request.getEmail());
            
            User savedUser = userRepository.save(newUser);
            System.out.println("User registered successfully: " + savedUser.getUsername());
            
            RegistrationResponse response = new RegistrationResponse(true, "Registration successful");
            response.setUsername(savedUser.getUsername());
            response.setFullName(savedUser.getFullName());
            response.setId(savedUser.getId());
            
            return ResponseEntity.status(201).body(response);
            
        } catch (Exception e) {
            System.err.println("Error in user registration: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(new RegistrationResponse(false, "Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/lawyer/register")
    public ResponseEntity<RegistrationResponse> lawyerRegister(@Valid @RequestBody LawyerRegistrationRequest request) {
        try {
            System.out.println("Lawyer registration attempt - Username: " + request.getUsername());
            
            // Validate password strength
            if (!passwordService.isPasswordStrong(request.getPassword())) {
                System.out.println("Registration failed: Password does not meet strength requirements");
                return ResponseEntity.badRequest()
                    .body(new RegistrationResponse(false, passwordService.getPasswordStrengthErrorMessage()));
            }
            
            // Check if username already exists
            if (lawyerRepository.existsByUsername(request.getUsername())) {
                System.out.println("Registration failed: Username already exists");
                return ResponseEntity.status(409)
                    .body(new RegistrationResponse(false, "Username already exists. Please choose a different username."));
            }
            
            // Check if bar number already exists (if provided)
            if (request.getBarNumber() != null && !request.getBarNumber().isEmpty()) {
                // Note: We'd need a findByBarNumber method in repository for proper check
                // For now, we'll allow registration but this should be added for production
            }
            
            // Create new lawyer
            Lawyer newLawyer = new Lawyer();
            newLawyer.setUsername(request.getUsername());
            newLawyer.setPassword(passwordService.hashPassword(request.getPassword()));
            newLawyer.setFullName(request.getFullName());
            newLawyer.setEmail(request.getEmail());
            newLawyer.setBarNumber(request.getBarNumber());
            newLawyer.setSpecialization(request.getSpecialization());
            
            Lawyer savedLawyer = lawyerRepository.save(newLawyer);
            System.out.println("Lawyer registered successfully: " + savedLawyer.getUsername());
            
            RegistrationResponse response = new RegistrationResponse(true, "Registration successful");
            response.setUsername(savedLawyer.getUsername());
            response.setFullName(savedLawyer.getFullName());
            response.setId(savedLawyer.getId());
            
            return ResponseEntity.status(201).body(response);
            
        } catch (Exception e) {
            System.err.println("Error in lawyer registration: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(new RegistrationResponse(false, "Internal server error: " + e.getMessage()));
        }
    }
}

