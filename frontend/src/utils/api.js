import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Add a request interceptor to include the auth token
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

export const audioApi = {
    upload: (formData, userId) => {
        // For FormData, let the browser set the Content-Type
        return api.post('/audio/upload', formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
            params: userId ? { userId } : {},
        });
    },
    getAll: () => api.get('/audio/all'),
    getById: (id) => api.get(`/audio/${id}`),
};

export const casesApi = {
    create: (caseData) => api.post('/cases/create', caseData),
    getById: (id) => api.get(`/cases/${id}`),
    getByUser: (userId) => api.get(`/cases/user/${userId}`),
    getByLawyer: (lawyerId) => api.get(`/cases/lawyer/${lawyerId}`),
    getUnassigned: () => api.get('/cases/unassigned'), // Fixed endpoint to match controller
    assignLawyer: (caseId, lawyerId) => api.post(`/cases/${caseId}/assign`, { lawyerId }),
    updateSolution: (caseId, solution) => api.put(`/cases/${caseId}/solution`, { solution }),
    updateStatus: (caseId, status) => api.put(`/cases/${caseId}/status`, { status }),
};

export const messagesApi = {
    send: (messageData) => api.post('/messages/send', messageData),
    getByCase: (caseId) => api.get(`/messages/case/${caseId}`),
    markRead: (messageId) => api.put(`/messages/${messageId}/read`),
};

export default api;
