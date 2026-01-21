import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import LandingPage from './components/LandingPage';
import UserLogin from './components/UserLogin';
import UserRegistration from './components/UserRegistration';
import LawyerLogin from './components/LawyerLogin';
import UserDashboard from './components/UserDashboard';
import LawyerDashboard from './components/LawyerDashboard';
import LawyerProfile from './components/LawyerProfile';
import AdminLogin from './components/AdminLogin';
import AdminDashboard from './components/AdminDashboard';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import './App.css';

function App() {
  return (
    <Router>
      <div className="App">
        <ToastContainer />
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/user-login" element={<UserLogin />} />
          <Route path="/user-register" element={<UserRegistration />} />
          <Route path="/lawyer-login" element={<LawyerLogin />} />
          <Route path="/user-dashboard" element={<UserDashboard />} />
          <Route path="/lawyer-dashboard" element={<LawyerDashboard />} />
          <Route path="/lawyer/:id" element={<LawyerProfile />} />
          <Route path="/admin/login" element={<AdminLogin />} />
          <Route path="/admin/dashboard" element={<AdminDashboard />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;

