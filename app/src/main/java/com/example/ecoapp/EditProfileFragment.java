package com.example.ecoapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.ecoapp.android.auth.ApiClient;
import com.ecoapp.android.auth.AuthService;
import com.ecoapp.android.auth.models.User;
import com.example.ecoapp.databinding.FragmentEditProfileBinding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileFragment extends Fragment {

    private static final String TAG = "EditProfileFragment";
    private FragmentEditProfileBinding binding;
    private AuthService authService;
    private User currentUser;
    private String encodedImage;

    // Launcher per la galleria
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    startCrop(imageUri); // Avvia il ritaglio
                }
            }
    );

    // Launcher per il ritaglio (Crop)
    private final ActivityResultLauncher<Intent> cropLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap bitmap = extras.getParcelable("data");
                        if (bitmap != null) {
                            binding.editProfileImagePreview.setImageBitmap(bitmap);
                            encodedImage = encodeImage(bitmap);
                        }
                    }
                }
            }
    );

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

        binding.buttonChangePhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        binding.buttonSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void startCrop(Uri uri) {
        try {
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            cropIntent.setDataAndType(uri, "image/*");
            cropIntent.putExtra("crop", "true");
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            cropIntent.putExtra("outputX", 300);
            cropIntent.putExtra("outputY", 300);
            cropIntent.putExtra("return-data", true);
            cropLauncher.launch(cropIntent);
        } catch (Exception e) {
            // Se il sistema non ha un'app di ritaglio, carichiamo l'immagine originale
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
                binding.editProfileImagePreview.setImageBitmap(bitmap);
                encodedImage = encodeImage(bitmap);
            } catch (IOException ex) {
                Log.e(TAG, "Errore caricamento immagine originale", ex);
            }
        }
    }

    private void loadUserProfile() {
        authService.getProfile().enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    binding.editName.setText(currentUser.getNickname() != null ? currentUser.getNickname() : "");
                    binding.editEmail.setText(currentUser.getEmail());
                    binding.editBio.setText(currentUser.getBio() != null ? currentUser.getBio() : "");
                    
                    // Carica l'anteprima dell'immagine attuale
                    String imageStr = currentUser.getUrlImmagineProfilo();
                    if (imageStr == null || imageStr.isEmpty() || imageStr.equals("default")) {
                        binding.editProfileImagePreview.setImageResource(R.drawable.ic_default_avatar);
                    } else {
                        try {
                            byte[] decodedString = Base64.decode(imageStr, Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            if (decodedByte != null) {
                                binding.editProfileImagePreview.setImageBitmap(decodedByte);
                            } else {
                                binding.editProfileImagePreview.setImageResource(R.drawable.ic_default_avatar);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Errore decodifica immagine", e);
                            binding.editProfileImagePreview.setImageResource(R.drawable.ic_default_avatar);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                if (isAdded()) Toast.makeText(requireContext(), "Errore caricamento dati", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String encodeImage(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private void saveProfile() {
        if (currentUser == null) return;

        String newNickname = binding.editName.getText().toString().trim();
        String newEmail = binding.editEmail.getText().toString().trim();
        String newBio = binding.editBio.getText().toString().trim();
        String newPassword = binding.editPassword.getText().toString();
        String confirmPassword = binding.confirmPassword.getText().toString();

        if (newNickname.isEmpty()) {
            binding.layoutEditName.setError("Il nickname non può essere vuoto");
            return;
        }
        if (newEmail.isEmpty()) {
            binding.layoutEditEmail.setError("L'email non può essere vuota");
            return;
        }

        if (!newPassword.isEmpty()) {
            if (!newPassword.equals(confirmPassword)) {
                binding.layoutConfirmPassword.setError("Le password non coincidono");
                return;
            }
            currentUser.setPassword(newPassword);
        }

        currentUser.setNickname(newNickname);
        currentUser.setEmail(newEmail);
        currentUser.setBio(newBio);
        if (encodedImage != null) {
            currentUser.setUrlImmagineProfilo(encodedImage);
        }

        authService.updateProfile(currentUser).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Profilo aggiornato con successo", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(EditProfileFragment.this).navigateUp();
                } else {
                    Toast.makeText(requireContext(), "Errore durante il salvataggio", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                if (isAdded()) Toast.makeText(requireContext(), "Errore di rete", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
