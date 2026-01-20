package com.example.ecoapp.quest_files;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    private static final String TAG = "OngoingQuestDetail";
    private QuestApiService apiService;
    private int questId;
    private LinearProgressIndicator progressBar;
    private TextView txtProgressStatus;

    private int currentProgress = 0;
    private int maxProgress = 0;

    public OngoingQuestDetailFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quest_ongoing_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.progressBar);
        txtProgressStatus = view.findViewById(R.id.txtProgressStatus);

        setupRetrofit();

        if (getArguments() != null) {
            questId = getArguments().getInt("questId", -1);
            loadData(view);
        }

        view.findViewById(R.id.buttonIncrement).setOnClickListener(v -> updateProgress(1));
        view.findViewById(R.id.buttonDecrement).setOnClickListener(v -> updateProgress(-1));
        view.findViewById(R.id.buttonCompleteQuest).setOnClickListener(v -> completeQuest());
    }

    private void setupRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://ecoapp-p5gp.onrender.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(QuestApiService.class);
    }

    private void loadData(View view) {
        String token = "Bearer " + AuthManager.getInstance(requireContext()).getToken();

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

    private void updateProgress(int increment) {
        if (increment < 0 && currentProgress <= 0) {
            Toast.makeText(getContext(), "Il progresso è già a zero", Toast.LENGTH_SHORT).show();
            return;
        }

        if (increment > 0 && currentProgress >= maxProgress) {
            Toast.makeText(getContext(), "Hai già raggiunto il massimo!", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = "Bearer " + AuthManager.getInstance(requireContext()).getToken();
        Map<String, Object> body = new HashMap<>();
        body.put("questId", questId);
        body.put("progressIncrement", increment);

        apiService.updateQuestProgress(token, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    currentProgress += increment;
                    updateProgressBar(currentProgress, maxProgress);
                    Toast.makeText(getContext(), "Progresso aggiornato!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Errore server: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Errore di connessione", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void completeQuest() {
        // Incremento 0 per forzare la logica di completamento lato server se necessario,
        // oppure naviga semplicemente indietro.
        updateProgress(0);
        Navigation.findNavController(requireView()).popBackStack();
    }

    private void populateStaticUI(View v, Quest q) {
        ((TextView) v.findViewById(R.id.txtQuestName)).setText(q.getName());
        ((TextView) v.findViewById(R.id.txtQuestDescription)).setText(q.getDescription());

        ImageView img = v.findViewById(R.id.questImage);
        int resId = q.getQuestImageResourceId(getContext());
        if (resId != 0) img.setImageResource(resId);
    }

    private void updateProgressBar(int current, int max) {
        this.currentProgress = current;
        this.maxProgress = max;
        progressBar.setMax(max);
        progressBar.setProgress(current);

        // Usa getString direttamente dal fragment
        txtProgressStatus.setText(getString(R.string.quest_current_progress, current, max));

        View btnComplete = getView() != null ? getView().findViewById(R.id.buttonCompleteQuest) : null;
        if (btnComplete != null) {
            btnComplete.setEnabled(currentProgress >= maxProgress);
        }
    }
}