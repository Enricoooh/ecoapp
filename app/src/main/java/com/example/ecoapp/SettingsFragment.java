package com.example.ecoapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.ecoapp.android.auth.AuthManager;
import com.ecoapp.android.auth.LoginActivity;

public class SettingsFragment extends Fragment {

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        // Setup logout button
        Button logoutButton = view.findViewById(R.id.buttonLogout);
        logoutButton.setOnClickListener(v -> performLogout());
        
        return view;
    }
    
    private void performLogout() {
        AuthManager authManager = AuthManager.getInstance(requireContext());
        authManager.logout();
        
        Toast.makeText(requireContext(), "Logout effettuato", Toast.LENGTH_SHORT).show();
        
        // Vai a LoginActivity
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}