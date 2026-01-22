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

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.ecoapp.android.auth.AuthManager; // Verifica che il pacchetto sia questo
import java.util.HashMap;

public class GlobalQuestDetailFragment extends Fragment {

    private static final String TAG = "GlobalQuestDetailFragment.java";

    private QuestApiService apiService;

    public GlobalQuestDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quest_global_detail, container, false);
    }

    // Dentro onViewCreated del QuestGlobalDetailFragment
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Inizializza il service PRIMA di tutto
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://ecoapp-p5gp.onrender.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(QuestApiService.class);

        // 2. Ora che apiService è pronto, recupera l'ID e carica i dettagli
        Bundle args = getArguments();
        if (args != null) {
            int questId = args.getInt("questId", 0);
            loadQuestDetails(view, questId);
        }

        // 3. Gestione pulsante
        Button buttonAccept = view.findViewById(R.id.buttonAcceptQuest);
        buttonAccept.setOnClickListener(v -> showAcceptConfirmation());
    }

    private void showAcceptConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Conferma Missione")
                .setMessage("Vuoi davvero iniziare questa missione?")
                .setPositiveButton("Sì, accetta", (dialog, which) -> sendAcceptRequestToServer())
                .setNegativeButton("Annulla", null)
                .show();
    }

    private void sendAcceptRequestToServer() {
        String token = "Bearer " + AuthManager.getInstance(requireContext()).getToken();

        int questId = -1;
        if (getArguments() != null) {
            questId = getArguments().getInt("questId", -1);
        }

        // Controllo apiService
        if (questId == -1 || apiService == null) return;

        // 2. Prepariamo il corpo per l'endpoint 'update'
        // Inviamo progressIncrement = 0 così il server la registra senza avanzare
        Map<String, Object> body = new HashMap<>();
        body.put("questId", questId);
        body.put("progressIncrement", 0);

        // 3. Usiamo l'endpoint update (cambia il nome del metodo se necessario nell'interfaccia API)
        apiService.updateQuestProgress(token, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse( @NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Missione accettata e salvata!", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).popBackStack();
                } else {
                    Toast.makeText(getContext(), "Errore nel salvataggio", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Errore di connessione", Toast.LENGTH_SHORT).show();
            }
        });
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

        if (apiService == null) {
            Log.e("RetrofitError", "ApiService non inizializzato");
            return;
        }

        // 2. Passa authToken come argomento a getGlobalQuests()
        apiService.getGlobalQuests(authToken).enqueue(new Callback<List<Quest>>() {
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
                else {
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
        TextView txtCO2 = v.findViewById(R.id.txtCO2saved);
        TextView txtReward = v.findViewById(R.id.txtQuestRewardPoints);

        // Impostazione dati dall'oggetto Quest
        img.setImageResource(quest.getQuestImageResourceId(getContext()));
        txtName.setText(quest.getName());
        txtType.setText(quest.getType());
        txtDesc.setText(quest.getDescription());

        //Per CO2 con double
        double co2Valore = quest.getCO2Saved();
        txtCO2.setText(getString(R.string.quest_CO2_saved, co2Valore));

        //Metodo per gli EU goals
        setupEuGoals(v, quest);

        // Utilizzo della stringa reward_label (Risolve il Warning)
        txtReward.setText(getString(R.string.quest_reward_points, quest.getRewardPoints()));
    }

    private void setupEuGoals(View view, Quest quest) {
        LinearLayout container = view.findViewById(R.id.containerEuGoals);
        if (container == null || quest.getImagesEuGoals() == null || getContext() == null) {
            Log.d(TAG, "Container o immagini EU Goals nulli");
            return;
        }

        container.removeAllViews();
        android.content.Context context = getContext();

        for (String imageName : quest.getImagesEuGoals()) {
            // LOG DI DEBUG: Controlla se i nomi arrivano correttamente dal JSON
            Log.d(TAG, "Cerco immagine EU Goal: " + imageName);

            ImageView imageView = new ImageView(context);

            // Importante: getIdentifier deve pulire eventuali spazi
            int resId = context.getResources().getIdentifier(
                    imageName.trim(), "drawable", context.getPackageName());

            if (resId != 0) {
                int sizeInPx = (int) (45 * context.getResources().getDisplayMetrics().density); // Un po' più piccoli per farli stare tutti
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizeInPx, sizeInPx);
                params.setMargins(0, 0, (int) (8 * context.getResources().getDisplayMetrics().density), 0);

                imageView.setLayoutParams(params);
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageView.setImageResource(resId);

                container.addView(imageView);
            } else {
                Log.e(TAG, "Risorsa non trovata per: " + imageName);
            }
        }
    }
}