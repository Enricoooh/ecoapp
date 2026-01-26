package com.example.ecoapp.quest_files;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.ecoapp.android.auth.AuthManager;
import com.ecoapp.android.auth.models.Quest;
import com.ecoapp.android.auth.models.LocalQuest;
import com.ecoapp.android.auth.models.UserQuest;
import com.example.ecoapp.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CompletedQuestDetailFragment extends Fragment{
    //ATTRIBUTI
    private static final String TAG = "OngoingQuestDetailFragment.java";
    private QuestApiService apiService;
    private int questId;

    private LocalQuest localQuest;

    //COSTRUTTORE
    public CompletedQuestDetailFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quest_completed_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Setup Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://ecoapp-p5gp.onrender.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(QuestApiService.class);

        //Recupera QuestID
        Bundle args = getArguments();
        if (args != null) {
            questId = args.getInt("questId", -1);
            loadData(view, questId);
        }

        //Setup Listener
        Button buttonRedo = view.findViewById(R.id.buttonRedoQuest);
        buttonRedo.setOnClickListener(v -> showRedoConfirmation());
    }

    private void loadData(View view, int questId) {
        //Errore di accesso
        if (AuthManager.getInstance(requireContext()).getToken() == null) {
            Log.e("RetrofitError", "Token mancante");
            return;
        }

        //Salva il token
        String token = "Bearer " + AuthManager.getInstance(requireContext()).getToken();

        //Controlla se non ci sono errori con l'api
        if (apiService == null) {
            Log.e("RetrofitError", "ApiService non inizializzato");
            return;
        }

        // 1. Scarichiamo le Quest Globali (Quest)
        apiService.getGlobalQuests(token).enqueue(new Callback<List<Quest>>() {
            @Override
            public void onResponse(@NonNull Call<List<Quest>> call, @NonNull Response<List<Quest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Quest foundGlobal = null;
                    //Scorro tutte le quest globali
                    for (Quest q : response.body()) {
                        //Corrispondenza di ID con una quest globale
                        if (q.getId() == questId) {
                            foundGlobal = q;
                            break;
                        }
                    }

                    if (foundGlobal != null) {
                        // 2. Ora scarichiamo i progressi dell'utente (UserQuest)
                        // Usiamo una variabile final per poterla passare dentro il prossimo callback
                        final Quest finalGlobal = foundGlobal;

                        apiService.getUserQuests(token).enqueue(new Callback<List<UserQuest>>() {
                            @Override
                            public void onResponse(@NonNull Call<List<UserQuest>> call, @NonNull Response<List<UserQuest>> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    UserQuest foundUserProgress = null;
                                    //Scorro tutte le quest locali
                                    for (UserQuest uq : response.body()) {
                                        //Trova una corrispondenza
                                        if (uq.getQuestId() == questId) {
                                            foundUserProgress = uq;
                                            break;
                                        }
                                    }

                                    // 3. Fondo i dati in LocalQuest
                                    if (foundUserProgress != null) {
                                        localQuest = new LocalQuest(finalGlobal, foundUserProgress);
                                        // 4. Carichiamo la UI
                                        loadGlobalData(view);
                                    }
                                    else {
                                        // CASO FALLIMENTO: La missione non è tra quelle dell'utente
                                        Log.e(TAG, "Missione " + questId + " non trovata nei progressi utente.");
                                        Toast.makeText(getContext(), "Errore: Dati missione non trovati", Toast.LENGTH_SHORT).show();

                                        // Opzionale: chiudi il fragment e torna alla lista
                                        Navigation.findNavController(view).popBackStack();
                                    }
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<List<UserQuest>> call, @NonNull Throwable t) {
                                Log.e(TAG, "Errore caricamento UserQuests", t);
                            }
                        });
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Quest>> call, @NonNull Throwable t) {
                Log.e(TAG, "Errore caricamento GlobalQuests", t);
            }
        });
    }

    private void loadGlobalData(View v) {
        //Riferimenti agli elementi del layout
        ImageView img = v.findViewById(R.id.questImage);
        TextView txtName = v.findViewById(R.id.txtQuestName);
        TextView txtType = v.findViewById(R.id.txtQuestType);
        TextView txtDesc = v.findViewById(R.id.txtQuestDescription);

        TextView txtTimesCompleted = v.findViewById(R.id.txtQuestTimesCompleted);

        TextView txtCO2 = v.findViewById(R.id.txtQuestCO2saved);
        TextView txtTotalCO2 = v.findViewById(R.id.txtQuestTotalCO2saved);

        TextView txtReward = v.findViewById(R.id.txtQuestRewardPoints);
        TextView txtTotalReward = v.findViewById(R.id.txtQuestTotalRewardPoints);

        //Impostazione immagine
        if(localQuest.getQuestImageResourceId(getContext()) != 0)
            img.setImageResource(localQuest.getQuestImageResourceId(getContext()));

        //Impostazione ""Stringhe""
        txtName.setText(localQuest.getName());
        txtType.setText(localQuest.getType());
        txtDesc.setText(localQuest.getDescription());

        //Impostazione int
        int timesCompleted = localQuest.getTimesCompleted();
        txtTimesCompleted.setText(getString(R.string.quest_times_completed, timesCompleted));

        txtReward.setText(getString(R.string.quest_reward_points, localQuest.getRewardPoints()));
        txtTotalReward.setText(getString(R.string.quest_reward_points, localQuest.getRewardPoints() * timesCompleted));

        //Impostazione double
        double co2Value = localQuest.getCO2Saved();
        double totalCo2Value = co2Value * timesCompleted;
        txtCO2.setText(getString(R.string.quest_CO2_saved, co2Value));
        txtTotalCO2.setText(getString(R.string.quest_CO2_saved, totalCo2Value));

        //Metodo per gli EU goals
        setupEuGoals(v, localQuest);
    }

    private void showRedoConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Ricomincia Quest")
                .setMessage("Vuoi davvero iniziare nuovamente questa quest?")
                .setPositiveButton("Sì, accetta", (dialog, which) -> sendRedoRequestToServer())
                .setNegativeButton("Annulla", null)
                .show();
    }

    private void sendRedoRequestToServer() {
        String token = "Bearer " + AuthManager.getInstance(requireContext()).getToken();

        if (questId == -1 || apiService == null) return;

        // Creiamo il body con la tua logica di "SET"
        Map<String, Object> body = new HashMap<>();
        body.put("questId", questId);
        body.put("actual_progress", 0);        // Forziamo il valore iniziale a 0
        body.put("is_currently_active", true); // Diciamo chiaramente cosa vogliamo settare

        // Una sola chiamata, zero problemi di "chi arriva prima"
        apiService.setQuestParameters(token, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Quest Ricominciata", Toast.LENGTH_SHORT).show();

                    // Notifica il cambio tab al Fragment principale
                    Bundle result = new Bundle();
                    result.putInt("selectedTab", 1); // 1 = Ongoing
                    getParentFragmentManager().setFragmentResult("questAccepted", result);

                    //Torna indietro
                    Navigation.findNavController(requireView()).popBackStack();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Errore di rete", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupEuGoals(View view, Quest quest) {
        LinearLayout container = view.findViewById(R.id.containerEuGoals);
        if (container == null) return;

        container.removeAllViews();

        // CHIAMATA AL METODO DELLA CLASSE QUEST
        int[] resIdArray = quest.getEuGoalsResourceIds(requireContext());

        for (int resId : resIdArray) {
            if (resId != 0) { // Se l'immagine esiste
                ImageView imageView = new ImageView(requireContext());

                // Impostazioni dimensioni (45dp)
                int size = (int) (45 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                params.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);

                imageView.setLayoutParams(params);
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageView.setImageResource(resId);

                container.addView(imageView);
            }
        }
    }
}
