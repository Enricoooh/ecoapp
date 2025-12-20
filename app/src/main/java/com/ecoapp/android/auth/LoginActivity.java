package com.ecoapp.android.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ecoapp.android.auth.models.AuthResponse;
import com.ecoapp.android.auth.models.ErrorResponse;
import com.ecoapp.android.auth.models.LoginRequest;
import com.example.ecoapp.MainActivity;
import com.example.ecoapp.databinding.ActivityLoginBinding;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthService authService;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authService = ApiClient.getAuthService(this);
        authManager = AuthManager.getInstance(this);

        // Se già autenticato, vai a MainActivity
        if (authManager.isAuthenticated()) {
            navigateToMain();
            return;
        }

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.buttonLogin.setOnClickListener(v -> attemptLogin());
        binding.textViewRegisterLink.setOnClickListener(v -> navigateToRegister());
    }

    private void attemptLogin() {
        String email = binding.editTextEmail.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString().trim();

        // Validazione
        if (!validateInput(email, password)) {
            return;
        }

        // Mostra loading
        setLoading(true);

        // Chiamata API
        LoginRequest request = new LoginRequest(email, password);
        authService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    
                    // Salva token e dati utente
                    authManager.saveAuthData(authResponse.getToken(), authResponse.getUser());
                    
                    Toast.makeText(LoginActivity.this, "Accesso effettuato!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                } else {
                    // Gestione errore
                    String errorMessage = "Errore di login";
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
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, 
                        "Errore di connessione: " + t.getMessage(), 
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateInput(String email, String password) {
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

        return true;
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.buttonLogin.setEnabled(!isLoading);
        binding.editTextEmail.setEnabled(!isLoading);
        binding.editTextPassword.setEnabled(!isLoading);
        binding.textViewRegisterLink.setEnabled(!isLoading);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
