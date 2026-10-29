"use client";

import {
    createContext,
    useCallback,
    useContext,
    useEffect,
    useState,
    type ReactNode,
} from "react";
import {
    getAccessToken,
    getCurrentUserId,
    logout,
} from "@/lib/auth";

type AuthContextType = {
    userId: number | null;
    isLoggedIn: boolean;
    isReady: boolean;
    refreshAuth: () => void;
    signOut: () => void;
};

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [userId, setUserId] = useState<number | null>(null);
    const [isReady, setIsReady] = useState(false);

    const refreshAuth = useCallback(() => {
        const token = getAccessToken();
        setUserId(token ? getCurrentUserId() : null);
        setIsReady(true);
    }, []);

    useEffect(() => {
        const timeout = window.setTimeout(refreshAuth, 0);
        return () => window.clearTimeout(timeout);
    }, [refreshAuth]);

    const signOut = useCallback(() => {
        logout();
        setUserId(null);
    }, []);

    return (
        <AuthContext.Provider
            value={{
                userId,
                isLoggedIn: userId !== null,
                isReady,
                refreshAuth,
                signOut,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);

    if (!context) {
        throw new Error("useAuth must be used within AuthProvider");
    }

    return context;
}