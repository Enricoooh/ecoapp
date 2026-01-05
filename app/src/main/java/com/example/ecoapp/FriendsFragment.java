package com.example.ecoapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ecoapp.android.auth.ApiClient;
import com.ecoapp.android.auth.AuthService;
import com.ecoapp.android.auth.models.User;
import com.example.ecoapp.databinding.FragmentFriendsBinding;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendsFragment extends Fragment {

    private FragmentFriendsBinding binding;
    private AuthService authService;
    private FriendsAdapter adapter;
    private List<User> friendsList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFriendsBinding.inflate(inflater, container, false);
        authService = ApiClient.getAuthService(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.friendsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FriendsAdapter(friendsList);
        binding.friendsRecyclerView.setAdapter(adapter);

        loadFriends();
    }

    private void loadFriends() {
        authService.getFriends().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    friendsList.clear();
                    friendsList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(requireContext(), "Impossibile caricare gli amici", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Errore di connessione", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
