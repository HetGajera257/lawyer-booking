package com.legalconnect.lawyerbooking.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;

import com.legalconnect.lawyerbooking.service.AudioProcessingService;
import com.legalconnect.lawyerbooking.repository.ClientAudioRepository;
import com.legalconnect.lawyerbooking.entity.ClientAudio;
import com.legalconnect.lawyerbooking.dto.ClientAudioDTO;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audio")
@CrossOrigin(origins = "*")
public class AudioController {

    @Autowired
    private AudioProcessingService audioService;

    @Autowired
    private ClientAudioRepository repository;

    @Autowired
    private com.legalconnect.lawyerbooking.service.CaseService caseService;

    @Autowired
    private com.legalconnect.lawyerbooking.service.RateLimitService rateLimitService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "caseTitle", required = false) String caseTitle) {

        try {
            if (!rateLimitService.tryConsumeAi()) {
                return ResponseEntity.status(429).body("{\"error\": \"Rate limit exceeded for AI video/audio processing. Please try again later.\"}");
            }

            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body("{\"error\": \"Audio file is missing or empty\"}");
            }

            // ... (rest of validation) ...
            long maxSize = 20 * 1024 * 1024; // 20MB
            if (file.getSize() > maxSize) {
                return ResponseEntity.badRequest()
                    .body("{\"error\": \"File size exceeds 20MB limit.\"}");
            }

            // Using the refactored, robust method from AudioProcessingService
            ClientAudio saved = audioService.processAndCreateCase(file, userId, caseTitle);
            
            // Convert to DTO to include masked audio as Base64
            ClientAudioDTO dto = new ClientAudioDTO(
                saved.getId(),
                saved.getLanguage(),
                saved.getOriginalEnglishText(),
                saved.getMaskedEnglishText(),
                saved.getMaskedTextAudio(),
                saved.getMaskedGujaratiText(),
                saved.getMaskedGujaratiAudio(),
                saved.getUserId(),
                saved.getCaseId(),
                saved.getLawyerId()
            );
            
            System.out.println("Returning DTO with masked audio: " + 
                (dto.getMaskedTextAudioBase64() != null ? 
                    dto.getMaskedTextAudioBase64().length() + " characters (base64)" : "null"));
            
            return ResponseEntity.ok(dto);

        } catch (RuntimeException e) {
            System.err.println("Error processing audio: " + e.getMessage());
            e.printStackTrace();
            
            // Return user-friendly error message
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Whisper")) {
                errorMessage = "Error processing audio with Whisper API. " +
                    "Please check: 1) Your internet connection, 2) Audio file format is supported, " +
                    "3) Audio file is not corrupted. Original error: " + e.getMessage();
            } else if (errorMessage != null && errorMessage.contains("timeout")) {
                errorMessage = "Audio processing timed out. The audio file might be too long. " +
                    "Please try with a shorter audio file (under 5 minutes).";
            }
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", errorMessage);
            return ResponseEntity.status(500).body(errorResponse);
        } catch (Exception e) {
            System.err.println("Unexpected error processing audio: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Unexpected error: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<ClientAudioDTO>> getAllRecords() {
        List<ClientAudio> records = repository.findAll();
        System.out.println("Fetching all records, count: " + records.size());
        
        List<ClientAudioDTO> dtos = records.stream()
            .map(record -> {
                System.out.println("Processing record ID: " + record.getId() + 
                    ", English masked audio: " + (record.getMaskedTextAudio() != null ? 
                        record.getMaskedTextAudio().length + " bytes" : "null") +
                    ", Gujarati masked audio: " + (record.getMaskedGujaratiAudio() != null ? 
                        record.getMaskedGujaratiAudio().length + " bytes" : "null"));
                return new ClientAudioDTO(
                    record.getId(),
                    record.getLanguage(),
                    record.getOriginalEnglishText(),
                    record.getMaskedEnglishText(),
                    record.getMaskedTextAudio(),
                    record.getMaskedGujaratiText(),
                    record.getMaskedGujaratiAudio(),
                    record.getUserId(),
                    record.getCaseId(),
                    record.getLawyerId()
                );
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ClientAudioDTO> getRecordById(@PathVariable Long id) {
        ClientAudio record = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Record not found with id: " + id));
        
        System.out.println("Fetching record ID: " + id + 
            ", English masked audio: " + (record.getMaskedTextAudio() != null ? 
                record.getMaskedTextAudio().length + " bytes" : "null") +
            ", Gujarati masked audio: " + (record.getMaskedGujaratiAudio() != null ? 
                record.getMaskedGujaratiAudio().length + " bytes" : "null"));
        
        ClientAudioDTO dto = new ClientAudioDTO(
            record.getId(),
            record.getLanguage(),
            record.getOriginalEnglishText(),
            record.getMaskedEnglishText(),
            record.getMaskedTextAudio(),
            record.getMaskedGujaratiText(),
            record.getMaskedGujaratiAudio(),
            record.getUserId(),
            record.getCaseId(),
            record.getLawyerId()
        );
        
        return ResponseEntity.ok(dto);
    }
}
