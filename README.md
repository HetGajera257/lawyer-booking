# 🏛️ Lawyer Booking System

A comprehensive full-stack web application for lawyer booking, case management, and intelligent audio processing with AI-powered privacy protection.

## 📋 Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Setup Instructions](#setup-instructions)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Security](#security)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

## ✨ Features

### Core Features
- **User & Lawyer Authentication**: Secure registration and login with JWT tokens
- **Audio Processing Pipeline**: 
  - Record/upload audio files (Gujarati/English)
  - Automatic transcription using OpenAI Whisper
  - AI-powered PII masking (names, phones, emails, addresses, IDs)
  - Translation to Gujarati
  - Text-to-speech conversion (both languages)
- **Case Management**: Create, assign, and manage legal cases
- **Messaging System**: Real-time communication between users and lawyers
- **Appointment Booking**: Schedule and manage appointments
- **Lawyer Availability**: Manage lawyer schedules and availability
- **Admin Dashboard**: Comprehensive admin panel for system management

### Security Features
- BCrypt password hashing
- JWT token-based authentication
- Role-based access control (User, Lawyer, Admin)
- Input validation and sanitization
- Global exception handling
- Rate limiting for API endpoints

## 🛠️ Technology Stack

### Backend
- **Framework**: Spring Boot 3.4.1
- **Language**: Java 17
- **Database**: MySQL 8.0+
- **ORM**: JPA/Hibernate
- **Security**: Spring Security + JWT
- **API Documentation**: Swagger/OpenAPI 3.0
- **WebSocket**: Real-time communication
- **Rate Limiting**: Bucket4j
- **Logging**: SLF4J/Logback

### Frontend
- **Framework**: React 18.2.0
- **Routing**: React Router DOM 6.20.0
- **HTTP Client**: Axios
- **UI**: Custom CSS + React Toastify
- **WebSocket**: STOMP.js + SockJS
- **Loading**: React Loading Skeleton

### External Services
- **OpenAI Whisper API**: Audio transcription
- **OpenAI GPT-4o-mini**: Text masking and translation
- **OpenAI TTS API**: Text-to-speech conversion

## 📦 Prerequisites

Before you begin, ensure you have the following installed:

- **Java**: JDK 17 or higher
- **Node.js**: v16 or higher
- **npm**: v8 or higher
- **MySQL**: 8.0 or higher
- **Maven**: 3.6+ (or use Maven wrapper)
- **OpenAI API Key**: Get one from [OpenAI Platform](https://platform.openai.com/)

## 🚀 Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/HetGajera257/lawyer-booking.git
cd lawyer-booking
```

### 2. Database Setup

1. **Create MySQL Database**:
```sql
CREATE DATABASE legal_connect_db;
```

2. **Update Database Credentials** in `backend/src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/legal_connect_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

3. **Run Schema Scripts** (optional - JPA will auto-create tables):
```bash
mysql -u your_username -p legal_connect_db < create_admin.sql
```

### 3. Backend Configuration

1. **Configure API Keys**:
   - Copy `backend/src/main/resources/application-local.properties.example` to `application-local.properties`
   - Add your OpenAI API key:
   ```properties
   openai.api.key=your-openai-api-key-here
   jwt.secret=your-strong-secret-key-here
   ```

2. **Generate JWT Secret** (for production):
```bash
openssl rand -base64 64
```

3. **Build Backend**:
```bash
cd backend
./mvnw clean install
```

### 4. Frontend Setup

1. **Install Dependencies**:
```bash
cd frontend
npm install
```

2. **Configure API Base URL** (if needed):
   - Default: `http://localhost:8080`
   - Update in `src/utils/api.js` if backend runs on different port

## ⚙️ Configuration

### Environment Variables (Production)

For production deployment, use environment variables:

```bash
export OPENAI_API_KEY=your-openai-api-key
export JWT_SECRET=your-strong-secret-key
export DB_USERNAME=your-db-username
export DB_PASSWORD=your-db-password
```

### Application Properties

Key configuration files:
- `application.properties`: Main configuration
- `application-local.properties`: Local development (not committed to git)

## 🏃 Running the Application

### Start Backend

```bash
cd backend
./mvnw spring-boot:run
```

Backend will start on: `http://localhost:8080`

### Start Frontend

```bash
cd frontend
npm start
```

Frontend will start on: `http://localhost:3000`

### Verify Installation

1. **Backend Health Check**:
```bash
curl http://localhost:8080/api/bookings/lawyers
```

2. **Frontend**: Open `http://localhost:3000` in browser

## 📚 API Documentation

Once the backend is running, access Swagger UI:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### Main API Endpoints

#### Authentication
- `POST /api/auth/user/register` - User registration
- `POST /api/auth/user/login` - User login
- `POST /api/auth/lawyer/register` - Lawyer registration
- `POST /api/auth/lawyer/login` - Lawyer login
- `POST /api/auth/admin/login` - Admin login

#### Audio Processing
- `POST /api/audio/upload` - Upload and process audio
- `GET /api/audio/all` - Get all audio records
- `GET /api/audio/{id}` - Get specific audio record

#### Case Management
- `POST /api/cases/create` - Create case
- `GET /api/cases/user/{userId}` - Get user's cases
- `GET /api/cases/lawyer/{lawyerId}` - Get lawyer's cases
- `POST /api/cases/{caseId}/assign` - Assign lawyer to case

#### Messaging
- `POST /api/messages/send` - Send message
- `GET /api/messages/case/{caseId}` - Get case messages

#### Appointments
- `POST /api/bookings/create` - Create appointment
- `GET /api/bookings/user/{userId}` - Get user appointments
- `GET /api/bookings/lawyer/{lawyerId}` - Get lawyer appointments

## 📁 Project Structure

```
lawyer-booking/
├── backend/
│   ├── src/main/java/com/legalconnect/lawyerbooking/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/     # REST controllers
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── entity/         # JPA entities
│   │   ├── exception/       # Custom exceptions
│   │   ├── filter/          # JWT filter
│   │   ├── repository/      # JPA repositories
│   │   ├── security/        # Security components
│   │   ├── service/         # Business logic
│   │   └── util/            # Utilities
│   └── src/main/resources/
│       ├── application.properties
│       └── application-local.properties
│
├── frontend/
│   ├── src/
│   │   ├── components/      # React components
│   │   ├── context/         # React context
│   │   ├── hooks/           # Custom hooks
│   │   ├── utils/           # Utilities
│   │   └── App.js           # Main app
│   └── package.json
│
├── create_admin.sql         # Admin user creation script
├── .gitignore              # Git ignore file
└── README.md               # This file
```

## 🔒 Security

### Implemented Security Features
- ✅ BCrypt password hashing
- ✅ JWT token authentication
- ✅ CORS configuration
- ✅ Input validation
- ✅ SQL injection prevention (JPA)
- ✅ Global exception handling
- ✅ Rate limiting
- ✅ Role-based access control

### Security Best Practices
1. **Never commit** `application-local.properties` with real API keys
2. **Use environment variables** in production
3. **Generate strong JWT secrets** (min 256 bits)
4. **Keep dependencies updated**
5. **Use HTTPS** in production
6. **Implement rate limiting** for authentication endpoints

## 🐛 Troubleshooting

### Backend Issues

**Port 8080 already in use**:
```bash
# Find process using port 8080
netstat -ano | findstr :8080
# Kill the process or change port in application.properties
```

**Database connection error**:
- Verify MySQL is running
- Check database credentials
- Ensure database exists

**OpenAI API errors**:
- Verify API key is correct
- Check API quota/limits
- Ensure internet connection

### Frontend Issues

**Port 3000 already in use**:
```bash
# Kill process or use different port
PORT=3001 npm start
```

**Module not found**:
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
```

**CORS errors**:
- Verify backend CORS configuration
- Check backend is running
- Verify API base URL in frontend

## 📝 Development

### Adding New Features

1. **Backend**: Add entity → repository → service → controller
2. **Frontend**: Create component → add route → integrate API
3. **Testing**: Add unit tests for services
4. **Documentation**: Update API docs and README

### Code Style

- Follow Java naming conventions
- Use SLF4J for logging (not System.out.println)
- Add JavaDoc comments for public methods
- Use custom exceptions for error handling

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👥 Contributors

- **Het Gajera** - *Initial work* - [HetGajera257](https://github.com/HetGajera257)

## 🙏 Acknowledgments

- OpenAI for API services
- Spring Boot team
- React team
- All contributors

---

**For detailed API documentation, visit**: http://localhost:8080/swagger-ui.html

**For support, contact**: [Create an issue](https://github.com/HetGajera257/lawyer-booking/issues)
