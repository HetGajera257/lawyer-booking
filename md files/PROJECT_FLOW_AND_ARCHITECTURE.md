# 📋 Complete Project Flow & Architecture Documentation

## 🏗️ Project Overview

**Lawyer Booking System** - A full-stack application for processing client audio recordings with privacy protection, supporting both English and Gujarati languages.

---

## 🎯 Project Architecture

### **Technology Stack**

#### **Backend (Spring Boot)**
- **Framework**: Spring Boot 4.0.0
- **Language**: Java 17
- **Database**: MySQL (legal_connect_db)
- **ORM**: JPA/Hibernate
- **Security**: Spring Security (BCrypt password hashing)
- **HTTP Client**: OkHttp 4.12.0
- **JSON Processing**: Jackson
- **File Upload**: Commons FileUpload

#### **Frontend (React)**
- **Framework**: React 18.2.0
- **Routing**: React Router DOM 6.20.0
- **Audio Recording**: HTML5 MediaRecorder API
- **HTTP Client**: Fetch API

#### **External APIs**
- **OpenAI Whisper API**: Audio transcription (Gujarati → English)
- **OpenAI GPT-4o-mini**: Text masking and translation
- **OpenAI TTS API**: Text-to-speech conversion (English & Gujarati)

---

## 📊 Database Models (Entities)

### 1. **User Entity** (`users` table)
```java
- id (Long, Primary Key, Auto-generated)
- username (String, Unique, Not Null)
- password (String, Not Null) - BCrypt hashed
- email (String)
- fullName (String)
- createdAt (LocalDateTime)
- updatedAt (LocalDateTime)
```

### 2. **Lawyer Entity** (`lawyers` table)
```java
- id (Long, Primary Key, Auto-generated)
- username (String, Unique, Not Null)
- password (String, Not Null) - BCrypt hashed
- email (String)
- fullName (String)
- barNumber (String) - Lawyer's bar registration number
- specialization (String) - Legal specialization
- createdAt (LocalDateTime)
- updatedAt (LocalDateTime)
```

### 3. **ClientAudio Entity** (`client_audio` table)
```java
- id (Long, Primary Key, Auto-generated)
- language (String) - "english"
- originalEnglishText (LONGTEXT) - Translated text from audio
- maskedEnglishText (LONGTEXT) - English text with PII masked
- maskedTextAudio (LONGBLOB) - English masked audio (MP3 bytes)
- maskedGujaratiText (LONGTEXT) - Gujarati translated masked text
- maskedGujaratiAudio (LONGBLOB) - Gujarati masked audio (MP3 bytes)
```

---

## 🔄 Complete Project Flow

### **Phase 1: User Registration & Authentication**

#### **1.1 User Registration Flow**
```
Frontend (UserRegistration.js)
    ↓
POST /api/auth/user/register
    ↓
AuthController.userRegister()
    ↓
PasswordService.hashPassword() - BCrypt hashing
    ↓
UserRepository.save() - Save to database
    ↓
Return RegistrationResponse
```

**API Endpoint:**
- `POST /api/auth/user/register`
- **Request Body:**
  ```json
  {
    "username": "string",
    "password": "string",
    "email": "string",
    "fullName": "string"
  }
  ```
- **Response:**
  ```json
  {
    "success": true,
    "message": "Registration successful",
    "username": "string",
    "fullName": "string",
    "id": 1
  }
  ```

#### **1.2 User Login Flow**
```
Frontend (UserLogin.js)
    ↓
POST /api/auth/user/login
    ↓
AuthController.userLogin()
    ↓
UserRepository.findByUsername()
    ↓
PasswordService.verifyPassword() - BCrypt verification
    ↓
Store userType="user" in localStorage
    ↓
Navigate to /user-dashboard
```

**API Endpoint:**
- `POST /api/auth/user/login`
- **Request Body:**
  ```json
  {
    "username": "string",
    "password": "string"
  }
  ```
- **Response:**
  ```json
  {
    "success": true,
    "message": "Login successful",
    "userType": "user",
    "username": "string",
    "fullName": "string",
    "id": 1
  }
  ```

#### **1.3 Lawyer Login Flow**
```
Frontend (LawyerLogin.js)
    ↓
POST /api/auth/lawyer/login
    ↓
AuthController.lawyerLogin()
    ↓
LawyerRepository.findByUsername()
    ↓
PasswordService.verifyPassword() - BCrypt verification
    ↓
Store userType="lawyer" in localStorage
    ↓
Navigate to /lawyer-dashboard
```

**API Endpoint:**
- `POST /api/auth/lawyer/login`
- **Request Body:** Same as user login
- **Response:** Same format, `userType: "lawyer"`

---

### **Phase 2: Audio Recording & Processing (User Flow)**

#### **2.1 Audio Recording Flow**
```
UserDashboard.js
    ↓
startRecording()
    ↓
navigator.mediaDevices.getUserMedia() - Request microphone
    ↓
MediaRecorder API - Record audio chunks
    ↓
stopRecording()
    ↓
Convert chunks to Blob (WAV/WebM format)
    ↓
Display audio preview
```

**Supported Audio Formats:**
- `audio/webm;codecs=opus` (preferred)
- `audio/webm`
- `audio/ogg;codecs=opus`
- `audio/mp4`
- `audio/wav`

#### **2.2 Audio Upload & Processing Flow**
```
UserDashboard.js
    ↓
POST /api/audio/upload (multipart/form-data)
    ↓
AudioController.uploadAudio()
    ↓
Validate file (size < 20MB, audio type)
    ↓
AudioProcessingService.process()
    ↓
    ├─ Step 1: OpenAIWhisperService.translateToEnglish()
    │   └─ OpenAI Whisper API: Gujarati Audio → English Text
    │
    ├─ Step 2: TextMaskingService.maskEnglishPersonalInfo()
    │   └─ OpenAI GPT-4o-mini: Mask PII (names, phones, emails, addresses, IDs, DOB)
    │
    ├─ Step 3: OpenAITextToSpeechService.textToSpeech(maskedEnglish, "en")
    │   └─ OpenAI TTS API: English Masked Text → English Audio (MP3)
    │
    ├─ Step 4: TextTranslationService.translateToGujarati()
    │   └─ OpenAI GPT-4o-mini: English Masked Text → Gujarati Masked Text
    │
    └─ Step 5: OpenAITextToSpeechService.textToSpeech(maskedGujarati, "gu")
        └─ OpenAI TTS API: Gujarati Masked Text → Gujarati Audio (MP3)
    ↓
ClientAudioRepository.save() - Save to database
    ↓
Convert to ClientAudioDTO (with Base64 audio)
    ↓
Return JSON response to frontend
    ↓
UserDashboard displays results
```

**API Endpoint:**
- `POST /api/audio/upload`
- **Request:** `multipart/form-data` with `file` parameter
- **Response:**
  ```json
  {
    "id": 1,
    "language": "english",
    "originalEnglishText": "string",
    "maskedEnglishText": "string",
    "maskedTextAudioBase64": "base64-encoded-mp3",
    "maskedGujaratiText": "string",
    "maskedGujaratiAudioBase64": "base64-encoded-mp3"
  }
  ```

---

### **Phase 3: Lawyer Dashboard Flow**

#### **3.1 View All Audio Records**
```
LawyerDashboard.js
    ↓
GET /api/audio/all
    ↓
AudioController.getAllRecords()
    ↓
ClientAudioRepository.findAll()
    ↓
Convert to List<ClientAudioDTO>
    ↓
Return JSON array
    ↓
LawyerDashboard displays records
```

**API Endpoint:**
- `GET /api/audio/all`
- **Response:** Array of ClientAudioDTO objects

#### **3.2 Language Selection & Audio Playback**
```
LawyerDashboard.js
    ↓
User selects language (English/Gujarati)
    ↓
Display corresponding text and audio
    ↓
playAudio() - Decode Base64 to Blob
    ↓
Create Audio URL
    ↓
HTML5 Audio element plays audio
```

**Features:**
- Toggle between English and Gujarati per record
- Play/pause audio controls
- Display masked text in selected language

---

## 🔧 Service Layer Architecture

### **1. AudioProcessingService**
**Purpose:** Orchestrates the complete audio processing pipeline

**Methods:**
- `process(MultipartFile audio)`: Main processing method

**Dependencies:**
- `OpenAIWhisperService` - Audio transcription
- `TextMaskingService` - PII masking
- `TextTranslationService` - English to Gujarati translation
- `OpenAITextToSpeechService` - Text-to-speech conversion
- `ClientAudioRepository` - Database operations

### **2. OpenAIWhisperService**
**Purpose:** Transcribe Gujarati audio to English text

**Methods:**
- `translateToEnglish(MultipartFile file)`: Uses OpenAI Whisper API

**API Used:**
- `POST https://api.openai.com/v1/audio/translations`
- Model: `whisper-1`

### **3. TextMaskingService**
**Purpose:** Mask personal information while preserving case details

**Methods:**
- `maskEnglishPersonalInfo(String text)`: Uses GPT-4o-mini to intelligently mask PII

**API Used:**
- `POST https://api.openai.com/v1/chat/completions`
- Model: `gpt-4o-mini`
- Temperature: 0.1 (for consistent masking)

**Masks:**
- Names → `[NAME_MASKED]`
- Phone numbers → `[PHONE_MASKED]`
- Emails → `[EMAIL_MASKED]`
- Addresses → `[ADDRESS_MASKED]`
- ID numbers → `[ID_MASKED]`
- Dates of birth → `[DOB_MASKED]`
- Other PII → `[PII_MASKED]`

### **4. TextTranslationService**
**Purpose:** Translate masked English text to Gujarati

**Methods:**
- `translateToGujarati(String englishText)`: Uses GPT-4o-mini for translation

**API Used:**
- `POST https://api.openai.com/v1/chat/completions`
- Model: `gpt-4o-mini`
- Temperature: 0.3 (for consistent translation)

**Features:**
- Preserves mask tokens exactly
- Natural Gujarati translation
- Handles long texts with chunking

### **5. OpenAITextToSpeechService**
**Purpose:** Convert text to speech audio

**Methods:**
- `textToSpeech(String text)`: Default English
- `textToSpeech(String text, String languageCode)`: Language-specific

**API Used:**
- `POST https://api.openai.com/v1/audio/speech`
- Model: `tts-1`
- Voice: `alloy` (English), `nova` (Gujarati)
- Format: `mp3`

### **6. PasswordService**
**Purpose:** Secure password hashing and verification

**Methods:**
- `hashPassword(String password)`: BCrypt hashing
- `verifyPassword(String raw, String hashed)`: BCrypt verification
- `isHashed(String password)`: Check if password is already hashed

---

## 🌐 API Endpoints Summary

### **Authentication APIs** (`/api/auth`)

| Method | Endpoint | Description | Request Body |
|--------|----------|-------------|--------------|
| POST | `/api/auth/user/login` | User login | `{username, password}` |
| POST | `/api/auth/lawyer/login` | Lawyer login | `{username, password}` |
| POST | `/api/auth/user/register` | User registration | `{username, password, email, fullName}` |

### **Audio Processing APIs** (`/api/audio`)

| Method | Endpoint | Description | Parameters |
|--------|----------|-------------|------------|
| POST | `/api/audio/upload` | Upload and process audio | `file` (multipart) |
| GET | `/api/audio/all` | Get all audio records | None |
| GET | `/api/audio/{id}` | Get specific audio record | `id` (path variable) |

---

## 🔐 Security Configuration

### **SecurityConfig**
- **CSRF**: Disabled (for API endpoints)
- **CORS**: Enabled for all origins (`*`)
- **Session**: Stateless (no session management)
- **Public Endpoints**: `/api/auth/**`, `/api/audio/**`
- **Password Hashing**: BCrypt (Spring Security)

---

## 📱 Frontend Routes

| Route | Component | Description |
|-------|-----------|-------------|
| `/` | Redirect | Redirects to `/user-login` |
| `/user-login` | UserLogin | User login page |
| `/user-register` | UserRegistration | User registration page |
| `/lawyer-login` | LawyerLogin | Lawyer login page |
| `/user-dashboard` | UserDashboard | Audio recording & upload |
| `/lawyer-dashboard` | LawyerDashboard | View all audio records |

---

## 🔄 Complete Data Flow Diagram

```
┌─────────────────┐
│   User Browser  │
└────────┬────────┘
         │
         │ 1. Record Audio (MediaRecorder API)
         ↓
┌─────────────────┐
│ UserDashboard.js│
└────────┬────────┘
         │
         │ 2. POST /api/audio/upload (multipart)
         ↓
┌─────────────────┐
│ AudioController  │
└────────┬────────┘
         │
         │ 3. AudioProcessingService.process()
         ↓
    ┌────┴────┐
    │         │
    ↓         ↓
┌─────────┐ ┌──────────────┐
│ Whisper │ │ TextMasking  │
│ Service │ │   Service    │
└────┬────┘ └──────┬───────┘
     │             │
     │ 4. OpenAI   │ 5. OpenAI GPT-4o-mini
     │ Whisper API │ (Mask PII)
     │             │
     ↓             ↓
┌─────────────────────────┐
│ Masked English Text     │
└────┬────────────────────┘
     │
     ├─→ 6. TTS Service (English Audio)
     │
     └─→ 7. Translation Service
         │
         ↓
     ┌─────────────────────┐
     │ Masked Gujarati Text│
     └────┬────────────────┘
          │
          └─→ 8. TTS Service (Gujarati Audio)
              │
              ↓
     ┌─────────────────────┐
     │  Save to Database   │
     │  (ClientAudio)      │
     └────┬────────────────┘
          │
          ↓
┌─────────────────────────┐
│  Return DTO to Frontend │
│  (with Base64 audio)    │
└─────────────────────────┘
```

---

## 🎯 Key Features

1. **Multi-language Support**
   - English and Gujarati masked audio
   - Language toggle in lawyer dashboard

2. **Privacy Protection**
   - Intelligent PII masking
   - Preserves case-related information
   - Mask tokens preserved in translation

3. **Secure Authentication**
   - BCrypt password hashing
   - Separate user and lawyer accounts
   - Session management via localStorage

4. **Audio Processing Pipeline**
   - Automatic transcription (Gujarati → English)
   - PII masking with AI
   - Dual-language audio generation

5. **User Experience**
   - Browser-based audio recording
   - Real-time audio preview
   - Playback controls for masked audio

---

## 📦 Dependencies (Key Libraries)

### **Backend**
- `spring-boot-starter-web` - REST API
- `spring-boot-starter-data-jpa` - Database ORM
- `spring-boot-starter-security` - Security & BCrypt
- `mysql-connector-j` - MySQL driver
- `okhttp:4.12.0` - HTTP client for OpenAI APIs
- `jackson-databind` - JSON processing
- `commons-fileupload:1.5` - File upload handling

### **Frontend**
- `react:18.2.0` - UI framework
- `react-router-dom:6.20.0` - Routing
- HTML5 MediaRecorder API - Audio recording

---

## 🔑 Configuration

### **application.properties**
- Database: MySQL (`legal_connect_db`)
- File Upload: 20MB max
- OpenAI API Key: Configured via environment variable or local properties
- JPA: Auto-update schema

### **Environment Variables**
- `OPENAI_API_KEY` - OpenAI API key (optional, can use application-local.properties)

---

## 🚀 Deployment Flow

1. **Backend**: Spring Boot runs on `http://localhost:8080`
2. **Frontend**: React runs on `http://localhost:3000`
3. **Database**: MySQL on `localhost:3306`
4. **External APIs**: OpenAI services (Whisper, GPT, TTS)

---

## 📝 Notes

- All audio processing happens server-side
- Masked audio is stored as binary (LONGBLOB) in database
- Base64 encoding used for API responses
- Supports backward compatibility (plain text passwords auto-hashed)
- Error handling with user-friendly messages
- Comprehensive logging for debugging

---

**Last Updated**: Based on current codebase with Gujarati language support

