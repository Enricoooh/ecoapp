package com.example.ecoapp.quest_files;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.ecoapp.R;
import com.ecoapp.android.auth.models.Quest;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class QuestGlobalDetailFragment extends Fragment {

    public QuestGlobalDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_quest_global_detail, container, false);

        Bundle args = getArguments();
        if (args != null) {
            int questId = args.getInt("questId", 0);
            // Avviamo il caricamento dal server
            loadQuestDetails(v, questId);
        }
        return v;
    }

    private void loadQuestDetails(View v, int questId) {
        // 1. Recupera il token dall'AuthManager
        com.ecoapp.android.auth.AuthManager authManager = com.ecoapp.android.auth.AuthManager.getInstance(getContext());
        String token = authManager.getToken();

        if (token == null) {
            Log.e("RetrofitError", "Token mancante");
            return;
        }

        String authToken = "Bearer " + token;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://ecoapp-p5gp.onrender.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        QuestApiService apiService = retrofit.create(QuestApiService.class);

        // 2. Passa authToken come argomento a getGlobalQuests()
        apiService.getGlobalQuests(authToken).enqueue(new Callback<List<Quest>>() {
            @Override
            public void onResponse(@NonNull Call<List<Quest>> call, @NonNull Response<List<Quest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Quest foundQuest = null;
                    for (Quest q : response.body()) {
                        if (q.getId() == questId) {
                            foundQuest = q;
                            break;
                        }
                    }

                    if (foundQuest != null) {
                        populateUI(v, foundQuest);
                    }
                } else {
                    Log.e("RetrofitError", "Errore server: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Quest>> call, @NonNull Throwable t) {
                Log.e("RetrofitError", "Errore connessione: " + t.getMessage());
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Errore connessione server", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void populateUI(View v, Quest quest) {
        // Riferimenti agli elementi del layout
        ImageView img = v.findViewById(R.id.questImage);
        TextView txtName = v.findViewById(R.id.txtQuestName);
        TextView txtType = v.findViewById(R.id.txtQuestType);
        TextView txtDesc = v.findViewById(R.id.txtQuestDescription);
        TextView txtReward = v.findViewById(R.id.txtQuestRewardPoints);
        LinearLayout euContainer = v.findViewById(R.id.containerEuGoals);

        // Impostazione dati dall'oggetto Quest
        img.setImageResource(quest.getQuestImageResourceId(getContext()));
        txtName.setText(quest.getName());
        txtType.setText(quest.getType());
        txtDesc.setText(quest.getDescription());

        // Utilizzo della stringa reward_label (Risolve il Warning)
        txtReward.setText(getString(R.string.quest_reward_points, quest.getRewardPoints()));

        // Icone EU Goals (Aggiunta dinamica)
        if (quest.getImagesEuGoals() != null) {
            euContainer.removeAllViews(); // Pulisce icone precedenti

            //Recupero gli ID numerici delle risorse partendo dai nomi (stringhe)
            int[] euResIds = quest.getEuGoalsResourceIds(getContext());

            for (int resId : euResIds) {
                if (resId != 0) { // Se la risorsa esista effettivamente
                    ImageView iv = new ImageView(getContext());

                    // Definisce la dimensione dell'icona (es. 120x120 pixel)
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(120, 120);
                    lp.setMargins(0, 0, 16, 0); // Margine a destra per distanziare le icone

                    iv.setLayoutParams(lp);
                    iv.setImageResource(resId);

                    euContainer.addView(iv);
                }
            }
        }
    }
}