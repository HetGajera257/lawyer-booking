import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import Skeleton from 'react-loading-skeleton';
import 'react-loading-skeleton/dist/skeleton.css';
import { getAuthHeaders } from '../utils/auth';
import './Booking.css';

const API_BASE_URL = 'http://localhost:8080/api';

function AppointmentsList({ userId, userType = 'user' }) {
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState('all'); // all, upcoming, past

  useEffect(() => {
    fetchAppointments();
  }, [userId, userType, filter]);

  const fetchAppointments = async () => {
    setLoading(true);
    setError('');

    try {
      let url;
      if (filter === 'upcoming') {
        url = userType === 'user' 
          ? `${API_BASE_URL}/bookings/user/${userId}/upcoming`
          : `${API_BASE_URL}/bookings/lawyer/${userId}/upcoming`;
      } else {
        url = userType === 'user'
          ? `${API_BASE_URL}/bookings/user/${userId}`
          : `${API_BASE_URL}/bookings/lawyer/${userId}`;
      }

      const response = await fetch(url, {
        headers: getAuthHeaders()
      });
      
      if (!response.ok) {
        throw new Error('Failed to fetch appointments');
      }

      const data = await response.json();
      setAppointments(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Error fetching appointments:', err);
      const errorMsg = err.message.includes('fetch') 
        ? 'Error loading appointments: Cannot connect to server. Please ensure the backend is running on http://localhost:8080'
        : 'Error loading appointments: ' + err.message;
      setError(errorMsg);
      toast.error(errorMsg);
      setAppointments([]);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = async (appointmentId) => {
    if (!window.confirm('Are you sure you want to cancel this appointment?')) {
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/bookings/${appointmentId}/cancel`, {
        method: 'PUT',
        headers: {
          ...getAuthHeaders(),
          'X-User-Id': userId.toString()
        }
      });

      const data = await response.json();

      if (response.ok && data.success) {
        toast.success('Appointment cancelled successfully');
        fetchAppointments(); // Refresh list
      } else {
        const errorMsg = data.message || 'Failed to cancel appointment';
        toast.error(errorMsg);
      }
    } catch (err) {
      console.error('Error cancelling appointment:', err);
      toast.error('Error cancelling appointment. Please try again.');
    }
  };

  const handleConfirm = async (appointmentId) => {
    try {
      const response = await fetch(`${API_BASE_URL}/bookings/${appointmentId}/confirm`, {
        method: 'PUT',
        headers: {
          ...getAuthHeaders(),
          'X-Lawyer-Id': userId.toString()
        }
      });

      const data = await response.json();

      if (response.ok && data.success) {
        toast.success('Appointment confirmed successfully');
        fetchAppointments(); // Refresh list
      } else {
        const errorMsg = data.message || 'Failed to confirm appointment';
        toast.error(errorMsg);
      }
    } catch (err) {
      console.error('Error confirming appointment:', err);
      toast.error('Error confirming appointment. Please try again.');
    }
  };

  const formatDateTime = (dateTimeString) => {
    const date = new Date(dateTimeString);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getStatusBadge = (status) => {
    const statusColors = {
      pending: '#f39c12',
      confirmed: '#27ae60',
      completed: '#3498db',
      cancelled: '#e74c3c'
    };
    
    return (
      <span 
        className="status-badge"
        style={{ backgroundColor: statusColors[status] || '#95a5a6' }}
      >
        {status.toUpperCase()}
      </span>
    );
  };

  const getMeetingTypeIcon = (type) => {
    const icons = {
      'video': '📹',
      'phone': '📞',
      'in-person': '🏢',
      'audio': '🎤'
    };
    return icons[type] || '📅';
  };

  return (
    <div className="appointments-list-container">
      <div className="appointments-header">
        <h2>My Appointments</h2>
        <div className="filter-buttons">
          <button
            onClick={() => setFilter('all')}
            className={filter === 'all' ? 'active' : ''}
          >
            All
          </button>
          <button
            onClick={() => setFilter('upcoming')}
            className={filter === 'upcoming' ? 'active' : ''}
          >
            Upcoming
          </button>
        </div>
      </div>

      {error && (
        <div className="error-message">
          <span className="error-text">{error}</span>
          <button 
            className="error-close" 
            onClick={() => setError('')}
            aria-label="Close error"
          >
            ×
          </button>
        </div>
      )}

      {loading ? (
        <div className="appointments-skeleton">
          {[1, 2, 3].map(i => (
            <div key={i} className="appointment-card-skeleton">
              <Skeleton height={60} style={{ marginBottom: '15px' }} />
              <Skeleton height={20} count={3} style={{ marginBottom: '10px' }} />
              <Skeleton height={40} width="30%" />
            </div>
          ))}
        </div>
      ) : appointments.length === 0 ? (
        <div className="empty-state">
          <p>No appointments found.</p>
        </div>
      ) : (
        <div className="appointments-grid">
          {appointments.map((appointment) => (
            <div key={appointment.id} className="appointment-card">
              <div className="appointment-header">
                <div>
                  <h3>
                    {getMeetingTypeIcon(appointment.meetingType)} {' '}
                    {userType === 'user' ? (
                      <Link to={`/lawyer/${appointment.lawyerId}`} className="profile-link">
                        {appointment.lawyerFullName || 'Lawyer'}
                      </Link>
                    ) : (
                      appointment.userFullName || 'Client'
                    )}
                  </h3>
                  <p className="appointment-date">
                    {formatDateTime(appointment.appointmentDate)}
                  </p>
                </div>
                {getStatusBadge(appointment.status)}
              </div>

              <div className="appointment-details">
                <p><strong>Duration:</strong> {appointment.durationMinutes} minutes</p>
                <p><strong>Meeting Type:</strong> {appointment.meetingType}</p>
                {appointment.description && (
                  <p><strong>Description:</strong> {appointment.description}</p>
                )}
              </div>

              <div className="appointment-actions">
                {userType === 'user' && 
                 (appointment.status === 'pending' || appointment.status === 'confirmed') && (
                  <button
                    onClick={() => handleCancel(appointment.id)}
                    className="cancel-button"
                  >
                    Cancel
                  </button>
                )}
                {userType === 'lawyer' && appointment.status === 'pending' && (
                  <button
                    onClick={() => handleConfirm(appointment.id)}
                    className="confirm-button"
                  >
                    Confirm
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default AppointmentsList;

