import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { setToken, removeToken } from '../utils/auth';
import './Login.css';

const API_BASE_URL = 'http://localhost:8080/api/auth';

function UserLogin() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    // Ensure we start with a clean slate
    removeToken();

    if (!username || !password) {
      setError('Please enter both username and password');
      setLoading(false);
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/user/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, password }),
      });

      let data;
      try {
        data = await response.json();
      } catch (jsonError) {
        console.error('Failed to parse response as JSON:', jsonError);
        setError('Server returned an invalid response. Please try again.');
        setLoading(false);
        return;
      }

      if (response.ok && data.success) {
        // Store JWT token and user session
        if (data.token) {
          setToken(data.token);
        }
        localStorage.setItem('userType', 'user');
        localStorage.setItem('username', data.username || username);
        localStorage.setItem('fullName', data.fullName || '');
        localStorage.setItem('userId', data.id || '');
        toast.success('Login successful!');
        navigate('/user-dashboard');
      } else {
        const errorMsg = data.message || 'Invalid username or password';
        setError(errorMsg);
        toast.error(errorMsg);
      }
    } catch (err) {
      console.error('Login error:', err);
      setError('Error connecting to server. Please make sure the backend is running on http://localhost:8080');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <h1 className="login-title">User Login</h1>
        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label htmlFor="username">Username</label>
            <input
              type="text"
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Enter your username"
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              type="password"
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
              required
            />
          </div>
          {error && <div className="error-message">{error}</div>}
          <button type="submit" className="login-button" disabled={loading}>
            {loading ? 'Logging in...' : 'Login'}
          </button>
        </form>
        <div className="login-footer">
          <p>Don't have an account? <Link to="/user-register">Register here</Link></p>
          <p style={{ marginTop: '10px' }}>Are you a lawyer? <Link to="/lawyer-login">Login here</Link></p>
        </div>
      </div>
    </div>
  );
}

export default UserLogin;

