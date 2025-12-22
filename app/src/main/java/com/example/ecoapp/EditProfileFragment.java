package com.example.ecoapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.ecoapp.android.auth.ApiClient;
import com.ecoapp.android.auth.AuthService;
import com.ecoapp.android.auth.models.User;
import com.example.ecoapp.databinding.FragmentEditProfileBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileFragment extends Fragment {

    private FragmentEditProfileBinding binding;
    private AuthService authService;
    private User currentUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        authService = ApiClient.getAuthService(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Pre-carica i dati attuali
        loadUserProfile();

        binding.buttonSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void loadUserProfile() {
        authService.getProfile().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    binding.editName.setText(currentUser.getName());
                    // Nota: se il modello User non ha un campo "quote", qui potresti avere problemi.
                    // Per ora usiamo il nome come esempio se la quote non esiste nel modello.
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(requireContext(), "Errore caricamento dati", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
        if (currentUser == null) return;

        String newName = binding.editName.getText().toString().trim();
        if (newName.isEmpty()) {
            binding.layoutEditName.setError("Il nome non può essere vuoto");
            return;
        }

        currentUser.setName(newName);
        // Se aggiungi il campo quote al modello User, aggiornalo qui:
        // currentUser.setQuote(binding.editQuote.getText().toString());

        authService.updateProfile(currentUser).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Profilo aggiornato con successo", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(EditProfileFragment.this).navigateUp();
                } else {
                    Toast.makeText(requireContext(), "Errore durante il salvataggio", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(requireContext(), "Errore di rete", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
