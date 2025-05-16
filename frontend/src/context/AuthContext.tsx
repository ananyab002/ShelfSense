import { createContext, PropsWithChildren, useEffect, useState } from "react";
import { AxiosError } from "axios";
import { LoginDataType } from "./type";
import api from "../api/globalApi";

interface LoginContextType {
    currentUser: LoginDataType | null,
    loginCurrentUser: (data: LoginDataType) => Promise<{ success: boolean }>;
    loginError: string | null
}

export const AuthContext = createContext<LoginContextType | undefined>(undefined);

export const AuthContextProvider = ({ children }: PropsWithChildren) => {

    const [currentUser, setCurrentUser] = useState<LoginDataType | null>(() => {
        try {
            const storedData = localStorage.getItem("userData");
            return storedData ? JSON.parse(storedData) : null;
        } catch (error) {
            console.log(error)
            localStorage.removeItem("userData");
            return null;
        }
    });
    const [loginError, setLoginError] = useState<string | null>(null);

    useEffect(() => {
        localStorage.setItem("userData", JSON.stringify(currentUser));
    }, [currentUser]);

    const loginCurrentUser = async (data: LoginDataType): Promise<{ success: boolean }> => {
        try {
            const response = await api.post("auth/login", {
                email: data.email,
                password: data.password
            });

            if (response.data) {
                console.log(response);
                localStorage.setItem("authToken", response.data.token);
                setCurrentUser(response.data.userData);
                return { success: true };
            }
        } catch (error: unknown) {
            if (error instanceof AxiosError) {
                if (error.response) {
                    if (error.response.status === 404) {
                        setLoginError("User not found");
                    }
                    if (error.response.status === 401) {
                        console.log("Inside catch");
                        setLoginError("Invalid email or password");
                    }
                } else {
                    setLoginError("Something went wrong. Please try again.");
                }
                console.error("Login error:", error);
            }
        }

        return { success: false };

    }

    return (
        <AuthContext.Provider value={{ currentUser, loginCurrentUser, loginError }}>{children}</AuthContext.Provider>
    )
}
