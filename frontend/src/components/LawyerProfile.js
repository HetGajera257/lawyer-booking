import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import Skeleton from 'react-loading-skeleton';
import 'react-loading-skeleton/dist/skeleton.css';
import { toast } from 'react-toastify';
import './LawyerProfile.css';

const API_BASE_URL = 'http://localhost:8080/api';

function LawyerProfile() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [lawyer, setLawyer] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchLawyerProfile();
  }, [id]);

  const fetchLawyerProfile = async () => {
    setLoading(true);
    setError('');

    try {
      const response = await fetch(`${API_BASE_URL}/bookings/lawyers`);
      
      if (!response.ok) {
        throw new Error('Failed to fetch lawyer profile');
      }

      const lawyers = await response.json();
      const foundLawyer = lawyers.find(l => l.id === parseInt(id));

      if (foundLawyer) {
        setLawyer(foundLawyer);
      } else {
        setError('Lawyer not found');
        toast.error('Lawyer profile not found');
      }
    } catch (err) {
      console.error('Error fetching lawyer profile:', err);
      setError('Error loading lawyer profile');
      toast.error('Error loading lawyer profile');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="lawyer-profile-container">
        <div className="lawyer-profile-card">
          <Skeleton height={200} style={{ marginBottom: '20px' }} />
          <Skeleton height={40} width="60%" style={{ marginBottom: '10px' }} />
          <Skeleton height={20} count={3} style={{ marginBottom: '10px' }} />
          <Skeleton height={100} style={{ marginTop: '20px' }} />
        </div>
      </div>
    );
  }

  if (error || !lawyer) {
    return (
      <div className="lawyer-profile-container">
        <div className="lawyer-profile-card">
          <div className="error-message">
            {error || 'Lawyer not found'}
          </div>
          <Link to="/" className="back-button">
            ← Back to Home
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="lawyer-profile-container">
      <div className="lawyer-profile-card">
        <div className="profile-header">
          <div className="profile-avatar">
            <span className="avatar-icon">⚖️</span>
          </div>
          <div className="profile-info">
            <h1>{lawyer.fullName || 'Lawyer Name'}</h1>
            <p className="specialization">{lawyer.specialization || 'Legal Services'}</p>
            {lawyer.barNumber && (
              <p className="bar-number">Bar Number: {lawyer.barNumber}</p>
            )}
          </div>
        </div>

        <div className="profile-details">
          <div className="detail-section">
            <h3>Contact Information</h3>
            {lawyer.email && (
              <p><strong>Email:</strong> {lawyer.email}</p>
            )}
          </div>

          <div className="detail-section">
            <h3>Specialization</h3>
            <p>{lawyer.specialization || 'General Practice'}</p>
          </div>

          {lawyer.barNumber && (
            <div className="detail-section">
              <h3>Bar Registration</h3>
              <p>{lawyer.barNumber}</p>
            </div>
          )}
        </div>

        <div className="profile-actions">
          <Link 
            to="/user-dashboard" 
            className="book-appointment-button"
            onClick={() => {
              // Store selected lawyer ID for booking
              localStorage.setItem('selectedLawyerId', lawyer.id);
            }}
          >
            Book Appointment
          </Link>
          <button 
            onClick={() => navigate(-1)} 
            className="back-button"
          >
            ← Back
          </button>
        </div>
      </div>
    </div>
  );
}

export default LawyerProfile;

