# 📋 Cases and Messaging System Implementation

## Overview

This document describes the implementation of the **Case Management** and **Messaging System** for the Lawyer Booking application. This feature allows users to upload audio files that automatically create cases, which lawyers can connect to, provide solutions, and communicate with users through a messaging interface.

---

## 🎯 Features Implemented

### 1. **Case Management System**
- Automatic case creation when users upload audio
- Case status tracking (open, in-progress, closed, on-hold)
- Lawyer assignment to cases
- Solution management for lawyers
- Case organization and filtering

### 2. **Messaging System**
- Real-time messaging between users and lawyers
- Message threading by case
- Read/unread message tracking
- Message history display

### 3. **User Features**
- View all cases created from audio uploads
- See case status and lawyer assignments
- View solutions provided by lawyers
- Access case details

### 4. **Lawyer Features**
- View unassigned cases
- Connect to cases
- View assigned cases
- Provide and update solutions
- Message with users about cases

---

## 🏗️ Architecture

### Backend Structure

```
backend/src/main/java/com/legalconnect/lawyerbooking/
├── entity/
│   ├── Case.java                    # Case entity
│   ├── Message.java                 # Message entity
│   └── ClientAudio.java             # Updated with caseId field
├── repository/
│   ├── CaseRepository.java          # Case data access
│   ├── MessageRepository.java       # Message data access
│   └── ClientAudioRepository.java   # Updated with new methods
├── dto/
│   ├── CaseDTO.java                 # Case data transfer object
│   ├── CaseRequest.java             # Case creation request
│   ├── MessageDTO.java              # Message data transfer object
│   └── MessageRequest.java          # Message creation request
├── service/
│   ├── CaseService.java                # Case business logic
│   └── MessageService.java         # Message business logic
└── controller/
    ├── CaseController.java          # Case REST endpoints
    ├── MessageController.java       # Message REST endpoints
    └── AudioController.java         # Updated to create cases
```

### Frontend Structure

```
frontend/src/components/
├── UserDashboard.js                 # Updated with Cases tab
└── LawyerDashboard.js               # Updated with Cases tab and messaging
```

---

## 📊 Database Schema

### Cases Table

```sql
CREATE TABLE IF NOT EXISTS cases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    lawyer_id BIGINT,
    case_title VARCHAR(255) NOT NULL,
    case_type VARCHAR(100),
    case_status VARCHAR(50) DEFAULT 'open',
    description LONGTEXT,
    solution LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (lawyer_id) REFERENCES lawyers(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_lawyer_id (lawyer_id),
    INDEX idx_case_status (case_status)
);
```

### Messages Table

```sql
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_id BIGINT,
    sender_id BIGINT NOT NULL,
    sender_type VARCHAR(20) NOT NULL,
    receiver_id BIGINT NOT NULL,
    receiver_type VARCHAR(20) NOT NULL,
    message_text LONGTEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_case_id (case_id),
    INDEX idx_sender_id (sender_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_created_at (created_at)
);
```

### Updated Client Audio Table

```sql
-- Add case_id column to client_audio table
ALTER TABLE client_audio ADD COLUMN case_id BIGINT;
```

---

## 🔌 API Endpoints

### Case Endpoints

#### 1. Create Case
```
POST /api/cases/create
Content-Type: application/json

Request Body:
{
  "userId": 1,
  "caseTitle": "Case Title",
  "caseType": "General",
  "description": "Case description"
}

Response: CaseDTO
```

#### 2. Get Case by ID
```
GET /api/cases/{id}

Response: CaseDTO
```

#### 3. Get Cases by User ID
```
GET /api/cases/user/{userId}

Response: List<CaseDTO>
```

#### 4. Get Cases by Lawyer ID
```
GET /api/cases/lawyer/{lawyerId}

Response: List<CaseDTO>
```

#### 5. Get Unassigned Cases
```
GET /api/cases/unassigned

Response: List<CaseDTO>
```

#### 6. Assign Lawyer to Case
```
POST /api/cases/{caseId}/assign
Content-Type: application/json

Request Body:
{
  "lawyerId": 1
}

Response: CaseDTO
```

#### 7. Update Case Solution
```
PUT /api/cases/{caseId}/solution
Content-Type: application/json

Request Body:
{
  "solution": "Solution text here"
}

Response: CaseDTO
```

#### 8. Update Case Status
```
PUT /api/cases/{caseId}/status
Content-Type: application/json

Request Body:
{
  "status": "in-progress"
}

Response: CaseDTO
```

### Message Endpoints

#### 1. Send Message
```
POST /api/messages/send
Content-Type: application/json

Request Body:
{
  "caseId": 1,
  "senderId": 1,
  "senderType": "lawyer",
  "receiverId": 2,
  "receiverType": "user",
  "messageText": "Message content"
}

Response: MessageDTO
```

#### 2. Get Messages by Case ID
```
GET /api/messages/case/{caseId}

Response: List<MessageDTO>
```

#### 3. Get Messages by Receiver
```
GET /api/messages/receiver/{receiverId}/{receiverType}

Response: List<MessageDTO>
```

#### 4. Mark Message as Read
```
PUT /api/messages/{messageId}/read

Response: 200 OK
```

#### 5. Get Unread Message Count
```
GET /api/messages/unread-count/{receiverId}/{receiverType}

Response: { "count": 5 }
```

### Updated Audio Endpoint

#### Upload Audio (Updated)
```
POST /api/audio/upload
Content-Type: multipart/form-data

Request Parameters:
- file: MultipartFile (required)
- userId: Long (optional)
- caseTitle: String (optional)

Response: ClientAudioDTO

Note: If userId is provided, a case is automatically created and linked to the audio.
```

---

## 💻 Frontend Implementation

### User Dashboard

#### New Features:
1. **My Cases Tab**
   - Displays all cases created by the user
   - Shows case status, lawyer assignment, and solutions
   - Organized by creation date

2. **Audio Upload Enhancement**
   - Automatically creates a case when audio is uploaded
   - Links audio to the created case
   - Passes userId during upload

#### Component Structure:
```javascript
// State management
const [cases, setCases] = useState([]);
const [casesLoading, setCasesLoading] = useState(false);

// Fetch cases
const fetchCases = async () => {
  const response = await fetch(`${CASES_API_BASE_URL}/user/${userId}`);
  const data = await response.json();
  setCases(data);
};
```

### Lawyer Dashboard

#### New Features:
1. **Cases Tab**
   - Shows unassigned cases (cases without lawyers)
   - Shows assigned cases (cases assigned to the lawyer)
   - Case detail view with:
     - Case information
     - Solution editor
     - Messaging interface

2. **Case Connection**
   - Lawyers can connect to unassigned cases
   - Updates case status to "in-progress"

3. **Solution Management**
   - Text area for providing solutions
   - Save/update solution functionality

4. **Messaging Interface**
   - View message history for selected case
   - Send messages to users
   - Real-time message display

#### Component Structure:
```javascript
// State management
const [cases, setCases] = useState([]);
const [unassignedCases, setUnassignedCases] = useState([]);
const [selectedCase, setSelectedCase] = useState(null);
const [messages, setMessages] = useState([]);
const [newMessage, setNewMessage] = useState('');
const [solutionText, setSolutionText] = useState('');

// Connect to case
const connectToCase = async (caseId) => {
  await fetch(`${CASES_API_BASE_URL}/${caseId}/assign`, {
    method: 'POST',
    body: JSON.stringify({ lawyerId })
  });
};

// Send message
const sendMessage = async () => {
  await fetch(`${MESSAGES_API_BASE_URL}/send`, {
    method: 'POST',
    body: JSON.stringify({
      caseId: selectedCase.id,
      senderId: lawyerId,
      senderType: 'lawyer',
      receiverId: selectedCase.userId,
      receiverType: 'user',
      messageText: newMessage
    })
  });
};

// Update solution
const updateSolution = async () => {
  await fetch(`${CASES_API_BASE_URL}/${selectedCase.id}/solution`, {
    method: 'PUT',
    body: JSON.stringify({ solution: solutionText })
  });
};
```

---

## 🔄 Workflow

### User Workflow

1. **Upload Audio**
   - User records or uploads audio file
   - User clicks "Upload & Process Audio"
   - System processes audio and creates a case automatically
   - Case is created with status "open"

2. **View Cases**
   - User navigates to "My Cases" tab
   - Views all their cases
   - Sees case status, assigned lawyer, and solutions

3. **Receive Messages**
   - User receives messages from assigned lawyers
   - Messages are displayed in case details

### Lawyer Workflow

1. **View Unassigned Cases**
   - Lawyer navigates to "Cases" tab
   - Views list of unassigned cases
   - Clicks "Connect to Case" to assign themselves

2. **Manage Assigned Cases**
   - Views cases assigned to them
   - Clicks on a case to view details
   - Provides solution in the solution editor
   - Saves solution to update case

3. **Communicate with Users**
   - Selects a case from the list
   - Views message history
   - Sends messages to the user
   - Receives responses from the user

---

## 📝 Database Migration

### Step 1: Add case_id Column to client_audio

```sql
ALTER TABLE client_audio ADD COLUMN case_id BIGINT;
```

**Note:** If you get an error that the column already exists, skip this step.

### Step 2: Create Cases Table

Run the `cases` table creation SQL from the schema section above.

### Step 3: Create Messages Table

Run the `messages` table creation SQL from the schema section above.

### Complete Migration Script

A complete migration script is available at:
```
backend/src/main/resources/schema_update_cases.sql
```

---

## 🚀 Setup Instructions

### Backend Setup

1. **Run Database Migration**
   ```bash
   # Connect to your MySQL database
   mysql -u username -p legal_connect_db < backend/src/main/resources/schema_update_cases.sql
   ```

2. **Rebuild Backend**
   ```bash
   cd backend
   mvn clean install
   mvn spring-boot:run
   ```

### Frontend Setup

1. **Install Dependencies** (if needed)
   ```bash
   cd frontend
   npm install
   ```

2. **Start Frontend**
   ```bash
   npm start
   ```

---

## 🧪 Testing

### Test Case Creation

1. **Upload Audio as User**
   - Login as a user
   - Record/upload audio
   - Upload audio (case should be created automatically)
   - Navigate to "My Cases" tab
   - Verify case is listed

2. **Connect to Case as Lawyer**
   - Login as a lawyer
   - Navigate to "Cases" tab
   - View unassigned cases
   - Click "Connect to Case"
   - Verify case moves to "My Cases" section

3. **Provide Solution**
   - Select a case from "My Cases"
   - Enter solution in text area
   - Click "Save Solution"
   - Verify solution is saved

4. **Send Messages**
   - Select a case
   - Type a message
   - Click "Send"
   - Verify message appears in message history

---

## 📋 Case Status Values

- **open**: Case is newly created, no lawyer assigned
- **in-progress**: Lawyer has been assigned, case is being worked on
- **closed**: Case has been resolved
- **on-hold**: Case is temporarily paused

---

## 🔐 Security Considerations

1. **Authorization**: Ensure users can only view their own cases
2. **Lawyer Assignment**: Only unassigned cases should be visible to lawyers
3. **Message Access**: Users and lawyers should only see messages for their cases
4. **Solution Updates**: Only assigned lawyers should be able to update solutions

---

## 🐛 Known Limitations

1. **Real-time Updates**: Messages require manual refresh (no WebSocket implementation)
2. **File Attachments**: Messages currently support text only
3. **Notification System**: No notification system for new messages (future enhancement)
4. **Case Search**: No search/filter functionality for cases (future enhancement)

---

## 🔮 Future Enhancements

1. **Real-time Messaging**: Implement WebSocket for real-time message updates
2. **File Attachments**: Support file attachments in messages
3. **Notifications**: Push notifications for new messages and case updates
4. **Case Search**: Search and filter cases by status, type, date
5. **Case Categories**: Categorize cases by legal type (criminal, family, etc.)
6. **Document Management**: Attach documents to cases
7. **Case History**: Track case status changes and updates
8. **Email Notifications**: Email notifications for important case events

---

## 📚 Related Documentation

- [PROJECT_FLOW_AND_ARCHITECTURE.md](md files/PROJECT_FLOW_AND_ARCHITECTURE.md) - Overall project architecture
- [TABLE_RECOMMENDATIONS.md](md files/TABLE_RECOMMENDATIONS.md) - Database table recommendations
- [recommended_tables.sql](md files/recommended_tables.sql) - SQL schema recommendations

---

## 📞 Support

For issues or questions regarding this implementation:
1. Check the API endpoints documentation above
2. Review the database schema
3. Check backend logs for errors
4. Verify database migration was successful

---

**Last Updated:** Implementation Date  
**Version:** 1.0.0  
**Status:** ✅ Complete

