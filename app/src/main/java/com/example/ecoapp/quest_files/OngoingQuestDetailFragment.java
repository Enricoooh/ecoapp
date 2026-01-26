package com.example.ecoapp.quest_files;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.ecoapp.android.auth.AuthManager;
import com.ecoapp.android.auth.models.Quest;
import com.ecoapp.android.auth.models.UserQuest;
import com.example.ecoapp.R;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OngoingQuestDetailFragment extends Fragment {

    private static final String TAG = "OngoingQuestDetailFragment.java";
    private QuestApiService apiService;
    private int questId;

    //Flag
    private boolean isAbandoningOrIsCompleting = false;

    //Progresso
    private int currentProgress = 0;
    private int maxProgress = 0;
    private LinearProgressIndicator progressBar;
    private TextView txtProgressStatus;

    public OngoingQuestDetailFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quest_ongoing_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Progresso dinamico
        progressBar = view.findViewById(R.id.progressBar);
        txtProgressStatus = view.findViewById(R.id.txtProgressStatus);

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
        view.findViewById(R.id.buttonDecrement10).setOnClickListener(v -> setProgress(currentProgress - 10));
        view.findViewById(R.id.buttonDecrement).setOnClickListener(v -> setProgress(currentProgress - 1));
        view.findViewById(R.id.buttonIncrement).setOnClickListener(v -> setProgress(currentProgress + 1));
        view.findViewById(R.id.buttonIncrement10).setOnClickListener(v -> setProgress(currentProgress + 10));
        view.findViewById(R.id.buttonCompleteQuest).setOnClickListener(v -> {
            //Attiva il flag per bloccare onPause()
            isAbandoningOrIsCompleting = true;

            //Chiamata al server
            //Resetta progresso a 0 e incrementa completamenti
            saveFinalStateToServer();

            Toast.makeText(getContext(), "Quest Completata!", Toast.LENGTH_SHORT).show();
            //Comunica al QuestFragment di passare alla tab Completed
            Bundle result = new Bundle();
            result.putInt("selectedTab", 2); // 2 = Completed
            getParentFragmentManager().setFragmentResult("questAccepted", result);

            //Torna indietro
            Navigation.findNavController(requireView()).popBackStack();
        });

        // Bottone Abbandona Quest con dialog di conferma
        view.findViewById(R.id.buttonAbandonQuest).setOnClickListener(v -> showAbandonConfirmation());
    }

    //PopUp di conferma
    private void showAbandonConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Abbandona Quest")
                .setMessage("Sei sicuro di voler abbandonare questa quest? Il progresso verrà perso.")
                .setPositiveButton("Sì, abbandona", (dialog, which) -> abandonQuest())
                .setNegativeButton("Annulla", null)
                .show();
    }

    private void abandonQuest() {
        //Attiva il flag per bloccare onPause()
        isAbandoningOrIsCompleting = true;
        String token = "Bearer " + AuthManager.getInstance(requireContext()).getToken();

        //Per resettare il progress (actual_progress a 0)
        //E per disattivare la quest (is_currently_active = false)
        Map<String, Object> body = new HashMap<>();
        body.put("questId", questId);
        body.put("actual_progress", 0);
        body.put("is_currently_active", false);

        // Una sola chiamata, zero problemi di "chi arriva prima"
        apiService.setQuestParameters(token, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Quest abbandonata", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).popBackStack();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Errore di rete", Toast.LENGTH_SHORT).show();
            }
        });
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

        apiService.getGlobalQuests(token).enqueue(new Callback<List<Quest>>() {
            @Override
            public void onResponse(@NonNull Call<List<Quest>> call, @NonNull Response<List<Quest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Quest q : response.body()) {
                        if (q.getId() == questId) {
                            populateStaticUI(view, q);
                            loadUserProgress(token, q.getMaxProgress());
                            break;
                        }
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Quest>> call, @NonNull Throwable t) {
                Log.e(TAG, "Errore caricamento dati");
            }
        });
    }

    private void loadUserProgress(String token, int maxProgress) {
        apiService.getUserQuests(token).enqueue(new Callback<List<UserQuest>>() {
            @Override
            public void onResponse(@NonNull Call<List<UserQuest>> call, @NonNull Response<List<UserQuest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (UserQuest uq : response.body()) {
                        if (uq.getQuestId() == questId) {
                            updateProgressBar(uq.getActualProgress(), maxProgress);
                            break;
                        }
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<UserQuest>> call, @NonNull Throwable t) {}
        });
    }

    private void setProgress(int newCurrentProgress) {
        if (newCurrentProgress < 0) {
            //Toast.makeText(getContext(), "Il progresso è già a zero", Toast.LENGTH_SHORT).show();
            newCurrentProgress = 0;
        }

        if (newCurrentProgress >= maxProgress) {
            //Toast.makeText(getContext(), "Hai già raggiunto il massimo!", Toast.LENGTH_SHORT).show();
            newCurrentProgress = maxProgress;
        }

        //Aggiorna i paramentri currentProgress e maxProgress
        //Aggiorna anche la barra di progresso
        updateProgressBar(newCurrentProgress, maxProgress);
    }

    private void saveFinalStateToServer() {
        String token = "Bearer " + AuthManager.getInstance(requireContext()).getToken();

        // Mandiamo progressIncrement = maxProgress per garantire che il server raggiunga il max
        // Anche se il server ha un valore inferiore, questo assicura il completamento
        // Il backend gestisce già il caso in cui actual_progress supera max_progress
        Map<String, Object> body = new HashMap<>();
        body.put("questId", questId);
        body.put("progressIncrement", maxProgress);

        apiService.updateQuestProgress(token, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Quest completata con successo, punti assegnati");
                } else {
                    Log.e(TAG, "Errore completamento quest: " + response.code());
                }
            }
            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Log.e(TAG, "Errore di rete: " + t.getMessage());
            }
        });
    }

    private void populateStaticUI(View v, Quest q) {
        //Riferimenti agli elementi del layout
        ImageView img = v.findViewById(R.id.questImage);
        TextView txtName = v.findViewById(R.id.txtQuestName);
        TextView txtType = v.findViewById(R.id.txtQuestType);
        TextView txtDesc = v.findViewById(R.id.txtQuestDescription);
        TextView txtCO2 = v.findViewById(R.id.txtCO2saved);
        TextView txtReward = v.findViewById(R.id.txtQuestRewardPoints);

        //Impostazione immagine
        if(q.getQuestImageResourceId(getContext()) != 0)
            img.setImageResource(q.getQuestImageResourceId(getContext()));

        //Impostazione ""Stringhe""
        txtName.setText(q.getName());
        txtType.setText(q.getType());
        txtDesc.setText(q.getDescription());

        //Impostazione double
        double co2Value = q.getCO2Saved();
        txtCO2.setText(getString(R.string.quest_CO2_saved, co2Value));

        //Impostazione int
        txtReward.setText(getString(R.string.quest_reward_points, q.getRewardPoints()));

        //Metodo per gli EU goals
        setupEuGoals(v, q);
    }

    private void setupEuGoals(View view, Quest quest) {
        // Riferimento a ChipGroup (che è un ViewGroup)
        ViewGroup container = view.findViewById(R.id.containerEuGoals);
        if (container == null) return;

        container.removeAllViews();

        int[] resIdArray = quest.getEuGoalsResourceIds(requireContext());

        for (int resId : resIdArray) {
            if (resId != 0) {
                // Creiamo la ImageView
                ImageView imageView = new ImageView(requireContext());

                // Imposta dimensioni (60dp)
                int size = (int) (100 * getResources().getDisplayMetrics().density);

                // Usa ViewGroup.LayoutParams perché ChipGroup accetta qualsiasi View
                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(size, size);
                imageView.setLayoutParams(params);

                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageView.setImageResource(resId);

                // Aggiunge l'immagine al ChipGroup
                container.addView(imageView);
            }
        }
    }

    private void updateProgressBar(int current, int max) {
        this.currentProgress = current;
        this.maxProgress = max;
        progressBar.setMax(max);
        progressBar.setProgress(current);

        // Usa getString direttamente dal fragment
        txtProgressStatus.setText(getString(R.string.quest_current_progress, current, max));

        //Abilita il pulsante "Complete" solo se hai raggiunto il punteggio massimo
        View btnComplete = getView() != null ? getView().findViewById(R.id.buttonCompleteQuest) : null;
        if (btnComplete != null) {
            btnComplete.setEnabled(currentProgress >= maxProgress);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        //Per evitare che modifichi i valori
        if(isAbandoningOrIsCompleting){
            return;
        }

        // Quando l'utente esce, salva il progresso fatto finora (senza cambiare stato/completamenti)
        String token = "Bearer " + AuthManager.getInstance(requireContext()).getToken();
        Map<String, Object> body = new HashMap<>();
        body.put("questId", questId);
        body.put("actual_progress", currentProgress);

        //Setta solo i parametri richiesti (quindi solo actual_progress in questo caso)
        apiService.setQuestParameters(token, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                Log.d(TAG, "Progresso salvato in uscita");
            }
            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {}
        });
    }
}