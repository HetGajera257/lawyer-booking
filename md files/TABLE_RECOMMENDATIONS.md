# Recommended Additional Tables for Lawyer Booking System

## Overview

This document outlines **10 recommended tables** to enhance your lawyer booking system with complete functionality.

## Current Tables
- ✅ `users` - Client users
- ✅ `lawyers` - Lawyers
- ✅ `client_audio` - Audio records (needs enhancement)

## Recommended Tables

### 1. 📅 **appointments** - Appointment/Booking Management
**Purpose**: Track appointments between users and lawyers

**Key Features**:
- Links users to lawyers
- Tracks appointment date/time and duration
- Status tracking (pending, confirmed, completed, cancelled)
- Meeting types (in-person, video, phone, audio)
- Notes and descriptions

**Use Cases**:
- Users can book appointments with lawyers
- Lawyers can view their schedule
- Track appointment history
- Manage appointment status

---

### 2. 📁 **cases** - Case/Matter Management
**Purpose**: Organize audio records and appointments by legal case

**Key Features**:
- Links users to lawyers for specific cases
- Case status tracking (open, in-progress, closed, on-hold)
- Priority levels (low, medium, high, urgent)
- Case descriptions and notes

**Use Cases**:
- Group related audio files by case
- Track case progress
- Organize all case-related documents and communications

---

### 3. 🔗 **Enhanced client_audio** - Link Audio to Users/Cases
**Purpose**: Link audio records to users, lawyers, and cases

**New Fields Added**:
- `user_id` - Who uploaded the audio
- `case_id` - Which case it belongs to
- `lawyer_id` - Assigned lawyer
- `file_name` - Original filename
- `file_size` - File size
- `uploaded_at` - Upload timestamp
- `status` - Processing status

**Use Cases**:
- Users see only their audio files
- Lawyers see audio files assigned to them
- Audio files organized by case

---

### 4. 💬 **messages** - Communication System
**Purpose**: Enable messaging between users and lawyers

**Key Features**:
- Case-linked messages
- Sender/receiver tracking
- Read/unread status
- Message threading

**Use Cases**:
- Users can message their lawyers
- Lawyers can respond to user queries
- Case-related discussions
- Internal notes

---

### 5. ⏰ **lawyer_availability** - Schedule Management
**Purpose**: Define lawyer's regular working hours

**Key Features**:
- Day of week availability
- Start/end times
- Available/unavailable status

**Use Cases**:
- Check lawyer availability before booking
- Display available time slots
- Prevent double-booking

---

### 6. 🚫 **lawyer_blocked_dates** - Blocked Dates
**Purpose**: Track specific dates when lawyer is unavailable

**Key Features**:
- Specific blocked dates
- Reason for blocking
- Holiday/vacation tracking

**Use Cases**:
- Block holidays
- Block vacation dates
- Block personal time off

---

### 7. 🔔 **notifications** - Notification System
**Purpose**: System notifications for users and lawyers

**Key Features**:
- Notification types (appointment, message, case_update, etc.)
- Read/unread tracking
- Links to relevant pages
- Timestamp tracking

**Use Cases**:
- Notify users when audio is processed
- Notify lawyers of new appointments
- Appointment reminders
- Case update notifications

---

### 8. ⭐ **lawyer_reviews** - Review & Rating System
**Purpose**: User reviews and ratings for lawyers

**Key Features**:
- 1-5 star ratings
- Review text
- Verified reviews (after appointment)
- Links to appointments

**Use Cases**:
- Users can rate lawyers after appointments
- Display lawyer ratings
- Build lawyer reputation
- Help users choose lawyers

---

### 9. 📄 **documents** - Document Management
**Purpose**: Store documents/files related to cases

**Key Features**:
- Case-linked documents
- File metadata (name, type, size)
- Upload tracking (user or lawyer)
- Document descriptions

**Use Cases**:
- Upload legal documents
- Share case files
- Document versioning
- File organization by case

---

### 10. 💳 **payments** - Payment Tracking
**Purpose**: Track payments for appointments and services

**Key Features**:
- Payment amounts and currency
- Payment status (pending, completed, failed, refunded)
- Payment methods
- Transaction IDs
- Links to appointments

**Use Cases**:
- Process appointment payments
- Track payment history
- Handle refunds
- Generate invoices

---

## Database Relationships

```
users
  ├── appointments (user_id)
  ├── cases (user_id)
  ├── client_audio (user_id)
  ├── messages (sender_id/receiver_id)
  ├── notifications (user_id)
  ├── lawyer_reviews (user_id)
  └── payments (user_id)

lawyers
  ├── appointments (lawyer_id)
  ├── cases (lawyer_id)
  ├── client_audio (lawyer_id)
  ├── lawyer_availability (lawyer_id)
  ├── lawyer_blocked_dates (lawyer_id)
  ├── messages (sender_id/receiver_id)
  ├── notifications (lawyer_id)
  ├── lawyer_reviews (lawyer_id)
  └── payments (lawyer_id)

cases
  ├── appointments (case_id)
  ├── client_audio (case_id)
  ├── messages (case_id)
  └── documents (case_id)

appointments
  ├── lawyer_reviews (appointment_id)
  └── payments (appointment_id)
```

## Implementation Priority

### Phase 1 - Essential (Implement First)
1. ✅ **appointments** - Core booking functionality
2. ✅ **Enhanced client_audio** - Link audio to users
3. ✅ **cases** - Organize by case

### Phase 2 - Important (Implement Next)
4. ✅ **messages** - Communication
5. ✅ **lawyer_availability** - Schedule management
6. ✅ **notifications** - User engagement

### Phase 3 - Enhanced Features (Implement Later)
7. ✅ **lawyer_blocked_dates** - Advanced scheduling
8. ✅ **lawyer_reviews** - Rating system
9. ✅ **documents** - File management
10. ✅ **payments** - Payment processing

## How to Implement

### Step 1: Run the SQL Script
```bash
mysql -u root -p legal_connect_db < recommended_tables.sql
```

### Step 2: Create Java Entities
Create corresponding entity classes in:
`lawyer-booking/src/main/java/com/legalconnect/lawyerbooking/entity/`

### Step 3: Create Repositories
Create JPA repositories for each entity.

### Step 4: Create Controllers
Create REST controllers for each feature.

### Step 5: Update Frontend
Add UI components for new features.

## Benefits

✅ **Complete Booking System** - Full appointment management  
✅ **Case Organization** - Organize all data by case  
✅ **Communication** - Direct messaging between users and lawyers  
✅ **Schedule Management** - Availability and booking system  
✅ **User Experience** - Notifications and reviews  
✅ **Document Management** - File storage and organization  
✅ **Payment Processing** - Complete payment tracking  

## Sample Queries

### Get all appointments for a user
```sql
SELECT a.*, l.full_name as lawyer_name, l.specialization
FROM appointments a
JOIN lawyers l ON a.lawyer_id = l.id
WHERE a.user_id = 1
ORDER BY a.appointment_date DESC;
```

### Get all audio files for a case
```sql
SELECT ca.*, u.full_name as user_name
FROM client_audio ca
JOIN users u ON ca.user_id = u.id
WHERE ca.case_id = 1
ORDER BY ca.uploaded_at DESC;
```

### Get unread notifications for a user
```sql
SELECT * FROM notifications
WHERE user_id = 1 AND is_read = FALSE
ORDER BY created_at DESC;
```

### Get lawyer's available time slots
```sql
SELECT la.*, ld.blocked_date
FROM lawyer_availability la
LEFT JOIN lawyer_blocked_dates ld 
  ON la.lawyer_id = ld.lawyer_id 
  AND DATE(NOW()) = ld.blocked_date
WHERE la.lawyer_id = 1 
  AND la.is_available = TRUE
  AND la.day_of_week = DAYOFWEEK(NOW());
```

## Next Steps

1. Review the recommended tables
2. Run `recommended_tables.sql` to create tables
3. Create Java entities for each table
4. Implement REST APIs for each feature
5. Update frontend to use new features

---

**Note**: Start with Phase 1 tables first, then gradually add Phase 2 and Phase 3 features based on your requirements.

