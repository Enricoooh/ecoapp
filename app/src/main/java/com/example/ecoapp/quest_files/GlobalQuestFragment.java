package com.example.ecoapp.quest_files;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ecoapp.android.auth.AuthManager;
import com.ecoapp.android.auth.models.Quest;
import com.example.ecoapp.R;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Gestisce la lista globale delle missioni.
 * Carica i dati da global_quests.json tramite il server.js.
 */
public class GlobalQuestFragment extends Fragment {

    private static final String TAG = "GlobalQuestFragment";
    private GlobalQuestAdapter adapter;
    private List<Quest> questList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Usa fragment_quest.xml che contiene la RecyclerView
        return inflater.inflate(R.layout.fragment_quest, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Configurazione RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.recyclerQuests);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Inizializzazione Adapter con logica di navigazione al dettaglio
        adapter = new GlobalQuestAdapter(questList, questId -> {
            Bundle bundle = new Bundle();
            bundle.putInt("questId", questId);
            Navigation.findNavController(view).navigate(R.id.action_questFragment_to_questDetailFragment, bundle);
        });
        recyclerView.setAdapter(adapter);

        // 2. Caricamento dati dal server
        loadQuestsFromServer();
    }

    private void loadQuestsFromServer() {
        // Configurazione Retrofit essenziale
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://ecoapp-p5gp.onrender.com") // IP speciale per localhost dall'emulatore
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        QuestApiService apiService = retrofit.create(QuestApiService.class);

        // Recupero del token (necessario perché server.js usa authenticateToken)
        String token = AuthManager.getInstance(requireContext()).getToken();
        if (token == null) return;

        // Chiamata GET /api/quests
        apiService.getGlobalQuests("Bearer " + token).enqueue(new Callback<List<Quest>>() {
            @Override
            public void onResponse(@NonNull Call<List<Quest>> call, @NonNull Response<List<Quest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Aggiornamento lista
                    questList.clear();
                    questList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "Quest caricate: " + questList.size());
                } else {
                    Log.e(TAG, "Errore server: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Quest>> call, @NonNull Throwable t) {
                Log.e(TAG, "Errore di rete: " + t.getMessage());
                if (isAdded()) {
                    Toast.makeText(getContext(), "Server non raggiungibile", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}