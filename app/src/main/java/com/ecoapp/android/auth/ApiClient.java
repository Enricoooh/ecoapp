package com.ecoapp.android.auth;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    // Backend su Render.com
    private static final String BASE_URL = "https://ecoapp-p5gp.onrender.com/";
    
    private static Retrofit retrofit = null;
    private static AuthService authService = null;

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            // Logging interceptor per debug
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Auth interceptor per aggiungere JWT token
            TokenManager tokenManager = new TokenManager(context);
            AuthInterceptor authInterceptor = new AuthInterceptor(tokenManager);

            // OkHttp client
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            // Retrofit instance
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static AuthService getAuthService(Context context) {
        if (authService == null) {
            authService = getClient(context).create(AuthService.class);
        }
        return authService;
    }
}
