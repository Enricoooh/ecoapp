package com.example.ecoapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.ecoapp.android.auth.ApiClient;
import com.ecoapp.android.auth.AuthManager;
import com.ecoapp.android.auth.AuthService;
import com.ecoapp.android.auth.LoginActivity;
import com.ecoapp.android.auth.models.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.ecoapp.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private AppBarConfiguration appBarConfiguration;
    
    // BroadcastReceiver per gestire 401 Unauthorized (token invalido/scaduto)
    private final BroadcastReceiver unauthorizedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Token invalido: reset ApiClient e redirect a login
            ApiClient.reset();
            redirectToLogin();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Controllo autenticazione
        AuthManager authManager = AuthManager.getInstance(this);
        if (!authManager.isAuthenticated()) {
            // Utente non autenticato, vai a LoginActivity
            Log.d(TAG, "No token found, redirecting to login");
            redirectToLogin();
            return;
        }
        
        Log.d(TAG, "Token found, validating with server...");
        
        // Valida il token con una chiamata al server
        validateTokenAndProceed();
    }
    
    private void validateTokenAndProceed() {
        AuthService authService = ApiClient.getAuthService(this);
        authService.getProfile().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Token valid, loading main content");
                    // Token valido, mostra l'app
                    setupMainContent();
                } else {
                    Log.d(TAG, "Token invalid (code: " + response.code() + "), redirecting to login");
                    // Token non valido (401, 403, etc.)
                    AuthManager.getInstance(MainActivity.this).logout();
                    ApiClient.reset();
                    redirectToLogin();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.e(TAG, "Network error validating token: " + t.getMessage());
                // Errore di rete: mostra comunque l'app, l'utente vedrà "Errore di rete"
                // Potrebbe essere offline temporaneamente
                setupMainContent();
            }
        });
    }
    
    private void setupMainContent() {
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        NavigationUI.setupWithNavController(binding.bottomNavView, navController);
        
        // Registra receiver per gestire 401 (token scaduto/invalido)
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(unauthorizedReceiver, new IntentFilter(ApiClient.ACTION_UNAUTHORIZED));
    }
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister receiver per evitare memory leak
        LocalBroadcastManager.getInstance(this).unregisterReceiver(unauthorizedReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
