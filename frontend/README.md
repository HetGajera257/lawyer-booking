# Lawyer Booking Frontend

React-based frontend for the Lawyer Booking Audio Processing System.

## Features

- **User Login**: Separate login page for users
- **Lawyer Login**: Separate login page for lawyers
- **Audio Recording**: Users can record audio directly in the browser
- **Audio Upload**: Recorded audio is converted to WAV format and uploaded to the backend
- **Results Display**: Shows original text, masked text, and masked audio after processing
- **Lawyer Dashboard**: Lawyers can view all client audio records

## Setup Instructions

1. **Install Dependencies**
   ```bash
   cd frontend
   npm install
   ```

2. **Start Development Server**
   ```bash
   npm start
   ```

   The app will open at `http://localhost:3000`

## Backend Configuration

Make sure the Spring Boot backend is running on `http://localhost:8080`. If your backend runs on a different port, update the `API_BASE_URL` constant in:
- `src/components/UserDashboard.js`
- `src/components/LawyerDashboard.js`

## Usage

1. **As a User:**
   - Navigate to `/user-login`
   - Enter any username and password (authentication is simplified for demo)
   - Click "Start Recording" to record audio
   - Click "Stop Recording" when done
   - Click "Upload & Process Audio" to process the recording
   - View the results (original text, masked text, and masked audio)

2. **As a Lawyer:**
   - Navigate to `/lawyer-login`
   - Enter any username and password (authentication is simplified for demo)
   - View all client audio records
   - Click "Play Audio" to listen to masked text audio

## Technologies Used

- React 18.2.0
- React Router DOM 6.20.0
- HTML5 MediaRecorder API
- Web Audio API
- CSS3

## Browser Compatibility

- Chrome/Edge (recommended)
- Firefox
- Safari (may have limited MediaRecorder support)

Note: Audio recording requires HTTPS in production or localhost for development.

