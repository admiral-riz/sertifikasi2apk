package com.example.batik_nusantara.ui.profile;

import static com.example.batik_nusantara.ServerAPI.BASE_URL_Image;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.batik_nusantara.Kontak;
import com.example.batik_nusantara.MainLogin;
import com.example.batik_nusantara.EditProfile;
import com.example.batik_nusantara.R;
import com.example.batik_nusantara.RegisterAPI;
import com.example.batik_nusantara.ServerAPI;
import com.example.batik_nusantara.databinding.FragmentProfileBinding;

import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    private RegisterAPI registerAPI;
    private TextView tvUsername, tvEmail;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String nama = sharedPreferences.getString("nama", null);

        // Jika belum login (nama kosong atau null)
        if (nama == null || nama.isEmpty()) {
            // Tampilkan alert dialog
            new AlertDialog.Builder(requireContext())
                    .setTitle("Peringatan")
                    .setMessage("Anda harus login dulu untuk mengakses halaman ini.")
                    .setCancelable(false) // Tidak bisa dismiss tanpa klik tombol
                    .setPositiveButton("OK", (dialog, which) -> {
                        // Arahkan ke halaman login dan hapus history activity
                        Intent intent = new Intent(requireActivity(), MainLogin.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    })
                    .show();

            // Kembalikan view kosong agar tidak lanjut render UI profil
            return new View(requireContext());
        }

        loadProfile();

        // Jika sudah login, lanjutkan buat layout dan inisialisasi UI
        ProfileViewModel profileViewModel =
                new ViewModelProvider(this).get(ProfileViewModel.class);

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Tombol Edit Profile
        binding.btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfile.class);
            startActivity(intent);
        });

        // Tombol Kontak Kami
        binding.btnContactUs.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Kontak.class);
            startActivity(intent);
        });

        // Tombol Logout
        binding.btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Logout")
                    .setMessage("Apakah kamu yakin ingin logout?")
                    .setPositiveButton("Ya", (dialog, which) -> {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.clear();
                        editor.apply();

                        Intent intent = new Intent(getActivity(), MainLogin.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        getActivity().finish();
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });

        return root;
    }

    private void loadProfile() {
        Log.d("HomeFragment", "Loading profile...");
        SharedPreferences sharedPreferences = requireActivity()
                .getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String email = sharedPreferences.getString("email", "Guest@gmail.com");

        ServerAPI urlAPI = new ServerAPI();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(urlAPI.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RegisterAPI api = retrofit.create(RegisterAPI.class);
        api.getProfile(email).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject json = new JSONObject(response.body().string());
                        if (json.getInt("result") == 1) {
                            JSONObject data = json.getJSONObject("data");
                            updateUI(
                                    data.getString("nama"),
                                    data.getString("email"),
                                    data.getString("foto")
                            );
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Gagal memuat data profil");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
//                binding.loadingProgressBar.setVisibility(View.GONE);
                showError("Koneksi gagal: " + t.getMessage());
            }
        });
    }

    private void updateUI(String nama, String email, String foto) {
        binding.tvUsername.setText(nama);
        binding.tvEmail.setText(email);
        Glide.with(requireContext())
                .load(BASE_URL_Image + "avatar/" + foto)
                .centerCrop()
                .placeholder(R.drawable.ic_profile_black_24dp)
                .error(R.drawable.ic_profile_black_24dp)
                .into(binding.imgProfilePicture);
    }

    private void showError(String message) {
        if (isAdded()) { // Check if the fragment is attached to an activity
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        } else {
            Log.e("ProfileFragment", "Fragment not attached to context. Error: " + message);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
