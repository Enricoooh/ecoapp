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

import com.ecoapp.android.auth.ApiClient;
import com.ecoapp.android.auth.AuthManager;
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
        
        // Carica dati profilo dal backend
        loadUserProfile();
    }
    
    private void loadUserProfile() {
        authService.getProfile().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    updateUI(user);
                } else {
                    Toast.makeText(requireContext(), 
                            "Errore nel caricamento del profilo", 
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(requireContext(), 
                        "Errore di connessione: " + t.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateUI(User user) {
        // Aggiorna nome utente
        TextView userName = binding.getRoot().findViewById(R.id.user_name);
        userName.setText(user.getName());
        
        // Aggiorna livello
        TextView userLevel = binding.getRoot().findViewById(R.id.user_level);
        userLevel.setText(user.getLevel());
        
        // Aggiorna punti totali
        TextView totalPoints = binding.getRoot().findViewById(R.id.total_points_value);
        totalPoints.setText(String.valueOf(user.getTotalPoints()));
        
        // Aggiorna CO2 risparmiata
        TextView co2Saved = binding.getRoot().findViewById(R.id.co2_saved_value);
        co2Saved.setText(String.format(Locale.getDefault(), "%.1f kg", user.getCo2Saved()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
