package com.legalconnect.lawyerbooking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.legalconnect.lawyerbooking.entity.ClientAudio;
import com.legalconnect.lawyerbooking.dto.CaseRequest;
import com.legalconnect.lawyerbooking.dto.CaseDTO;
import com.legalconnect.lawyerbooking.repository.ClientAudioRepository;
import com.legalconnect.lawyerbooking.exception.AudioProcessingException;

/**
 * Service responsible for processing audio files, including:
 * 1. Transcription (Whisper)
 * 2. PII Masking
 * 3. Translation
 * 4. Text-to-Speech generation
 * 5. Case creation and linking
 */
@Service
public class AudioProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(AudioProcessingService.class);

    private final OpenAIWhisperService whisperService;
    private final TextMaskingService maskingService;
    private final OpenAITextToSpeechService textToSpeechService;
    private final TextTranslationService translationService;
    private final ClientAudioRepository repository;
    private final CaseService caseService;
    private final CaseClassificationService classificationService;

    @Autowired
    public AudioProcessingService(
            OpenAIWhisperService whisperService,
            TextMaskingService maskingService,
            OpenAITextToSpeechService textToSpeechService,
            TextTranslationService translationService,
            ClientAudioRepository repository,
            CaseService caseService,
            CaseClassificationService classificationService) {
        this.whisperService = whisperService;
        this.maskingService = maskingService;
        this.textToSpeechService = textToSpeechService;
        this.translationService = translationService;
        this.repository = repository;
        this.caseService = caseService;
        this.classificationService = classificationService;
    }

    /**
     * Orchestrates the full audio processing workflow, including case creation if userId is present.
     * @param audio The uploaded audio file
     * @param userId The ID of the uploading user (optional)
     * @param caseTitle The title for the created case (optional)
     * @return The processed and saved ClientAudio entity
     */
    @Transactional
    public ClientAudio processAndCreateCase(MultipartFile audio, Long userId, String caseTitle) {
        // Core Processing Phase
        ClientAudio clientAudio = processAudioPipeline(audio, userId);

        // Case Creation Phase
        if (userId != null) {
            linkToCase(clientAudio, userId, caseTitle, audio.getOriginalFilename());
        } else {
            logger.warn("UserId is null, skipping case creation for audio ID: {}", clientAudio.getId());
        }

        return clientAudio;
    }
    
    // Legacy method support if needed, or redirect to main flow
    public ClientAudio process(MultipartFile audio) {
        return processAndCreateCase(audio, null, null);
    }
    
    public ClientAudio process(MultipartFile audio, Long userId) {
        return processAndCreateCase(audio, userId, null);
    }

    private ClientAudio processAudioPipeline(MultipartFile audio, Long userId) {
        try {
            logger.info("Starting audio pipeline for file: {} (size: {} bytes)", 
                       audio.getOriginalFilename(), audio.getSize());

            // 1. Transcription
            String originalEnglish = transcribeAudio(audio);

            // 2. Masking
            String maskedEnglish = maskPersonalInfo(originalEnglish);

            // 3. Audio & Translation Generation (Parallelizable in future)
            byte[] maskedTextAudio = generateEnglishAudio(maskedEnglish);
            String maskedGujarati = translateToGujarati(maskedEnglish);
            byte[] maskedGujaratiAudio = generateGujaratiAudio(maskedGujarati);

            // 4. Persistence
            return saveClientAudio(userId, originalEnglish, maskedEnglish, 
                                 maskedTextAudio, maskedGujarati, maskedGujaratiAudio);

        } catch (Exception e) {
            logger.error("Audio pipeline failed: {}", e.getMessage(), e);
            throw new AudioProcessingException("Failed to process audio file", e);
        }
    }

    private String transcribeAudio(MultipartFile audio) {
        logger.debug("Step 1: Transcribing audio...");
        String text = null;
        try {
            text = whisperService.translateToEnglish(audio);
        } catch (Exception e) {
             throw new AudioProcessingException("Whisper transcription failed", e);
        }
        
        if (text == null || text.trim().isEmpty()) {
            throw new AudioProcessingException("Transcription returned empty text");
        }
        logger.info("Transcription completed. Length: {}", text.length());
        return text;
    }

    private String maskPersonalInfo(String text) {
        logger.debug("Step 2: Masking personal info...");
        String masked = maskingService.maskEnglishPersonalInfo(text);
        if (masked == null || masked.trim().isEmpty()) {
            logger.warn("Masking returned empty, falling back to original");
            return text;
        }
        return masked;
    }

    private byte[] generateEnglishAudio(String text) {
        logger.debug("Step 3: Generating English TTS...");
        try {
            return textToSpeechService.textToSpeech(text, "en");
        } catch (Exception e) {
            logger.error("English TTS failed", e);
            return null; // Non-blocking failure
        }
    }

    private String translateToGujarati(String text) {
        logger.debug("Step 4: Translating to Gujarati...");
        try {
            return translationService.translateToGujarati(text);
        } catch (Exception e) {
            logger.error("Gujarati translation failed", e);
            return null; // Non-blocking failure
        }
    }

    private byte[] generateGujaratiAudio(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        
        logger.debug("Step 5: Generating Gujarati TTS...");
        try {
            return textToSpeechService.textToSpeech(text, "gu");
        } catch (Exception e) {
            logger.error("Gujarati TTS failed", e);
            return null; // Non-blocking failure
        }
    }

    private ClientAudio saveClientAudio(Long userId, String original, String masked, 
                                      byte[] audioEn, String gujarati, byte[] audioGu) {
        ClientAudio ca = new ClientAudio();
        ca.setUserId(userId);
        ca.setLanguage("english");
        ca.setOriginalEnglishText(original);
        ca.setMaskedEnglishText(masked);
        ca.setMaskedTextAudio(audioEn);
        ca.setMaskedGujaratiText(gujarati);
        ca.setMaskedGujaratiAudio(audioGu);
        return repository.save(ca);
    }

    private void linkToCase(ClientAudio clientAudio, Long userId, String caseTitle, String fileName) {
        try {
            String title = (caseTitle != null && !caseTitle.trim().isEmpty()) 
                ? caseTitle 
                : "Case from Audio - " + (fileName != null ? fileName : "recording");

            CaseRequest caseRequest = new CaseRequest();
            caseRequest.setUserId(userId);
            caseRequest.setCaseTitle(title);
            caseRequest.setCaseType("General");
            
            // 6. Classification
            logger.debug("Step 6: Classifying case category...");
            String category = classificationService.classifyCase(clientAudio.getMaskedEnglishText());
            caseRequest.setCaseCategory(category);
            
            // Generate description safely (max 500 chars)
            String description = clientAudio.getMaskedEnglishText() != null 
                ? clientAudio.getMaskedEnglishText() 
                : "Case created from audio upload";
            
            if (description.length() > 500) {
                description = description.substring(0, 497) + "...";
            }
            caseRequest.setDescription(description);

            System.out.println(">>> [STEP 7] Triggering CaseService.createCase for user: " + userId);
            logger.info("Step 7: Creating case via CaseService for user {}...", userId);
            CaseDTO caseDTO = caseService.createCase(caseRequest);
            
            if (caseDTO != null && caseDTO.getId() != null) {
                System.out.println(">>> [STEP 8] Case created SUCCESSFULLY! ID: " + caseDTO.getId());
                logger.info("Step 8: Successfully created case ID: {}", caseDTO.getId());
                clientAudio.setCaseId(caseDTO.getId());
                repository.save(clientAudio);
                System.out.println(">>> [STEP 9] Audio linked to Case ID: " + caseDTO.getId());
                logger.info("Step 9: Linked audio ID {} to new Case ID {}", clientAudio.getId(), caseDTO.getId());
            } else {
                System.err.println(">>> [STEP 8 FAIL] CaseDTO is NULL or missing ID!");
                logger.error("Step 8 FAIL: CaseService returned null or DTO with no ID!");
            }
        } catch (Exception e) {
            System.err.println(">>> [AUDIO PROCESSING ERROR] " + e.getMessage());
            e.printStackTrace();
            logger.error("Failed to create/link case for user {}: {}", userId, e.getMessage(), e);
            // We do NOT throw here to preserve the saved audio
        }
    }
}
