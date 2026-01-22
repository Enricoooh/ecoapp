package com.ecoapp.android.auth;

import com.ecoapp.android.auth.models.AuthResponse;
import com.ecoapp.android.auth.models.LoginRequest;
import com.ecoapp.android.auth.models.RegisterRequest;
import com.ecoapp.android.auth.models.User;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface AuthService {
    
    @POST("/api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);
    
    @POST("/api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);
    
    @GET("/api/user/profile")
    Call<User> getProfile();

    @GET("/api/user/profile/{id}")
    Call<User> getFriendProfile(@Path("id") String userId);

    @PUT("/api/user/profile")
    Call<User> updateProfile(@Body User user);

    @GET("/api/user/friends")
    Call<List<User>> getFriends();

    @POST("/api/user/friends/request")
    Call<Map<String, Object>> sendFriendRequest(@Body Map<String, String> body);

    @GET("/api/user/friends/requests")
    Call<List<User>> getPendingRequests();

    @POST("/api/user/friends/respond")
    Call<Map<String, Object>> respondToRequest(@Body Map<String, String> body);

    @POST("/api/user/friends/remove")
    Call<Map<String, Object>> removeFriend(@Body Map<String, String> body);
}
