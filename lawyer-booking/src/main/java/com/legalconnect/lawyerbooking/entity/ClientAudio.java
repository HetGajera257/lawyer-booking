package com.legalconnect.lawyerbooking.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "client_audio")
public class ClientAudio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String language;

    @Lob
    @Column(name = "original_english_text", columnDefinition = "LONGTEXT")
    private String originalEnglishText;

    @Lob
    @Column(name = "masked_english_text", columnDefinition = "LONGTEXT")
    private String maskedEnglishText;

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getOriginalEnglishText() { return originalEnglishText; }
    public void setOriginalEnglishText(String originalEnglishText) {
        this.originalEnglishText = originalEnglishText;
    }

    public String getMaskedEnglishText() { return maskedEnglishText; }
    public void setMaskedEnglishText(String maskedEnglishText) {
        this.maskedEnglishText = maskedEnglishText;
    }
}
