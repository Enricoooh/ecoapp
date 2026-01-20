package com.example.ecoapp.quest_files;

import com.ecoapp.android.auth.models.Quest;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header; // Assicurati che l'import sia questo di retrofit2

/**
 * Interfaccia che definisce le rotte HTTP per le Quest.
 */
public interface QuestApiService {

    // Usa @Header di Retrofit, non quello di Volley
    @GET("api/quests")
    Call<List<Quest>> getGlobalQuests(@Header("Authorization") String token);

    @GET("api/user/quests")
    Call<List<com.ecoapp.android.auth.models.UserQuest>> getUserQuests(@Header("Authorization") String token);
}