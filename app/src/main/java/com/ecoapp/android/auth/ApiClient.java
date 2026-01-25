package com.ecoapp.android.auth;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    // Backend su Render.com
    private static final String BASE_URL = "https://ecoapp-p5gp.onrender.com/";
    
    // Action per broadcast quando il token è invalido (401)
    public static final String ACTION_UNAUTHORIZED = "com.ecoapp.android.ACTION_UNAUTHORIZED";
    
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
            
            // Response interceptor per gestire 401 Unauthorized
            Interceptor unauthorizedInterceptor = new Interceptor() {
                @NonNull
                @Override
                public Response intercept(@NonNull Chain chain) throws IOException {
                    Response response = chain.proceed(chain.request());
                    if (response.code() == 401) {
                        // Token invalido o scaduto: pulisci e notifica
                        tokenManager.clearAll();
                        Intent intent = new Intent(ACTION_UNAUTHORIZED);
                        LocalBroadcastManager.getInstance(context.getApplicationContext())
                                .sendBroadcast(intent);
                    }
                    return response;
                }
            };

            // OkHttp client
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .addInterceptor(unauthorizedInterceptor)
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
    
    /**
     * Reset delle istanze statiche. Da chiamare dopo logout o 401.
     * Necessario per evitare che il vecchio interceptor con token invalido venga riusato.
     */
    public static void reset() {
        retrofit = null;
        authService = null;
    }
}
