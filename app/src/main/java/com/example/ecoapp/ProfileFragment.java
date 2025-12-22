package com.example.ecoapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ecoapp.android.auth.ApiClient;
import com.ecoapp.android.auth.AuthService;
import com.ecoapp.android.auth.models.Quest;
import com.ecoapp.android.auth.models.User;
import com.example.ecoapp.databinding.FragmentProfileBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private AuthService authService;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        authService = ApiClient.getAuthService(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.viewFriends.setOnClickListener(v -> {
            NavHostFragment.findNavController(ProfileFragment.this)
                    .navigate(R.id.action_profileFragment_to_friendsFragment);
        });

        binding.editProfileButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(ProfileFragment.this)
                    .navigate(R.id.action_profileFragment_to_editProfileFragment);
        });
        
        binding.co2Layout.setOnClickListener(v -> showCO2DetailSheet());
        
        setupBadges();
        setupOngoingQuests();
        setupCompletedQuests();
        loadUserProfile();
    }

    private void setupBadges() {
        binding.badgesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        List<Integer> dummyBadges = new ArrayList<>();
        dummyBadges.add(android.R.drawable.ic_menu_compass);
        dummyBadges.add(android.R.drawable.ic_menu_gallery);
        dummyBadges.add(android.R.drawable.ic_menu_manage);
        BadgesAdapter adapter = new BadgesAdapter(dummyBadges, id -> showDetailSheet("Badge", "Badge ottenuto per i tuoi meriti ecologici."));
        binding.badgesRecyclerView.setAdapter(adapter);
    }

    private void setupOngoingQuests() {
        binding.ongoingQuestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        List<Quest> ongoing = new ArrayList<>();
        ongoing.add(new Quest(1, "Pianta 5 Alberi", 40, 0));
        ongoing.add(new Quest(2, "Settimana senza plastica", 70, 0));
        
        OngoingQuestsAdapter adapter = new OngoingQuestsAdapter(ongoing, quest -> 
            showDetailSheet(quest.getName(), "Progresso attuale: " + quest.getProgress() + "%"));
        binding.ongoingQuestsRecyclerView.setAdapter(adapter);
    }
    
    private void setupCompletedQuests() {
        binding.completedQuestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        List<Quest> completed = new ArrayList<>();
        completed.add(new Quest(5, "Riciclo Plastica", 100, 0));
        completed.add(new Quest(3, "Mobilità Sostenibile", 100, 0));
        
        CompletedQuestsAdapter adapter = new CompletedQuestsAdapter(completed, quest -> 
            showDetailSheet(quest.getName(), "Sfida completata con successo!"));
        binding.completedQuestsRecyclerView.setAdapter(adapter);
    }

    private void showCO2DetailSheet() {
        StringBuilder sb = new StringBuilder();
        sb.append("- Riciclo Plastica: 5.2 kg\n");
        sb.append("- Mobilità Sostenibile: 8.0 kg\n");
        sb.append("- Risparmio Energetico: 2.2 kg\n\n");
        sb.append("Totale calcolato in base alle sfide completate.");
        
        showDetailSheet("Dettaglio CO2 Salvata", sb.toString());
    }

    private void showDetailSheet(String title, String description) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_item_detail_sheet, null);
        ((TextView)view.findViewById(R.id.sheet_title)).setText(title);
        ((TextView)view.findViewById(R.id.sheet_description)).setText(description);
        view.findViewById(R.id.sheet_close_button).setOnClickListener(v -> bottomSheetDialog.dismiss());
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }
    
    private void loadUserProfile() {
        authService.getProfile().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) updateUI(response.body());
            }
            @Override public void onFailure(Call<User> call, Throwable t) {}
        });
    }
    
    private void updateUI(User user) {
        binding.userNickname.setText(user.getName());
        binding.userRealName.setText(user.getName());
        binding.userBio.setText(""); 
        binding.userLevel.setText(String.format("Livello: %s", user.getLevel()));
        binding.totalPointsValue.setText(String.valueOf(user.getTotalPoints()));
        binding.co2SavedValue.setText(String.format(Locale.getDefault(), "%.1f kg", user.getCo2Saved()));
        int percentage = (user.getTotalPoints() % 1000 * 100) / 1000;
        binding.levelProgress.setProgress(percentage);
        binding.nextLevelPercentage.setText(String.format(Locale.getDefault(), "%d%% completato", percentage));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
