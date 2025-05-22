import axios from 'axios';

const baseURL = import.meta.env.VITE_API_URL || 'http://localhost:8080/';
//|| 'http://localhost:8080/';

const api = axios.create({
    baseURL: baseURL, // Adjust according to your backend
    headers: { 'Content-Type': 'application/json' }
});

export default api;
