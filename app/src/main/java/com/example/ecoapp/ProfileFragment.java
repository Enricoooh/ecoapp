package com.example.ecoapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.ecoapp.android.auth.ApiClient;
import com.ecoapp.android.auth.AuthService;
import com.ecoapp.android.auth.models.User;
import com.example.ecoapp.databinding.FragmentProfileBinding;

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
        
        loadUserProfile();
    }
    
    private void loadUserProfile() {
        authService.getProfile().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body());
                } else {
                    Toast.makeText(requireContext(), "Errore caricamento dati", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(requireContext(), "Errore di rete", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateUI(User user) {
        binding.userName.setText(user.getName());
        binding.userLevel.setText(String.format("Livello: %s", user.getLevel()));
        binding.totalPointsValue.setText(String.valueOf(user.getTotalPoints()));
        binding.co2SavedValue.setText(String.format(Locale.getDefault(), "%.1f kg", user.getCo2Saved()));
        
        int pointsForNextLevel = 1000;
        int currentProgress = user.getTotalPoints() % pointsForNextLevel;
        int percentage = (currentProgress * 100) / pointsForNextLevel;
        
        binding.levelProgress.setProgress(percentage);
        binding.nextLevelPercentage.setText(String.format(Locale.getDefault(), "%d%% completato", percentage));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
