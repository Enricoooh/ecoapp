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

                    //DEBUG
                    Log.d(TAG, "Ricevuti progressi per " + userLocalQuests.size() + " quest.");
                    for(UserQuest u : userLocalQuests) {
                        Log.d(TAG, "ID: " + u.getQuestId() + " - Attiva: " + u.isCurrentlyActive());
                    }

                    if (isAdded()) {
                        applyFilter();
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<UserQuest>> call, @NonNull Throwable t) {
                Log.e(TAG, "Errore UserQuest: " + t.getMessage());
            }
        });
    }

    private void applyFilter() {
        // 1. Controllo di sicurezza: se non abbiamo le quest globali, non possiamo filtrare
        if (allGlobalQuests == null || allGlobalQuests.isEmpty()) {
            return;
        }

        List<Quest> newList = new ArrayList<>();

        for (Quest gQuest : allGlobalQuests) {
            // Cerchiamo il progresso in modo ultra-sicuro
            UserQuest progress = null;
            for (UserQuest uq : userLocalQuests) {
                // Forziamo il confronto come stringhe per evitare errori tra int/long/string
                String gId = String.valueOf(gQuest.getId()).trim();
                String uId = String.valueOf(uq.getQuestId()).trim();

                if (gId.equals(uId)) {
                    progress = uq;
                    break;
                }
            }

            // LOGICA DI FILTRAGGIO BASATA SULLA TAB
            if (selectedTabPosition == 0) {
                // TAB GLOBAL: La quest DEVE sparire se è attiva
                // Se progress è null, non l'ha mai toccata -> la mostriamo
                if (progress == null) {
                    newList.add(gQuest);
                } else {
                    // Se esiste un progresso, la mostriamo SOLO se NON è attiva
                    if (!progress.isCurrentlyActive()) {
                        newList.add(gQuest);
                    }
                }
            }
            else if (selectedTabPosition == 1) {
                // TAB ONGOING: Mostra solo se attiva E non finita
                if (progress != null && progress.isCurrentlyActive() && progress.getTimesCompleted() == 0) {
                    newList.add(gQuest);
                }
            }
            else if (selectedTabPosition == 2) {
                // TAB COMPLETED: Mostra se finita almeno una volta
                if (progress != null && progress.getTimesCompleted() > 0) {
                    newList.add(gQuest);
                }
            }
        }

        // 2. AGGIORNAMENTO DIRETTO (Senza DiffUtil per ora, per isolare il bug)
        // Se nemmeno questo funziona, il problema è nei getter dei tuoi modelli Java
        filteredQuestList.clear();
        filteredQuestList.addAll(newList);

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        Log.d(TAG, "FILTRO APPLICATO - Tab: " + selectedTabPosition + " | Elementi: " + filteredQuestList.size());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Ogni volta che torniamo in questa schermata, scarichiamo i dati aggiornati
        // così vedremo la quest spostata in "Ongoing"
        loadDataFromServer();
    }
}