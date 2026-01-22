package com.example.ecoapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ecoapp.android.auth.ApiClient;
import com.ecoapp.android.auth.AuthService;
import com.ecoapp.android.auth.models.Badge;
import com.ecoapp.android.auth.models.User;
import com.example.ecoapp.databinding.FragmentProfileBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private AuthService authService;
    private String userId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString("userId");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        authService = ApiClient.getAuthService(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Personalizzazione per il profilo di un amico
        binding.editProfileButton.setVisibility(View.GONE);
        
        // Rimuovi sezione Social
        binding.socialTitle.setVisibility(View.GONE);
        binding.friendsCard.setVisibility(View.GONE);
        
        // Cambia i titoli per riferirsi all'amico
        binding.impactTitle.setText("IL SUO IMPATTO (Statistiche)");
        binding.myBadgesTitle.setText("I Suoi Badge");
        
        binding.badgesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        
        if (userId != null) {
            loadFriendProfile();
        }
    }

    private void loadFriendProfile() {
        authService.getFriendProfile(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful() && response.body() != null && isAdded()) {
                    updateUI(response.body());
                }
            }
            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                if (isAdded()) Toast.makeText(requireContext(), "Errore caricamento", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(User user) {
        if (binding == null) return;
        binding.userNickname.setText(user.getNickname() != null ? user.getNickname() : user.getName());
        binding.userRealName.setText(user.getName());
        binding.userBio.setText(user.getBio());
        binding.userLevel.setText(String.format("Livello: %s", user.getLevel()));
        binding.totalPointsValue.setText(String.valueOf(user.getTotalPoints()));
        binding.co2SavedValue.setText(String.format(Locale.getDefault(), "%.1f kg", user.getCo2Saved()));

        // Immagine profilo
        String imageStr = user.getUrlImmagineProfilo();
        if (imageStr != null && !imageStr.isEmpty() && !imageStr.equals("default")) {
            try {
                byte[] decodedString = Base64.decode(imageStr, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                binding.profileAvatar.setImageBitmap(decodedByte);
            } catch (Exception ignored) {}
        } else {
            binding.profileAvatar.setImageResource(R.drawable.ic_default_avatar);
        }

        // Badges
        List<Badge> badges = user.getBadges();
        if (badges != null && !badges.isEmpty()) {
            binding.badgesRecyclerView.setAdapter(new BadgesAdapter(badges, b -> showBadgeDetail(b)));
            binding.badgesRecyclerView.setVisibility(View.VISIBLE);
        } else {
            binding.badgesRecyclerView.setVisibility(View.GONE);
        }

        int points = user.getTotalPoints();
        int percentage = (points % 1000 * 100) / 1000;
        binding.levelProgress.setProgress(percentage);
        binding.nextLevelPercentage.setText(String.format(Locale.getDefault(), "%d%%", percentage));
    }

    private void showBadgeDetail(Badge badge) {
        if (!isAdded()) return;
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_item_detail_sheet, null);
        if (view != null) {
            ((TextView)view.findViewById(R.id.sheet_title)).setText(badge.getName());
            ((TextView)view.findViewById(R.id.sheet_description)).setText(badge.getDescription());
            view.findViewById(R.id.sheet_close_button).setOnClickListener(v -> dialog.dismiss());
            dialog.setContentView(view);
            dialog.show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
