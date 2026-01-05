package com.example.ecoapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ecoapp.android.auth.ApiClient;
import com.ecoapp.android.auth.AuthService;
import com.ecoapp.android.auth.models.Badge;
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (binding == null) return;

        binding.friendsCard.setOnClickListener(v -> {
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
        if (binding == null) return;
        binding.badgesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        
        List<Badge> dummyBadges = new ArrayList<>();
        dummyBadges.add(new Badge(1, "Eco-Novizio", "Benvenuto nel mondo della sostenibilità!", android.R.drawable.ic_menu_compass));
        dummyBadges.add(new Badge(2, "Riciclatore Seriale", "Hai completato 10 sfide di riciclo.", android.R.drawable.ic_menu_gallery));
        dummyBadges.add(new Badge(3, "Amico della Terra", "Hai salvato i tuoi primi 10kg di CO2.", android.R.drawable.ic_menu_manage));
        dummyBadges.add(new Badge(4, "Pioniere Verde", "Uno dei primi 100 utenti della community.", android.R.drawable.ic_menu_camera));
        
        BadgesAdapter adapter = new BadgesAdapter(dummyBadges, badge -> {
            showDetailSheet(badge.getName(), badge.getDescription());
        });
        binding.badgesRecyclerView.setAdapter(adapter);
    }

    private void setupOngoingQuests() {
        if (binding == null) return;
        binding.ongoingQuestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        List<Quest> ongoing = new ArrayList<>();
        ongoing.add(new Quest(1, "Pianta 5 Alberi", "ecology", 2, 5, 0, "Pianta degli alberi per aiutare il pianeta", null, 100));
        ongoing.add(new Quest(2, "Settimana senza plastica", "recycle", 5, 7, 0, "Evita l'uso di plastica monouso", null, 150));
        
        OngoingQuestsAdapter adapter = new OngoingQuestsAdapter(ongoing, quest -> {
            if (quest.getMaxProgress() > 0) {
                int percentage = (quest.getActualProgress() * 100) / quest.getMaxProgress();
                showDetailSheet(quest.getName(), quest.getDescription() + "\nProgresso attuale: " + percentage + "%");
            }
        });
        binding.ongoingQuestsRecyclerView.setAdapter(adapter);
    }
    
    private void setupCompletedQuests() {
        if (binding == null) return;
        binding.completedQuestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        List<Quest> completed = new ArrayList<>();
        completed.add(new Quest(5, "Riciclo Plastica", "recycle", 10, 10, 0, "Hai riciclato correttamente la plastica", null, 50));
        completed.add(new Quest(3, "Mobilità Sostenibile", "mobility", 5, 5, 0, "Hai usato i mezzi pubblici", null, 30));
        
        CompletedQuestsAdapter adapter = new CompletedQuestsAdapter(completed, quest -> 
            showDetailSheet(quest.getName(), quest.getDescription() + "\nSfida completata con successo!"));
        binding.completedQuestsRecyclerView.setAdapter(adapter);
    }

    private void showCO2DetailSheet() {
        String detail = "- Riciclo Plastica: 5.2 kg\n" +
                "- Mobilità Sostenibile: 8.0 kg\n" +
                "- Risparmio Energetico: 2.2 kg\n\n" +
                "Totale calcolato in base alle sfide completate.";
        
        showDetailSheet("Dettaglio CO2 Salvata", detail);
    }

    private void showDetailSheet(String title, String description) {
        if (!isAdded()) return;
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_item_detail_sheet, null);
        if (view != null) {
            ((TextView)view.findViewById(R.id.sheet_title)).setText(title);
            ((TextView)view.findViewById(R.id.sheet_description)).setText(description);
            view.findViewById(R.id.sheet_close_button).setOnClickListener(v -> bottomSheetDialog.dismiss());
            bottomSheetDialog.setContentView(view);
            bottomSheetDialog.show();
        }
    }
    
    private void loadUserProfile() {
        authService.getProfile().enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful() && response.body() != null && isAdded()) {
                    updateUI(response.body());
                }
            }
            @Override public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Errore di rete", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void updateUI(User user) {
        if (binding == null) return;

        binding.userNickname.setText(user.getNickname() != null ? user.getNickname() : user.getName());
        binding.userRealName.setText(user.getName() != null ? user.getName() : "");
        binding.userBio.setText(user.getBio() != null ? user.getBio() : ""); 
        binding.userLevel.setText(String.format("Livello: %s", user.getLevel() != null ? user.getLevel() : "Eco-Novizio"));
        binding.totalPointsValue.setText(String.valueOf(user.getTotalPoints()));
        binding.co2SavedValue.setText(String.format(Locale.getDefault(), "%.1f kg", user.getCo2Saved()));

        // Gestione Immagine Profilo
        String imageStr = user.getUrlImmagineProfilo();
        if (imageStr == null || imageStr.isEmpty() || imageStr.equals("default")) {
            // Imposta l'immagine predefinita se non è presente o è impostata su default
            binding.profileAvatar.setImageResource(R.drawable.ic_default_avatar);
        } else if (imageStr.startsWith("http")) {
            // Per ora lasciamo l'immagine di default per URL esterni non gestiti
            binding.profileAvatar.setImageResource(R.drawable.ic_default_avatar);
        } else {
            try {
                byte[] decodedString = Base64.decode(imageStr, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                if (decodedByte != null) {
                    binding.profileAvatar.setImageBitmap(decodedByte);
                } else {
                    binding.profileAvatar.setImageResource(R.drawable.ic_default_avatar);
                }
            } catch (Exception e) {
                e.printStackTrace();
                binding.profileAvatar.setImageResource(R.drawable.ic_default_avatar);
            }
        }

        int points = user.getTotalPoints();
        int percentage = (points % 1000 * 100) / 1000;
        binding.levelProgress.setProgress(percentage);
        binding.nextLevelPercentage.setText(String.format(Locale.getDefault(), "%d%%", percentage));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
