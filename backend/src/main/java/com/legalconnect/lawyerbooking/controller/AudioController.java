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

    @PostMapping("/upload")
    public ResponseEntity<?> uploadAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "caseTitle", required = false) String caseTitle) {

        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body("{\"error\": \"Audio file is missing or empty\"}");
            }

            // Validate file size (20MB limit)
            long maxSize = 20 * 1024 * 1024; // 20MB
            if (file.getSize() > maxSize) {
                return ResponseEntity.badRequest()
                    .body("{\"error\": \"File size exceeds 20MB limit. Please upload a smaller file.\"}");
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType != null && !contentType.startsWith("audio/")) {
                System.out.println("Warning: File content type is: " + contentType + 
                    ", but proceeding with upload");
            }

            System.out.println("Uploading file: " + file.getOriginalFilename() + 
                ", size: " + file.getSize() + " bytes, type: " + contentType);

            ClientAudio saved = audioService.process(file, userId);
            
            // Create case if userId is provided
            Long caseId = null;
            if (userId != null) {
                try {
                    String title = caseTitle != null && !caseTitle.trim().isEmpty() 
                        ? caseTitle 
                        : "Case from Audio - " + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "recording");
                    
                    com.legalconnect.lawyerbooking.dto.CaseRequest caseRequest = 
                        new com.legalconnect.lawyerbooking.dto.CaseRequest();
                    caseRequest.setUserId(userId);
                    caseRequest.setCaseTitle(title);
                    caseRequest.setCaseType("General");
                    caseRequest.setDescription(saved.getMaskedEnglishText() != null 
                        ? saved.getMaskedEnglishText().substring(0, Math.min(500, saved.getMaskedEnglishText().length()))
                        : "Case created from audio upload");
                    
                    com.legalconnect.lawyerbooking.dto.CaseDTO caseDTO = caseService.createCase(caseRequest);
                    caseId = caseDTO.getId();
                    
                    System.out.println("Case created successfully with ID: " + caseId + " for user ID: " + userId);
                    
                    // Link audio to case
                    saved.setCaseId(caseId);
                    saved = repository.save(saved);
                    
                    System.out.println("Audio record linked to case ID: " + caseId);
                } catch (Exception e) {
                    System.err.println("Error creating case for user " + userId + ": " + e.getMessage());
                    e.printStackTrace();
                    // Continue without case creation - audio is still saved
                    System.err.println("Warning: Audio uploaded but case creation failed. Audio ID: " + saved.getId());
                }
            } else {
                System.out.println("Warning: userId is null, case will not be created for audio ID: " + saved.getId());
            }
            
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
            
            return ResponseEntity.status(500)
                .body("{\"error\": \"" + errorMessage.replace("\"", "\\\"") + "\"}");
        } catch (Exception e) {
            System.err.println("Unexpected error processing audio: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body("{\"error\": \"Unexpected error: " + e.getMessage().replace("\"", "\\\"") + "\"}");
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
