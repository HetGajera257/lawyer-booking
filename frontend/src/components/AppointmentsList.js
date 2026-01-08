import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import Skeleton from 'react-loading-skeleton';
import 'react-loading-skeleton/dist/skeleton.css';
import { getAuthHeaders } from '../utils/auth';
import Booking from './Booking';
import './Booking.css';

const API_BASE_URL = 'http://localhost:8080/api';

function AppointmentsList({ userId, userType = 'user', caseId = null }) {
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState('all'); // all, upcoming, past
  const [editingAppointment, setEditingAppointment] = useState(null);

  const fetchAppointments = useCallback(async () => {
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
  }, [userId, userType, filter]);

  useEffect(() => {
    fetchAppointments();
  }, [fetchAppointments]);

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

  const handleEditClick = (appointment) => {
    setEditingAppointment(appointment);
  };

  const handleEditSuccess = () => {
    setEditingAppointment(null);
    fetchAppointments(); // Refresh list
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

  const filteredAppointments = caseId
    ? appointments.filter(app => String(app.caseId) === String(caseId))
    : appointments;

  return (
    <div className="appointments-list-container" style={caseId ? { marginTop: '30px' } : {}}>
      <div className="appointments-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <h2 style={{ margin: 0, fontSize: '1.2rem' }}>{caseId ? 'Case Appointments' : 'My Appointments'}</h2>
        {!caseId && (
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
        )}
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
            <div key={i} className="appointment-card-skeleton" style={{ marginBottom: '15px' }}>
              <Skeleton height={60} style={{ marginBottom: '10px' }} />
            </div>
          ))}
        </div>
      ) : filteredAppointments.length === 0 ? (
        <div className="empty-state" style={{ textAlign: 'center', padding: '30px', color: '#888', background: '#fcfcfc', borderRadius: '8px', border: '1px dashed #ddd' }}>
          <p>{caseId ? 'No appointments scheduled for this case.' : 'No appointments found.'}</p>
        </div>
      ) : (
        <div className={caseId ? "appointments-grid-compact" : "appointments-grid"} style={caseId ? { display: 'flex', flexDirection: 'column', gap: '15px' } : {}}>
          {filteredAppointments.map((appointment) => (
            <div key={appointment.id} className="appointment-card" style={{
              border: '1px solid #eee',
              borderRadius: '8px',
              padding: '15px',
              backgroundColor: '#fff',
              boxShadow: '0 2px 4px rgba(0,0,0,0.02)'
            }}>
              <div className="appointment-header" style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px' }}>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1rem' }}>
                    {getMeetingTypeIcon(appointment.meetingType)} {' '}
                    {userType === 'user' ? (
                      <Link to={`/lawyer/${appointment.lawyerId}`} className="profile-link" style={{ color: '#2c3e50', textDecoration: 'none', fontWeight: 'bold' }}>
                        {appointment.lawyerFullName || 'Lawyer'}
                      </Link>
                    ) : (
                      appointment.userFullName || 'Client'
                    )}
                  </h3>
                  <p className="appointment-date" style={{ margin: '4px 0 0 0', fontSize: '0.85rem', color: '#7f8c8d' }}>
                    {formatDateTime(appointment.appointmentDate)}
                  </p>
                </div>
                {getStatusBadge(appointment.status)}
              </div>

              <div className="appointment-details" style={{ fontSize: '0.9rem', color: '#34495e', marginBottom: '12px' }}>
                <p style={{ margin: '4px 0' }}><strong>Duration:</strong> {appointment.durationMinutes} minutes</p>
                <p style={{ margin: '4px 0' }}><strong>Type:</strong> {appointment.meetingType}</p>
                {appointment.description && (
                  <p style={{ margin: '8px 0', fontSize: '0.85rem', fontStyle: 'italic', color: '#7f8c8d' }}>"{appointment.description}"</p>
                )}
              </div>

              <div className="appointment-actions" style={{ display: 'flex', gap: '10px' }}>
                {userType === 'user' &&
                  (appointment.status === 'pending' || appointment.status === 'confirmed') && (
                    <>
                      <button
                        onClick={() => handleEditClick(appointment)}
                        className="edit-button-small"
                        style={{
                          padding: '6px 12px',
                          backgroundColor: '#3498db',
                          color: 'white',
                          border: 'none',
                          borderRadius: '4px',
                          cursor: 'pointer',
                          fontSize: '13px',
                          fontWeight: '600'
                        }}
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleCancel(appointment.id)}
                        className="cancel-button"
                        style={{
                          padding: '6px 12px',
                          backgroundColor: 'transparent',
                          color: '#e74c3c',
                          border: '1px solid #e74c3c',
                          borderRadius: '4px',
                          cursor: 'pointer',
                          fontSize: '13px',
                          fontWeight: '600'
                        }}
                      >
                        Cancel
                      </button>
                    </>
                  )}
                {userType === 'lawyer' && appointment.status === 'pending' && (
                  <button
                    onClick={() => handleConfirm(appointment.id)}
                    className="confirm-button"
                    style={{
                      padding: '6px 12px',
                      backgroundColor: '#27ae60',
                      color: 'white',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: 'pointer',
                      fontSize: '13px',
                      fontWeight: '600'
                    }}
                  >
                    Confirm
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {editingAppointment && (
        <div className="modal-overlay" style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 2000,
          padding: '20px'
        }}>
          <div className="modal-content" style={{
            backgroundColor: 'white',
            borderRadius: '12px',
            width: '100%',
            maxWidth: '600px',
            maxHeight: '90vh',
            overflowY: 'auto',
            position: 'relative',
            padding: '20px',
            boxShadow: '0 10px 25px rgba(0,0,0,0.2)'
          }}>
            <button
              onClick={() => setEditingAppointment(null)}
              style={{
                position: 'absolute',
                top: '15px',
                right: '15px',
                border: 'none',
                background: 'none',
                fontSize: '24px',
                cursor: 'pointer',
                color: '#666'
              }}
            >
              ×
            </button>
            <div style={{ marginTop: '10px' }}>
              <Booking
                userId={userId}
                appointment={editingAppointment}
                onBookingSuccess={handleEditSuccess}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default AppointmentsList;
