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

import com.example.ecoapp.R;
import com.ecoapp.android.auth.models.Quest;
import com.ecoapp.android.auth.AuthManager;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GlobalQuestDetailFragment extends Fragment {

    private static final String TAG = "GlobalQuestDetail";
    private QuestApiService apiService;
    private int questId;

    public GlobalQuestDetailFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quest_global_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Setup Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://ecoapp-p5gp.onrender.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(QuestApiService.class);

        // 2. Recupero ID dal Bundle
        if (getArguments() != null) {
            questId = getArguments().getInt("questId", -1);
            loadQuestDetails(view, questId);
        }

        // 3. Pulsante Accetta (Semplice, senza isRedo)
        Button buttonAccept = view.findViewById(R.id.buttonAcceptQuest);
        buttonAccept.setOnClickListener(v -> showAcceptConfirmation());
    }

    //Interfaccia di conferma della scelta
    private void showAcceptConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Conferma Quest")
                .setMessage("Vuoi davvero iniziare questa quest?")
                .setPositiveButton("Sì, accetta", (dialog, which) -> sendAcceptRequestToServer())
                .setNegativeButton("Annulla", null)
                .show();
    }

    private void sendAcceptRequestToServer() {
        String token = "Bearer " + AuthManager.getInstance(requireContext()).getToken();

        if (questId == -1 || apiService == null) return;

        // Creiamo il body con la tua logica di "SET"
        Map<String, Object> body = new HashMap<>();
        body.put("questId", questId);
        body.put("actual_progress", 0);        // Forziamo il valore iniziale a 0
        body.put("times_completed", 0);        // Forziamo il valore iniziale a 0
        body.put("is_currently_active", true); // Diciamo chiaramente cosa vogliamo settare


        //Metodo specifico
        apiService.setFirstActivation(token, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Quest attivata!", Toast.LENGTH_SHORT).show();

                    // Notifica il cambio tab al Fragment principale
                    Bundle result = new Bundle();
                    result.putInt("selectedTab", 1);
                    getParentFragmentManager().setFragmentResult("questAccepted", result);

                    Navigation.findNavController(requireView()).popBackStack();
                }
                else {
                    // Leggiamo il codice di errore (es. 401, 500)
                    int errorCode = response.code();

                    // Proviamo a leggere il messaggio di errore inviato dal server
                    String errorMsg = "Errore sconosciuto";
                    try (okhttp3.ResponseBody body = response.errorBody()){
                        if (body != null) {
                            errorMsg = body.string();
                        }
                    }
                    catch (Exception e) {
                        Log.e(TAG, "Errore durante la lettura del corpo dell'errore", e);
                    }

                    Log.e(TAG, "ERRORE SERVER: Codice " + errorCode + " - Messaggio: " + errorMsg);
                    Toast.makeText(getContext(), "Errore " + errorCode + ": " + errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Log.e(TAG, "ERRORE CRITICO", t);new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Errore di Connessione")
                        .setMessage(t.toString()) // Ti dirà esattamente se è Timeout, DNS o JSON error
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void loadQuestDetails(View v, int questId) {
        String token = "Bearer " + AuthManager.getInstance(requireContext()).getToken();

        apiService.getGlobalQuests(token).enqueue(new Callback<List<Quest>>() {
            @Override
            public void onResponse(@NonNull Call<List<Quest>> call, @NonNull Response<List<Quest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Quest q : response.body()) {
                        if (q.getId() == questId) {
                            populateUI(v, q);
                            break;
                        }
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Quest>> call, @NonNull Throwable t) {
                Log.e(TAG, "Errore caricamento dettagli");
            }
        });
    }

    private void populateUI(View v, Quest quest) {
        //Binding
        ImageView img = v.findViewById(R.id.questImage);
        TextView txtName = v.findViewById(R.id.txtQuestName);
        TextView txtType = v.findViewById(R.id.txtQuestType);
        TextView txtDesc = v.findViewById(R.id.txtQuestDescription);
        TextView txtCO2 = v.findViewById(R.id.txtCO2saved);
        TextView txtReward = v.findViewById(R.id.txtQuestRewardPoints);

        //""Set"" delle ""Stringe""
        txtName.setText(quest.getName());
        txtType.setText(quest.getType());
        txtDesc.setText(quest.getDescription());

        //""Set"" dei ""double""
        txtCO2.setText(getString(R.string.quest_CO2_saved, quest.getCO2Saved()));

        //""Set"" degli ""int""
        txtReward.setText(getString(R.string.quest_reward_points, quest.getRewardPoints()));

        //""Set"" delle immagini
        img.setImageResource(quest.getQuestImageResourceId(requireContext()));
        setupEuGoals(v, quest);
    }

    /*
    private void setupEuGoals(View view, Quest quest) {
        LinearLayout container = view.findViewById(R.id.containerEuGoals);
        if (container == null || quest.getImagesEuGoals() == null) return;

        container.removeAllViews();
        for (String imageName : quest.getImagesEuGoals()) {
            ImageView imageView = new ImageView(requireContext());
            int resId = getResources().getIdentifier(imageName.trim(), "drawable", requireContext().getPackageName());

            if (resId != 0) {
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
    */

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