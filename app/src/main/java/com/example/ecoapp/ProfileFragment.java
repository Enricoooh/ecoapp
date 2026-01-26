package com.example.ecoapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
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
import com.ecoapp.android.auth.AuthManager;
import com.ecoapp.android.auth.AuthService;
import com.ecoapp.android.auth.models.Badge;
import com.ecoapp.android.auth.models.User;
import com.example.ecoapp.databinding.FragmentProfileBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;

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

        binding.friendsCard.setOnClickListener(v -> NavHostFragment.findNavController(ProfileFragment.this)
                .navigate(R.id.action_profileFragment_to_friendsFragment));

        binding.editProfileButton.setOnClickListener(v -> NavHostFragment.findNavController(ProfileFragment.this)
                .navigate(R.id.action_profileFragment_to_editProfileFragment));
        
        binding.badgesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Forza il ricaricamento dei dati ogni volta che torniamo sul profilo
        loadUserProfile();
    }

    private void showDetailSheet(String title, String description) {
        if (!isAdded() || binding == null) return;
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_item_detail_sheet, binding.getRoot(), false);
        if (view != null) {
            ((TextView)view.findViewById(R.id.sheet_title)).setText(title);
            ((TextView)view.findViewById(R.id.sheet_description)).setText(description);
            view.findViewById(R.id.sheet_close_button).setOnClickListener(v -> bottomSheetDialog.dismiss());
            bottomSheetDialog.setContentView(view);
            bottomSheetDialog.show();
        }
    }
    
    private void loadUserProfile() {
        authService.getProfile().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (!isAdded()) return;
                
                if (response.isSuccessful() && response.body() != null) {
                    User updatedUser = response.body();
                    // Fondamentale: aggiorna i dati in AuthManager così le altre schermate vedono i cambiamenti
                    AuthManager.getInstance(requireContext()).setCurrentUser(updatedUser);
                    updateUI(updatedUser);
                } else if (response.code() == 401) {
                    Toast.makeText(requireContext(), "Sessione scaduta, effettua nuovamente l'accesso", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Errore nel caricamento profilo", Toast.LENGTH_SHORT).show();
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
                Log.e("ProfileFragment", "Errore decodifica immagine", e);
                binding.profileAvatar.setImageResource(R.drawable.ic_default_avatar);
            }
        }

        // Badges del profilo
        List<Badge> badges = user.getBadges();
        if (badges != null && !badges.isEmpty()) {
            BadgesAdapter adapter = new BadgesAdapter(badges, badge -> showDetailSheet(badge.getName(), badge.getDescription()));
            binding.badgesRecyclerView.setAdapter(adapter);
            binding.badgesRecyclerView.setVisibility(View.VISIBLE);
        } else {
            binding.badgesRecyclerView.setVisibility(View.GONE);
        }

        int points = user.getTotalPoints();
        int percentage = calculateLevelPercentage(points);
        binding.levelProgress.setProgress(percentage);
        binding.nextLevelPercentage.setText(String.format(Locale.getDefault(), "%d%%", percentage));
    }

    /**
     * Calcola la percentuale di progresso verso il prossimo livello.
     * Soglie: 0 (Novizio) → 1000 (Apprendista) → 2000 (Guerriero) → 5000 (Eroe) → 10000 (Leggenda)
     */
    private int calculateLevelPercentage(int points) {
        int[] thresholds = {0, 1000, 2000, 5000, 10000};
        
        for (int i = 0; i < thresholds.length - 1; i++) {
            if (points < thresholds[i + 1]) {
                int pointsInLevel = points - thresholds[i];
                int levelRange = thresholds[i + 1] - thresholds[i];
                return (pointsInLevel * 100) / levelRange;
            }
        }
        return 100; // Già al livello massimo
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
