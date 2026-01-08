import React, { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { lawyersApi } from '../utils/api';

const LawyerProfile = ({ lawyerId: propLawyerId }) => {
  const { id: paramLawyerId } = useParams();
  const lawyerId = propLawyerId || paramLawyerId;
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchProfile = useCallback(async () => {
    try {
      const response = await lawyersApi.getProfile(lawyerId);
      setProfile(response.data);
    } catch (err) {
      console.error(`Error fetching lawyer profile for ID ${lawyerId}:`, err.response?.status, err.message);
      if (err.response?.status === 401 || err.response?.status === 403) {
        console.warn('Profile access restricted. Check SecurityConfig.');
      }
    } finally {
      setLoading(false);
    }
  }, [lawyerId]);

  useEffect(() => {
    if (lawyerId) {
      setLoading(true);
      setProfile(null);
      fetchProfile();
    }
  }, [lawyerId, fetchProfile]);

  if (loading) return <div>Loading profile...</div>;
  if (!profile) return <div>Lawyer profile not found.</div>;

  const styles = {
    container: {
      padding: '24px',
      backgroundColor: '#ffffff',
      borderRadius: '12px',
      boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
      maxWidth: '600px',
      margin: '20px auto',
      fontFamily: "'Inter', sans-serif"
    },
    header: {
      display: 'flex',
      alignItems: 'center',
      gap: '20px',
      marginBottom: '24px'
    },
    avatar: {
      width: '80px',
      height: '80px',
      borderRadius: '50%',
      backgroundColor: '#e3f2fd',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      fontSize: '32px',
      color: '#1976d2',
      fontWeight: 'bold'
    },
    name: {
      margin: 0,
      fontSize: '24px',
      color: '#2c3e50'
    },
    specialization: {
      color: '#1976d2',
      fontWeight: 'bold',
      marginTop: '4px'
    },
    grid: {
      display: 'grid',
      gridTemplateColumns: 'repeat(2, 1fr)',
      gap: '20px',
      marginTop: '24px',
      borderTop: '1px solid #eee',
      paddingTop: '20px'
    },
    infoBox: {
      padding: '12px',
      backgroundColor: '#f8f9fa',
      borderRadius: '8px'
    },
    label: {
      fontSize: '12px',
      color: '#7f8c8d',
      textTransform: 'uppercase',
      letterSpacing: '0.5px',
      marginBottom: '4px'
    },
    value: {
      fontSize: '16px',
      color: '#2c3e50',
      fontWeight: '600'
    }
  };

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <div style={styles.avatar}>
          {profile.profilePhotoUrl ? (
            <img src={profile.profilePhotoUrl} alt={profile.fullName} style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }} />
          ) : (
            profile.fullName.charAt(0)
          )}
        </div>
        <div>
          <h2 style={styles.name}>{profile.fullName}</h2>
          <div style={styles.specialization}>{profile.specialization || 'Legal Expert'}</div>
          <div style={{ color: '#f39c12', marginTop: '4px', fontSize: '14px' }}>
            {'★'.repeat(Math.round(profile.rating || 5))}
            <span style={{ color: '#7f8c8d', marginLeft: '5px' }}>({profile.completedCasesCount || 0} cases)</span>
          </div>
        </div>
      </div>

      <div style={styles.grid}>
        <div style={styles.infoBox}>
          <div style={styles.label}>Experience</div>
          <div style={styles.value}>{profile.yearsOfExperience || '5+'} Years</div>
        </div>
        <div style={styles.infoBox}>
          <div style={styles.label}>Languages</div>
          <div style={styles.value}>{profile.languagesKnown || 'English, Gujarati'}</div>
        </div>
        <div style={styles.infoBox}>
          <div style={styles.label}>Bar Number</div>
          <div style={styles.value}>{profile.barNumber}</div>
        </div>
        <div style={styles.infoBox}>
          <div style={styles.label}>Availability</div>
          <div style={styles.value}>{profile.availabilityInfo || 'Mon-Fri, 9AM-6PM'}</div>
        </div>
      </div>

      <div style={{ marginTop: '24px', paddingTop: '20px', borderTop: '1px solid #eee' }}>
        <div style={styles.label}>Contact Information</div>
        <div style={{ ...styles.value, marginTop: '8px', color: '#1976d2' }}>{profile.email}</div>
      </div>
    </div>
  );
};

export default LawyerProfile;

