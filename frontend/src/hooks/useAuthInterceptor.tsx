// In a custom hook or context
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/globalApi';

export const useAuthInterceptor = () => {
    const navigate = useNavigate();

    useEffect(() => {
        const token = localStorage.getItem('authToken'); // Get token from storage
        console.log(token)
        if (!token) return;
        const interceptor = api.interceptors.response.use(
            response => response,
            error => {
                console.log(error)
                if (error.response?.status === 401) {
                    let authErrorMessage = 'Your session has expired. Please log in again.';

                    if (error.response?.data?.message === 'Token expired') {
                        authErrorMessage = 'Your login session has expired. Please log in again to continue.';
                    } else if (error.response?.data?.message === 'Token invalid') {
                        authErrorMessage = 'Invalid authentication. Please log in again.';
                    }

                    console.log('Auth error detected:', authErrorMessage);

                    localStorage.removeItem('userData');
                    localStorage.removeItem('authToken');

                    navigate('/Messenger-typescript-frontend/', { state: { from: window.location.pathname, authError: authErrorMessage } });
                }
                return Promise.reject(error);
            }
        );

        return () => {
            api.interceptors.request.eject(interceptor);
            api.interceptors.response.eject(interceptor);
        };
    }, [navigate]);
};