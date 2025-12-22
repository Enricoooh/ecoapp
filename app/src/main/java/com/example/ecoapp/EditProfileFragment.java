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
                    binding.editEmail.setText(currentUser.getEmail());
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
        String newEmail = binding.editEmail.getText().toString().trim();
        String newPassword = binding.editPassword.getText().toString();
        String confirmPassword = binding.confirmPassword.getText().toString();

        if (newName.isEmpty()) {
            binding.layoutEditName.setError("Il nome non può essere vuoto");
            return;
        }
        
        if (newEmail.isEmpty()) {
            binding.layoutEditEmail.setError("L'email non può essere vuota");
            return;
        }

        // Controllo grafico password se inserita
        if (!newPassword.isEmpty()) {
            if (!newPassword.equals(confirmPassword)) {
                binding.layoutConfirmPassword.setError("Le password non coincidono");
                return;
            }
            // NOTA: Non possiamo salvare la password nell'oggetto User 
            // perché il modello User.java non può essere modificato.
            // In futuro, dovrai implementare un endpoint API specifico per il cambio password.
            Toast.makeText(requireContext(), "Il cambio password richiede un endpoint dedicato", Toast.LENGTH_LONG).show();
        }

        currentUser.setName(newName);
        currentUser.setEmail(newEmail);

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
