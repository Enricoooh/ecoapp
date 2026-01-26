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

public class QuestFragment extends Fragment {

    private static final String TAG = "QuestFragment";

    //TAB
    private static int selectedTabPosition = 0; // 0: Global, 1: Ongoing, 2: Completed

    //ADAPTERS
    private GlobalQuestAdapter globalAdapter;
    private OngoingQuestAdapter2 ongoingAdapter;
    private CompletedQuestAdapter completedAdapter;

    //MAPPE
    private Map<Integer, Quest> allGlobalQuests = new HashMap<>();
    private Map<Integer, LocalQuest> localQuests = new HashMap<>();
    private Map<Integer, LocalQuest> filteredQuests = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quest, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //I 3 tab in alto
        TabLayout tabLayout = view.findViewById(R.id.tabLayoutQuests);

        //Per cambiare tab in caso di accettazione della quest
        getParentFragmentManager().setFragmentResultListener("questAccepted", this, (requestKey, bundle) -> {
            int targetTab = bundle.getInt("selectedTab", 0);
            selectedTabPosition = targetTab;
            TabLayout.Tab selectedTab = tabLayout.getTabAt(targetTab);
            if (selectedTab != null) {
                selectedTab.select();
            }
        });

        //Serve a ricordarsi in che tab si era prima
        TabLayout.Tab tab = tabLayout.getTabAt(selectedTabPosition);
        if (tab != null) {
            tab.select();
        }

        //La lista di quest
        RecyclerView recyclerView = view.findViewById(R.id.recyclerQuests);

        //Imposta la lista con un layout verticale
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Inizializziamo il listener del tab (già presente)
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTabPosition = tab.getPosition();
                applyFilter();
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
        Log.d(TAG, "Token usato: " + token);

        //Carica le Quest Globali
        apiService.getGlobalQuests(token).enqueue(new Callback<List<Quest>>() {
            @Override
            public void onResponse(@NonNull Call<List<Quest>> call, @NonNull Response<List<Quest>> response) {
                Log.d(TAG, "Response code: " + response.code());
                //Se la chiamata ha avuto successo
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Quest ricevute: " + response.body().size());
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

                    /*
                    //Aggiunge tutti gli elementi della lista all'interno della mappa
                    for (UserQuest newUserQuestElement: response.body()) {
                        //Controllo che l'elemento esista nelle quest globali
                        Quest globalElement = allGlobalQuests.get(newUserQuestElement.getQuestId());
                        if(globalElement != null){
                            //Creo un elemento di tipo LocalQuest da aggiungere alla mappa
                            LocalQuest localQuestElement = new LocalQuest(globalElement, newUserQuestElement);
                            //Aggiungo alla mappa
                            localQuests.put(newUserQuestElement.getQuestId(), localQuestElement);
                        }
                    }
                    */

                    for (UserQuest newUserQuestElement : response.body()) {
                        // Forziamo entrambi a int per essere sicuri al 100%
                        int idDalServer = newUserQuestElement.getQuestId();

                        // Recuperiamo la quest dal catalogo globale
                        Quest globalElement = allGlobalQuests.get(idDalServer);

                        /*
                        Quest globalElement = null;
                        //Doppio controllo per sicurezza
                        //Cerchiamo manualmente se la mappa contiene questa chiave
                        for (Integer key : allGlobalQuests.keySet()) {
                            if (key.intValue() == idDalServer) {
                                globalElement = allGlobalQuests.get(key);
                                break;
                            }
                        }
                        */

                        if (globalElement != null) {
                            LocalQuest localQuestElement = new LocalQuest(globalElement, newUserQuestElement);
                            localQuests.put(idDalServer, localQuestElement);
                            Log.d(TAG, "MATCH TROVATO! Inserita quest: " + idDalServer);
                        }
                        else {
                            Log.e(TAG, "NESSUN MATCH per ID: " + idDalServer + ". Mappa globale ha chiavi: " + allGlobalQuests.keySet());
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
        if (allGlobalQuests == null || allGlobalQuests.isEmpty()) {
            Log.e(TAG, "applyFilter: allGlobalQuests è vuoto o null!");
            return;
        }
        
        Log.d(TAG, "applyFilter: allGlobalQuests.size()=" + allGlobalQuests.size() + ", localQuests.size()=" + localQuests.size());

        //Resetto il filtro
        filteredQuests.clear();

        //Esce se non esiste la view
        if (getView() == null) return;

        RecyclerView recyclerView = getView().findViewById(R.id.recyclerQuests);

        switch (selectedTabPosition) {
            //Global quests - mostra SOLO le quest MAI iniziate
            case 0:{
                for(Map.Entry<Integer, Quest> globalElement : allGlobalQuests.entrySet()){
                    Integer globalElementId = globalElement.getKey();
                    LocalQuest localElement = localQuests.get(globalElementId);
                    
                    // Mostra solo se l'utente non ha MAI iniziato questa quest
                    if (localElement == null) {
                        // Nessun UserQuest entry - mai iniziata
                        filteredQuests.put(globalElementId, new LocalQuest(globalElement.getValue()));
                    }
                    //localElement.getActualProgress() == 0 -> tecnicamente inutile, ma lasciato per sicurezza
                    else if (localElement.getTimesCompleted() == 0
                               && !localElement.isCurrentlyActive()
                               && localElement.getActualProgress() == 0) {
                        // Ha entry ma non è mai stata realmente iniziata
                        filteredQuests.put(globalElementId, localElement);
                    }
                }
                
                Log.d(TAG, "TAB GLOBAL: filteredQuests.size()=" + filteredQuests.size());

                // Convertiamo LocalQuest filteredQuests.values() in una ArrayList<Quest>
                ArrayList<Quest> tempQuestList = new ArrayList<>();
                for (LocalQuest q : filteredQuests.values()) {
                    tempQuestList.add(new Quest(q));
                }

                //globalAdapter richiede una Lista di Quest
                globalAdapter = new GlobalQuestAdapter(tempQuestList, questId -> {
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

                Log.d("DEBUG_QUEST", "Elementi in localQuests: " + localQuests.size());
                for(Map.Entry<Integer, LocalQuest> localElement : localQuests.entrySet()){
                    Log.d("DEBUG_QUEST", "Quest ID: " + localElement.getKey() + " | Attiva: " + localElement.getValue().isCurrentlyActive());
                    //se è attiva al momento
                    if(localElement.getValue().isCurrentlyActive()){
                        filteredQuests.put(localElement.getKey(), localElement.getValue());
                    }
                }
                Log.d("DEBUG_QUEST", "Elementi filtrati per Ongoing: " + filteredQuests.size());

                // Passiamo la lista delle quest filtrate e la mappa/lista dei progressi
                ongoingAdapter = new OngoingQuestAdapter2(new ArrayList<LocalQuest>(filteredQuests.values()), questId -> {
                    // Logica al click: navighiamo verso il dettaglio "Ongoing"
                    Bundle bundle = new Bundle();
                    bundle.putInt("questId", questId);
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_questFragment_to_questOngoingDetailFragment, bundle);
                });

                recyclerView.setAdapter(ongoingAdapter);
                break;
            }

            //Completed quests
            case 2:{
                for(Map.Entry<Integer, LocalQuest> localElement : localQuests.entrySet()){
                    //se è stata completata almeno una volta E non è attualmente attiva
                    if(localElement.getValue().getTimesCompleted() > 0 
                       && !localElement.getValue().isCurrentlyActive()){
                        filteredQuests.put(localElement.getKey(), localElement.getValue());
                    }
                }
                Log.d("DEBUG_QUEST", "Elementi filtrati per Completed: " + filteredQuests.size());

                completedAdapter = new CompletedQuestAdapter(new ArrayList<>(filteredQuests.values()), questId -> {
                    // Click su quest completata - naviga per permettere Re-do
                    Bundle bundle = new Bundle();
                    bundle.putInt("questId", questId);
                    //bundle.putBoolean("isRedo", true); // Flag per indicare che è un re-do
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_questFragment_to_questCompletedDetailFragment, bundle);

                });

                recyclerView.setAdapter(completedAdapter);
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