package com.example.ecoapp.quest_files;

import com.google.android.material.tabs.TabLayout;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ecoapp.android.auth.AuthManager;
import com.ecoapp.android.auth.models.Quest;
import com.ecoapp.android.auth.models.UserQuest; // Assicurati di avere questo modello
import com.example.ecoapp.R;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GlobalQuestFragment extends Fragment {

    private static final String TAG = "GlobalQuestFragment";
    private GlobalQuestAdapter adapter;

    // Richiesti:
    private List<Quest> filteredQuestList = new ArrayList<>(); // Lista mostrata dall'adapter
    private List<Quest> allGlobalQuests = new ArrayList<>();  // Tutte le quest dal server
    private List<UserQuest> userLocalQuests = new ArrayList<>(); // Progressi utente (UserQuest model)

    private int selectedTabPosition = 0; // 0: Global, 1: Ongoing, 2: Completed

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quest, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TabLayout tabLayout = view.findViewById(R.id.tabLayoutQuests);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerQuests);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Inizializzazione Adapter con la lista FILTRATA
        adapter = new GlobalQuestAdapter(filteredQuestList, questId -> {
            Bundle bundle = new Bundle();
            bundle.putInt("questId", questId);
            Navigation.findNavController(view).navigate(R.id.action_questFragment_to_questDetailFragment, bundle);
        });
        recyclerView.setAdapter(adapter);

        // Listener per il cambio sezione
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTabPosition = tab.getPosition();
                applyFilter();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadDataFromServer();
    }

    private void loadDataFromServer() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://ecoapp-p5gp.onrender.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        QuestApiService apiService = retrofit.create(QuestApiService.class);
        String token = "Bearer " + AuthManager.getInstance(requireContext()).getToken();

        // 1. Carica le Quest Globali
        apiService.getGlobalQuests(token).enqueue(new Callback<List<Quest>>() {
            @Override
            public void onResponse(@NonNull Call<List<Quest>> call, @NonNull Response<List<Quest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allGlobalQuests.clear();
                    allGlobalQuests.addAll(response.body());

                    // 2. Carica i progressi dell'utente dopo aver ottenuto le globali
                    loadUserQuests(apiService, token);
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Quest>> call, @NonNull Throwable t) {
                Log.e(TAG, "Errore Global: " + t.getMessage());
            }
        });
    }

    private void loadUserQuests(QuestApiService apiService, String token) {
        apiService.getUserQuests(token).enqueue(new Callback<List<UserQuest>>() {
            @Override
            public void onResponse(@NonNull Call<List<UserQuest>> call, @NonNull Response<List<UserQuest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userLocalQuests.clear();
                    userLocalQuests.addAll(response.body());
                    applyFilter(); // Mostra i dati iniziali
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<UserQuest>> call, @NonNull Throwable t) {
                Log.e(TAG, "Errore UserQuest: " + t.getMessage());
            }
        });
    }

    private void applyFilter() {
        filteredQuestList.clear();

        for (Quest globalQuest : allGlobalQuests) {
            // Cerchiamo se l'utente ha un progresso per questa quest specifica
            UserQuest progress = null;
            for (UserQuest uq : userLocalQuests) {
                if (uq.getQuestId() == globalQuest.getId()) {
                    progress = uq;
                    break;
                }
            }

            if (selectedTabPosition == 0) {
                // SEZIONE GLOBAL: Mostra tutto il catalogo
                filteredQuestList.add(globalQuest);
            }
            else if (selectedTabPosition == 1) {
                // SEZIONE ONGOING: Presente in user_quest E times_completed == 0
                if (progress != null && progress.getTimesCompleted() == 0) {
                    filteredQuestList.add(globalQuest);
                }
            }
            else if (selectedTabPosition == 2) {
                // SEZIONE COMPLETED: times_completed > 0
                if (progress != null && progress.getTimesCompleted() > 0) {
                    filteredQuestList.add(globalQuest);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}