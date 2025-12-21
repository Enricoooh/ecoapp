package com.ecoapp.android.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ecoapp.android.auth.models.AuthResponse;
import com.ecoapp.android.auth.models.ErrorResponse;
import com.ecoapp.android.auth.models.RegisterRequest;
import com.example.ecoapp.MainActivity;
import com.example.ecoapp.databinding.ActivityRegisterBinding;
import com.google.gson.Gson;

import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.emitter.EmitterConfig;
import nl.dionsegijn.konfetti.core.models.Shape;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthService authService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authService = ApiClient.getAuthService(this);
        authManager = AuthManager.getInstance(this);

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.buttonRegister.setOnClickListener(v -> attemptRegister());
        binding.textViewLoginLink.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String name = binding.editTextName.getText().toString().trim();
        String email = binding.editTextEmail.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString().trim();
        String confirmPassword = binding.editTextConfirmPassword.getText().toString().trim();

        // Validazione
        if (!validateInput(name, email, password, confirmPassword)) {
            return;
        }

        // Mostra loading
        setLoading(true);

        // Chiamata API
        RegisterRequest request = new RegisterRequest(email, password, name);
        authService.register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    
                    // Salva token e dati utente
                    authManager.saveAuthData(authResponse.getToken(), authResponse.getUser());
                    
                    // Celebrazione con confetti!
                    Toast.makeText(RegisterActivity.this, 
                            "🎉 Benvenuto " + authResponse.getUser().getName() + "!", 
                            Toast.LENGTH_LONG).show();
                    celebrateWithConfetti();
                    
                    // Naviga dopo 2 secondi per godersi i confetti
                    new Handler(Looper.getMainLooper()).postDelayed(() -> navigateToMain(), 2000);
                } else {
                    // Gestione errore
                    String errorMessage = "Errore di registrazione";
                    if (response.errorBody() != null) {
                        try {
                            ErrorResponse errorResponse = new Gson().fromJson(
                                    response.errorBody().string(),
                                    ErrorResponse.class
                            );
                            errorMessage = errorResponse.getError();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, 
                        "Errore di connessione: " + t.getMessage(), 
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateInput(String name, String email, String password, String confirmPassword) {
        // Validazione nome
        if (name.isEmpty()) {
            binding.textInputLayoutName.setError("Nome richiesto");
            return false;
        }
        if (name.length() < 2) {
            binding.textInputLayoutName.setError("Nome troppo corto");
            return false;
        }
        binding.textInputLayoutName.setError(null);

        // Validazione email
        if (email.isEmpty()) {
            binding.textInputLayoutEmail.setError("Email richiesta");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.textInputLayoutEmail.setError("Email non valida");
            return false;
        }
        binding.textInputLayoutEmail.setError(null);

        // Validazione password
        if (password.isEmpty()) {
            binding.textInputLayoutPassword.setError("Password richiesta");
            return false;
        }
        if (password.length() < 6) {
            binding.textInputLayoutPassword.setError("Password troppo corta (min 6 caratteri)");
            return false;
        }
        binding.textInputLayoutPassword.setError(null);

        // Validazione conferma password
        if (confirmPassword.isEmpty()) {
            binding.textInputLayoutConfirmPassword.setError("Conferma password");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            binding.textInputLayoutConfirmPassword.setError("Le password non corrispondono");
            return false;
        }
        binding.textInputLayoutConfirmPassword.setError(null);

        return true;
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.buttonRegister.setEnabled(!isLoading);
        binding.editTextName.setEnabled(!isLoading);
        binding.editTextEmail.setEnabled(!isLoading);
        binding.editTextPassword.setEnabled(!isLoading);
        binding.editTextConfirmPassword.setEnabled(!isLoading);
        binding.textViewLoginLink.setEnabled(!isLoading);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void celebrateWithConfetti() {
        EmitterConfig emitterConfig = new Emitter(5L, TimeUnit.SECONDS).max(100);
        Party party = new PartyFactory(emitterConfig)
                .shapes(Arrays.asList(Shape.Circle.INSTANCE, Shape.Square.INSTANCE))
                .colors(Arrays.asList(0xFF58CC02, 0xFF1CB0F6, 0xFFFFC800, 0xFFFF9600))
                .setSpeedBetween(0f, 30f)
                .position(0.5, 0.0, 1.0, 0.0)
                .build();
        
        binding.konfettiView.start(party);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
