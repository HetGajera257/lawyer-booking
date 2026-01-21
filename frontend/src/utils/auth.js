// Auth utility functions for JWT token management

export const getToken = () => {
  return localStorage.getItem('token');
};

export const setToken = (token) => {
  localStorage.setItem('token', token);
};

export const removeToken = () => {
  localStorage.clear();
};

export const getAuthHeaders = () => {
  const token = getToken();
  return {
    'Content-Type': 'application/json',
    ...(token && { 'Authorization': `Bearer ${token}` })
  };
};

export const isAuthenticated = () => {
  return !!getToken();
};

export const getUserType = () => {
  return localStorage.getItem('userType');
};

export const getUserId = () => {
  const userType = getUserType();
  if (userType === 'user') {
    return localStorage.getItem('userId');
  } else if (userType === 'lawyer') {
    return localStorage.getItem('lawyerId');
  }
  return null;
};

