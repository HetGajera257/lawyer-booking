package com.legalconnect.lawyerbooking.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.legalconnect.lawyerbooking.dto.LoginRequest;
import com.legalconnect.lawyerbooking.dto.LoginResponse;
import com.legalconnect.lawyerbooking.repository.AdminRepository;
import com.legalconnect.lawyerbooking.repository.UserRepository;
import com.legalconnect.lawyerbooking.repository.LawyerRepository;
import com.legalconnect.lawyerbooking.repository.CaseRepository;
import com.legalconnect.lawyerbooking.entity.Admin;
import com.legalconnect.lawyerbooking.entity.User;
import com.legalconnect.lawyerbooking.entity.Lawyer;
import com.legalconnect.lawyerbooking.entity.Case;
import com.legalconnect.lawyerbooking.service.PasswordService;
import com.legalconnect.lawyerbooking.util.JwtUtil;

import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LawyerRepository lawyerRepository;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private JwtUtil jwtUtil;

    // Admin Login
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> adminLogin(@RequestBody LoginRequest request) {
        try {
            System.out.println("Admin login attempt - Username: " + request.getUsername());

            Optional<Admin> adminOpt = adminRepository.findByUsername(request.getUsername());

            if (adminOpt.isEmpty()) {
                return ResponseEntity.status(401)
                    .body(new LoginResponse(false, "Invalid username or password"));
            }

            Admin admin = adminOpt.get();

            boolean passwordValid;
            if (passwordService.isHashed(admin.getPassword())) {
                passwordValid = passwordService.verifyPassword(request.getPassword(), admin.getPassword());
            } else {
                passwordValid = admin.getPassword().equals(request.getPassword());
                if (passwordValid) {
                    admin.setPassword(passwordService.hashPassword(request.getPassword()));
                    adminRepository.save(admin);
                }
            }

            if (passwordValid) {
                String token = jwtUtil.generateToken(admin.getId(), admin.getUsername(), "admin");

                LoginResponse response = new LoginResponse(true, "Login successful");
                response.setUserType("admin");
                response.setUsername(admin.getUsername());
                response.setFullName(admin.getFullName());
                response.setId(admin.getId());
                response.setToken(token);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(401)
                    .body(new LoginResponse(false, "Invalid username or password"));
            }
        } catch (Exception e) {
            System.err.println("Error in admin login: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(new LoginResponse(false, "Internal server error: " + e.getMessage()));
        }
    }

    // Dashboard Statistics
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            long totalUsers = userRepository.count();
            long totalLawyers = lawyerRepository.count();
            long totalCases = caseRepository.count();
            
            stats.put("totalUsers", totalUsers);
            stats.put("totalLawyers", totalLawyers);
            stats.put("totalCases", totalCases);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("Error fetching stats: " + e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    // Get All Users
    @GetMapping("/users")
    public ResponseEntity<Page<User>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> users = userRepository.findAll(pageable);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            System.err.println("Error fetching users: " + e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    // Get User by ID
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        try {
            Optional<User> user = userRepository.findById(id);
            return user.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Update User
    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userUpdate) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            if (userUpdate.getFullName() != null) user.setFullName(userUpdate.getFullName());
            if (userUpdate.getEmail() != null) user.setEmail(userUpdate.getEmail());
            
            User savedUser = userRepository.save(user);
            return ResponseEntity.ok(savedUser);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Delete User
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        try {
            if (!userRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            userRepository.deleteById(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "User deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Get All Lawyers
    @GetMapping("/lawyers")
    public ResponseEntity<Page<Lawyer>> getAllLawyers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Lawyer> lawyers = lawyerRepository.findAll(pageable);
            return ResponseEntity.ok(lawyers);
        } catch (Exception e) {
            System.err.println("Error fetching lawyers: " + e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    // Get Lawyer by ID
    @GetMapping("/lawyers/{id}")
    public ResponseEntity<Lawyer> getLawyerById(@PathVariable Long id) {
        try {
            Optional<Lawyer> lawyer = lawyerRepository.findById(id);
            return lawyer.map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Update Lawyer
    @PutMapping("/lawyers/{id}")
    public ResponseEntity<Lawyer> updateLawyer(@PathVariable Long id, @RequestBody Lawyer lawyerUpdate) {
        try {
            Optional<Lawyer> lawyerOpt = lawyerRepository.findById(id);
            if (lawyerOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Lawyer lawyer = lawyerOpt.get();
            if (lawyerUpdate.getFullName() != null) lawyer.setFullName(lawyerUpdate.getFullName());
            if (lawyerUpdate.getEmail() != null) lawyer.setEmail(lawyerUpdate.getEmail());
            if (lawyerUpdate.getSpecialization() != null) lawyer.setSpecialization(lawyerUpdate.getSpecialization());
            if (lawyerUpdate.getBarNumber() != null) lawyer.setBarNumber(lawyerUpdate.getBarNumber());
            
            Lawyer savedLawyer = lawyerRepository.save(lawyer);
            return ResponseEntity.ok(savedLawyer);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Delete Lawyer
    @DeleteMapping("/lawyers/{id}")
    public ResponseEntity<Map<String, String>> deleteLawyer(@PathVariable Long id) {
        try {
            if (!lawyerRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            lawyerRepository.deleteById(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Lawyer deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Get All Cases
    @GetMapping("/cases")
    public ResponseEntity<Page<Case>> getAllCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Case> cases = caseRepository.findAll(pageable);
            return ResponseEntity.ok(cases);
        } catch (Exception e) {
            System.err.println("Error fetching cases: " + e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    // Get Case by ID
    @GetMapping("/cases/{id}")
    public ResponseEntity<Case> getCaseById(@PathVariable Long id) {
        try {
            Optional<Case> caseEntity = caseRepository.findById(id);
            return caseEntity.map(ResponseEntity::ok)
                            .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Update Case
    @PutMapping("/cases/{id}")
    public ResponseEntity<Case> updateCase(@PathVariable Long id, @RequestBody Case caseUpdate) {
        try {
            Optional<Case> caseOpt = caseRepository.findById(id);
            if (caseOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Case caseEntity = caseOpt.get();
            if (caseUpdate.getCaseTitle() != null) caseEntity.setCaseTitle(caseUpdate.getCaseTitle());
            if (caseUpdate.getCaseStatus() != null) caseEntity.setCaseStatus(caseUpdate.getCaseStatus());
            if (caseUpdate.getCaseCategory() != null) caseEntity.setCaseCategory(caseUpdate.getCaseCategory());
            if (caseUpdate.getDescription() != null) caseEntity.setDescription(caseUpdate.getDescription());
            
            Case savedCase = caseRepository.save(caseEntity);
            return ResponseEntity.ok(savedCase);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Reassign Case
    @PutMapping("/cases/{id}/reassign")
    public ResponseEntity<Case> reassignCase(@PathVariable Long id, @RequestBody Map<String, Long> request) {
        try {
            Optional<Case> caseOpt = caseRepository.findById(id);
            if (caseOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Case caseEntity = caseOpt.get();
            Long newLawyerId = request.get("lawyerId");
            
            if (newLawyerId != null && lawyerRepository.existsById(newLawyerId)) {
                caseEntity.setLawyerId(newLawyerId);
                Case savedCase = caseRepository.save(caseEntity);
                return ResponseEntity.ok(savedCase);
            } else {
                return ResponseEntity.badRequest().body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Delete Case
    @DeleteMapping("/cases/{id}")
    public ResponseEntity<Map<String, String>> deleteCase(@PathVariable Long id) {
        try {
            if (!caseRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            caseRepository.deleteById(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Case deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
}
