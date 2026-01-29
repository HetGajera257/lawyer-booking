package com.legalconnect.lawyerbooking.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;

import com.legalconnect.lawyerbooking.service.AudioProcessingService;

@RestController
@RequestMapping("/api/audio")
public class AudioController {

    @Autowired
    private AudioProcessingService audioService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadAudio(
            @RequestParam("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("Audio file missing");
        }

        return ResponseEntity.ok(audioService.process(file));
    }
}
