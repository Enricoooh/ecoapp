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

    private String searchQuery = ""; // Memorizza il testo cercato

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

        //Barra di ricerca
        androidx.appcompat.widget.SearchView searchView = view.findViewById(R.id.searchQuests);
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchQuery = newText.toLowerCase().trim();
                applyFilter(); // Riesegue il filtro ogni volta che il testo cambia
                return true;
            }
        });

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
        // 1. CONTROLLO DI SICUREZZA INIZIALE
        // Se la mappa globale è vuota (es. server lento), inutile procedere
        if (allGlobalQuests == null || allGlobalQuests.isEmpty()) {
            Log.e(TAG, "applyFilter: Catalogo globale non ancora caricato!");
            return;
        }

        // Recupero del riferimento al RecyclerView dal layout
        if (getView() == null) return;
        RecyclerView recyclerView = getView().findViewById(R.id.recyclerQuests);

        // 2. RESET DEI DATI FILTRATI
        filteredQuests.clear();

        // 3. LOGICA DI FILTRAGGIO (Basata sulla Tab selezionata)
        switch (selectedTabPosition) {
            // --- TAB GLOBALI: Mostra solo le quest MAI iniziate ---
            case 0: {
                for (Map.Entry<Integer, Quest> entry : allGlobalQuests.entrySet()) {
                    Integer id = entry.getKey();
                    Quest globalData = entry.getValue();

                    // Verifica che l'utente ha già un record per questa quest
                    LocalQuest userLocal = localQuests.get(id);

                    // Una quest è considerata "Globale/Disponibile" se:
                    // a) Non esiste proprio nei progressi utente (null)
                    // b) Oppure esiste ma non è attiva e non è mai stata completata (times == 0)
                    boolean isNew = (userLocal == null) ||
                            (userLocal.getTimesCompleted() == 0 && !userLocal.isCurrentlyActive());

                    if (isNew) {
                        // CONTROLLO RICERCA: Applichiamo il filtro stringa (nome o tipo)
                        if (isQuestMatchingSearch(globalData.getName(), globalData.getType())) {
                            // Se non esiste localmente, la creiamo per l'adapter
                            filteredQuests.put(id, userLocal != null ? userLocal : new LocalQuest(globalData));
                        }
                    }
                }

                // Trasformiamo i risultati per l'adapter GlobalQuestAdapter
                ArrayList<Quest> globalList = new ArrayList<>();
                for (LocalQuest lq : filteredQuests.values()) {
                    globalList.add(new Quest(lq)); // Convertiamo in oggetto Quest base
                }

                // Aggiorniamo la UI con l'adapter per le quest Globali
                globalAdapter = new GlobalQuestAdapter(globalList, questId -> {
                    Bundle bundle = new Bundle();
                    bundle.putInt("questId", questId);
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_questFragment_to_questGlobalDetailFragment, bundle);
                });
                recyclerView.setAdapter(globalAdapter);
                break;
            }

            // --- TAB IN CORSO: Mostra solo le quest attive ---
            case 1: {
                for (LocalQuest lq : localQuests.values()) {
                    // Una quest è "In corso" se il flag isCurrentlyActive è true sul DB
                    if (lq.isCurrentlyActive()) {
                        // CONTROLLO RICERCA: Applica il filtro stringa
                        if (isQuestMatchingSearch(lq.getName(), lq.getType())) {
                            filteredQuests.put(lq.getId(), lq);
                        }
                    }
                }

                // Aggiorna la UI con l'adapter per le quest Ongoing
                ongoingAdapter = new OngoingQuestAdapter2(new ArrayList<>(filteredQuests.values()), questId -> {
                    Bundle bundle = new Bundle();
                    bundle.putInt("questId", questId);
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_questFragment_to_questOngoingDetailFragment, bundle);
                });
                recyclerView.setAdapter(ongoingAdapter);
                break;
            }

            // --- TAB COMPLETATE: Mostra quest finite almeno una volta ---
            case 2: {
                for (LocalQuest lq : localQuests.values()) {
                    // Una quest è "Completata" se è stata finita > 0 volte e non è attiva ora
                    if (lq.getTimesCompleted() > 0 && !lq.isCurrentlyActive()) {
                        // CONTROLLO RICERCA: Applica il filtro stringa
                        if (isQuestMatchingSearch(lq.getName(), lq.getType())) {
                            filteredQuests.put(lq.getId(), lq);
                        }
                    }
                }

                // Aggiorna la UI con l'adapter per le quest Completate
                completedAdapter = new CompletedQuestAdapter(new ArrayList<>(filteredQuests.values()), questId -> {
                    Bundle bundle = new Bundle();
                    bundle.putInt("questId", questId);
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_questFragment_to_questCompletedDetailFragment, bundle);
                });
                recyclerView.setAdapter(completedAdapter);
                break;
            }

            default:
                Log.e(TAG, "applyFilter: Posizione tab non valida!");
                break;
        }

        // 4. LOG DI DEBUG FINALE
        // Utile per capire se la ricerca ha prodotto risultati nel Logcat
        Log.d(TAG, "Filtro applicato. Tab: " + selectedTabPosition +
                " | Query: '" + searchQuery + "' | Risultati: " + filteredQuests.size());
    }

    // Metodo helper per la ricerca
    private boolean isQuestMatchingSearch(String name, String type) {
        if (searchQuery.isEmpty()) return true;
        return (name != null && name.toLowerCase().contains(searchQuery)) ||
                (type != null && type.toLowerCase().contains(searchQuery));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Ogni volta che torniamo in questa schermata, scarica i dati aggiornati
        // così vedremo la quest spostata in "Ongoing"
        loadDataFromServer();
    }
}