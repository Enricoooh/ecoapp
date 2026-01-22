package com.example.ecoapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ecoapp.android.auth.ApiClient;
import com.ecoapp.android.auth.AuthService;
import com.ecoapp.android.auth.models.User;
import com.example.ecoapp.databinding.FragmentFriendsBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendsFragment extends Fragment {

    private static final String TAG = "FriendsFragment";
    private FragmentFriendsBinding binding;
    private AuthService authService;
    private FriendsAdapter friendsAdapter;
    private FriendRequestAdapter requestAdapter;
    
    private List<User> originalFriendsList = new ArrayList<>();
    private List<User> displayedFriendsList = new ArrayList<>();
    private List<User> pendingRequests = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFriendsBinding.inflate(inflater, container, false);
        authService = ApiClient.getAuthService(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup Friends List
        binding.friendsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        friendsAdapter = new FriendsAdapter(displayedFriendsList, this::showRemoveConfirmation);
        binding.friendsRecyclerView.setAdapter(friendsAdapter);

        // Setup Requests List
        binding.requestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        requestAdapter = new FriendRequestAdapter(pendingRequests, this::respondToRequest);
        binding.requestsRecyclerView.setAdapter(requestAdapter);

        // Search/Invite Logic
        binding.searchFriendInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFriends(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.searchFriendLayout.setEndIconOnClickListener(v -> {
            String query = binding.searchFriendInput.getText().toString().trim();
            if (!query.isEmpty()) sendFriendRequest(query);
        });

        binding.searchFriendInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                String query = binding.searchFriendInput.getText().toString().trim();
                if (!query.isEmpty()) sendFriendRequest(query);
                return true;
            }
            return false;
        });

        loadData();
    }

    private void loadData() {
        loadFriends();
        loadRequests();
    }

    private void sendFriendRequest(String query) {
        Map<String, String> body = new HashMap<>();
        body.put("query", query);
        authService.sendFriendRequest(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Richiesta inviata!", Toast.LENGTH_SHORT).show();
                    binding.searchFriendInput.setText("");
                } else {
                    handleErrorResponse(response);
                }
            }
            @Override public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Errore di rete", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRequests() {
        authService.getPendingRequests().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    pendingRequests.clear();
                    pendingRequests.addAll(response.body());
                    requestAdapter.notifyDataSetChanged();
                    
                    int visibility = pendingRequests.isEmpty() ? View.GONE : View.VISIBLE;
                    binding.requestsTitle.setVisibility(visibility);
                    binding.requestsRecyclerView.setVisibility(visibility);
                }
            }
            @Override public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {}
        });
    }

    private void respondToRequest(User sender, String action) {
        Map<String, String> body = new HashMap<>();
        body.put("senderId", sender.getId());
        body.put("action", action);
        
        authService.respondToRequest(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    loadData();
                }
            }
            @Override public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {}
        });
    }

    private void loadFriends() {
        authService.getFriends().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    originalFriendsList.clear();
                    originalFriendsList.addAll(response.body());
                    filterFriends(binding.searchFriendInput.getText().toString());
                }
            }
            @Override public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {}
        });
    }

    private void filterFriends(String text) {
        displayedFriendsList.clear();
        if (text.isEmpty()) {
            displayedFriendsList.addAll(originalFriendsList);
        } else {
            String query = text.toLowerCase();
            for (User friend : originalFriendsList) {
                if ((friend.getNickname() != null && friend.getNickname().toLowerCase().contains(query)) ||
                    (friend.getName() != null && friend.getName().toLowerCase().contains(query))) {
                    displayedFriendsList.add(friend);
                }
            }
        }
        friendsAdapter.notifyDataSetChanged();
    }

    private void showRemoveConfirmation(User friend) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Rimuovi amico")
                .setMessage("Rimuovere " + friend.getName() + "?")
                .setPositiveButton("Sì", (d, w) -> removeFriend(friend.getId()))
                .setNegativeButton("No", null).show();
    }

    private void removeFriend(String friendId) {
        Map<String, String> body = new HashMap<>();
        body.put("friendId", friendId);
        authService.removeFriend(body).enqueue(new Callback<Map<String, Object>>() {
            @Override public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful()) loadFriends();
            }
            @Override public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {}
        });
    }

    private void handleErrorResponse(Response<?> response) {
        if (response.errorBody() != null) {
            try {
                String errorJson = response.errorBody().string();
                Map<String, String> errorMap = new Gson().fromJson(errorJson, new TypeToken<Map<String, String>>(){}.getType());
                Toast.makeText(getContext(), errorMap.getOrDefault("error", "Errore"), Toast.LENGTH_LONG).show();
            } catch (IOException ignored) {}
        }
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
