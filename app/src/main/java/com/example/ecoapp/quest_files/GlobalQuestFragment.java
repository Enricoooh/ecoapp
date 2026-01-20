package com.example.ecoapp.quest_files;

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
import com.ecoapp.android.auth.models.LocalQuest;
import com.ecoapp.android.auth.models.UserQuest;
import com.example.ecoapp.R;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GlobalQuestFragment extends Fragment {

    private static final String TAG = "GlobalQuestFragment";
    private GlobalQuestAdapter globalAdapter; // Rinominiamo il vecchio adapter
    private OngoingQuestAdapter2 ongoingAdapter; // Il nuovo adapter per le ongoing

    // Richiesti:
    // Prima: private List<UserQuest> userLocalQuests = new ArrayList<>();
    // Dopo: Mappa che associa l'ID della Quest al suo oggetto Progresso
    private Map<Integer, Quest> allGlobalQuests = new HashMap<>();
    private Map<Integer, LocalQuest> localQuests = new HashMap<>();
    private Map<Integer, LocalQuest> filteredQuests = new HashMap<>();


    private int selectedTabPosition = 0; // 0: Global, 1: Ongoing, 2: Completed

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quest, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //I 3 tab in alto
        TabLayout tabLayout = view.findViewById(R.id.tabLayoutQuests);

        //La lista di quest
        RecyclerView recyclerView = view.findViewById(R.id.recyclerQuests);

        //Imposta la lista con un layout verticale
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Inizializziamo il listener del tab (già presente)
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTabPosition = tab.getPosition();
                applyFilter(); // Questo metodo ora cambierà anche l'adapter
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        loadDataFromServer();
    }

    private void loadDataFromServer() {
        //PARTE DELLE QUEST GLOBALI
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://ecoapp-p5gp.onrender.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        QuestApiService apiService = retrofit.create(QuestApiService.class);
        String token = "Bearer " + AuthManager.getInstance(requireContext()).getToken();

        //Carica le Quest Globali
        apiService.getGlobalQuests(token).enqueue(new Callback<List<Quest>>() {
            @Override
            public void onResponse(@NonNull Call<List<Quest>> call, @NonNull Response<List<Quest>> response) {
                //Se la chiamata ha avuto successo
                if (response.isSuccessful() && response.body() != null) {
                    //Svuota la precendente mappa
                    allGlobalQuests.clear();

                    //Aggiunge tutti gli elementi della lista all'interno della mappa
                    for (Quest newGlobalElement: response.body()) {
                        allGlobalQuests.put(newGlobalElement.getId(), newGlobalElement);
                    }

                    //PARTE DELLE QUEST LOCALI
                    //Carica i progressi dell'utente dopo aver ottenuto le globali
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
                //Se la chiamata ha avuto successo
                if (response.isSuccessful() && response.body() != null) {
                    //svuota la precedente mappa
                    localQuests.clear();

                    //Aggiunge tutti gli elementi della lista all'interno della mappa
                    for (UserQuest newUserQuestElement: response.body()) {
                        //Controllo che l'elemento esista nelle quest globali
                        Quest globalElement = allGlobalQuests.get(newUserQuestElement.getQuestId());
                        if(globalElement != null){
                            //Creo un elemento di tipo LocalQuest da aggiungere alla mappa
                            LocalQuest localQuestElement = new LocalQuest(globalElement, newUserQuestElement);
                            //Aggiungo alla mappa
                            localQuests.put(localQuestElement.getId(), localQuestElement);
                        }
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
        //Se il server non ha inviato le quest è inutile filtrarle
        if (allGlobalQuests == null || allGlobalQuests.isEmpty()) return;

        //Resetto il filtro
        filteredQuests.clear();

        View view = getView();
        if (view == null) return;
        RecyclerView recyclerView = getView().findViewById(R.id.recyclerQuests);

        switch (selectedTabPosition) {
            //Global quests
            case 0:{
                Integer globalQuestId;
                for(Map.Entry<Integer, Quest> globalElement : allGlobalQuests.entrySet()){
                    globalQuestId = globalElement.getKey();
                    //se esiste una quest in allGlobalQuests ma non esiste in localQuests allora la aggiungo a
                    if(!localQuests.containsKey(globalQuestId)){
                        filteredQuests.put(globalQuestId, new LocalQuest(globalElement.getValue()));
                    }
                }

                // Convertiamo filteredQuests.values() in una ArrayList
                globalAdapter = new GlobalQuestAdapter(new ArrayList<>(filteredQuests.values()), questId -> {
                    Bundle bundle = new Bundle();
                    bundle.putInt("questId", questId);
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_questFragment_to_questGlobalDetailFragment, bundle);
                });

                recyclerView.setAdapter(globalAdapter);
                break;
            }

            //Ongoing quests
            case 1:{
                /*
                for(Map.Entry<Integer, LocalQuest> localElement : localQuests.entrySet()){
                    //se è attiva al momento
                    if(localElement.getValue().isCurrentlyActive()){
                        filteredQuests.put(localElement.getKey(), localElement.getValue());
                    }
                }
                recyclerView.setAdapter(ongoingAdapter);
                */
                break;
            }

            //Completed quests
            case 2:{
                /*
                for(Map.Entry<Integer, LocalQuest> localElement : localQuests.entrySet()){
                    //se è stata completata almeno una volta e non è attualmente attiva
                    if(localElement.getValue().getTimesCompleted() > 0 && !localElement.getValue().isCurrentlyActive()){
                        filteredQuests.put(localElement.getKey(), localElement.getValue());
                    }
                }
                //recyclerView.setAdapter(ongoingAdapter); da cambiare adapter e aggiungere completedAdapter
                */
                break;
            }

            default:
                //error
                break;
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        // Ogni volta che torniamo in questa schermata, scarica i dati aggiornati
        // così vedremo la quest spostata in "Ongoing"
        loadDataFromServer();
    }
}