package com.example.ecoapp.quest_files;

import com.ecoapp.android.auth.models.Quest;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Header; // Assicurati che l'import sia questo di retrofit2
import retrofit2.http.Body;

/**
 * Interfaccia che definisce le rotte HTTP per le Quest.
 */
public interface QuestApiService {

    // Usa @Header di Retrofit, non quello di Volley
    @GET("api/quests")
    Call<List<Quest>> getGlobalQuests(@Header("Authorization") String token);

    @GET("api/user/quests")
    Call<List<com.ecoapp.android.auth.models.UserQuest>> getUserQuests(@Header("Authorization") String token);

    @POST("api/user/quests/update")
    Call<Map<String, Object>> updateQuestProgress(
            @Header("Authorization") String token,
            @Body Map<String, Object> body
    );

}