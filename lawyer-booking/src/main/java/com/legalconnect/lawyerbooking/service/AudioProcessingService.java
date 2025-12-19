package com.legalconnect.lawyerbooking.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import com.legalconnect.lawyerbooking.entity.ClientAudio;
import com.legalconnect.lawyerbooking.repository.ClientAudioRepository;

@Service
public class AudioProcessingService {

    @Autowired
    private OpenAIWhisperService whisperService;

    @Autowired
    private TextMaskingService maskingService;

    @Autowired
    private ClientAudioRepository repository;

    public ClientAudio process(MultipartFile audio) {

        try {
            System.out.println("Processing audio: " + audio.getOriginalFilename());

            // 1️⃣ Gujarati audio → English text
            String originalEnglish =
                    whisperService.translateToEnglish(audio);

            // 2️⃣ Mask English personal info
            String maskedEnglish =
                    maskingService.maskEnglishPersonalInfo(originalEnglish);

            // 3️⃣ Save to DB
            ClientAudio ca = new ClientAudio();
            ca.setLanguage("english");
            ca.setOriginalEnglishText(originalEnglish);
            ca.setMaskedEnglishText(maskedEnglish);

            return repository.save(ca);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Audio processing failed: " + e.getMessage());
        }
    }
}
