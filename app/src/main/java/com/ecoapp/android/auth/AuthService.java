package com.ecoapp.android.auth;

import com.ecoapp.android.auth.models.AuthResponse;
import com.ecoapp.android.auth.models.LoginRequest;
import com.ecoapp.android.auth.models.RegisterRequest;
import com.ecoapp.android.auth.models.User;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface AuthService {
    
    @POST("/api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);
    
    @POST("/api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);
    
    @GET("/api/user/profile")
    Call<User> getProfile();
}
