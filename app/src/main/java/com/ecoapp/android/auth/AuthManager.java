package com.ecoapp.android.auth;

import android.content.Context;

import com.ecoapp.android.auth.models.User;

public class AuthManager {
    private static AuthManager instance;
    private TokenManager tokenManager;
    private User currentUser;

    private AuthManager(Context context) {
        tokenManager = new TokenManager(context);
    }

    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context.getApplicationContext());
        }
        return instance;
    }

    public boolean isAuthenticated() {
        return tokenManager.hasToken();
    }

    public void saveAuthData(String token, User user) {
        tokenManager.saveToken(token);
        tokenManager.saveUserId(user.getId());
        tokenManager.saveUserEmail(user.getEmail());
        tokenManager.saveUserName(user.getName());
        currentUser = user;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public String getToken() {
        return tokenManager.getToken();
    }

    public String getUserId() {
        return tokenManager.getUserId();
    }

    public String getUserEmail() {
        return tokenManager.getUserEmail();
    }

    public String getUserName() {
        return tokenManager.getUserName();
    }

    public void logout() {
        tokenManager.clearAll();
        currentUser = null;
    }
}
