package com.example.batik_nusantara.ui.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.batik_nusantara.R;
import com.example.batik_nusantara.RegisterAPI;
import com.example.batik_nusantara.ServerAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class EditProfileFragment extends Fragment {
    public static final String URL = new ServerAPI().BASE_URL;

    private String nama, email;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    EditText etProfile_Nama, etProfile_Email, etProfile_Alamat, etProfile_Kota, etProfile_Provinsi, etProfile_Telp, etProfile_Kodepos;
    Button btnSubmit, btnLogout;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        SharedPreferences sharedPreferences = requireActivity()
                .getSharedPreferences("user_session", Context.MODE_PRIVATE);

        editor = sharedPreferences.edit();
        email = sharedPreferences.getString("email", "");
        nama = sharedPreferences.getString("nama", "");


        TextView tvProfil_Welcome = view.findViewById(R.id.tvProfil_Welcome);
        tvProfil_Welcome.setText("Selamat Datang " + nama);

        etProfile_Nama = view.findViewById(R.id.etProfileNama);
        etProfile_Alamat = view.findViewById(R.id.etProfile_Alamat);
        etProfile_Kota = view.findViewById(R.id.etProfile_Kota);
        etProfile_Provinsi = view.findViewById(R.id.etProfile_Provinsi);
        etProfile_Telp = view.findViewById(R.id.etProfile_Telp);
        etProfile_Kodepos = view.findViewById(R.id.etProfile_Kodepos);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        Log.d("EditProfileFragment", "Email: " + email);

        getProfil(email);

        btnSubmit.setOnClickListener(v -> updateProfil());

        setHasOptionsMenu(true);

        if (requireActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) requireActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        return view;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { // ID default untuk tombol back
            NavController navController = Navigation.findNavController(requireView());
            navController.popBackStack(); // Kembali ke fragment sebelumnya
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateProfil() {
        DataUser data = new DataUser();
        data.setNama(etProfile_Nama.getText().toString().trim());
        data.setAlamat(etProfile_Alamat.getText().toString().trim());
        data.setKota(etProfile_Kota.getText().toString().trim());
        data.setProvinsi(etProfile_Provinsi.getText().toString().trim());
        data.setTelp(etProfile_Telp.getText().toString().trim());
        data.setKodepos(etProfile_Kodepos.getText().toString().trim());
        data.setEmail(email);

        ServerAPI urlAPI = new ServerAPI();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(urlAPI.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RegisterAPI api = retrofit.create(RegisterAPI.class);
        api.updateProfile(data.getNama(), data.getAlamat(), data.getKota(), data.getProvinsi(),
                        data.getTelp(), data.getKodepos(), data.getEmail())
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        try {
                            if (response.body() != null) {
                                JSONObject json = new JSONObject(response.body().string());

                                Toast.makeText(requireContext(), json.getString("message"), Toast.LENGTH_SHORT).show();
                                getProfil(data.getEmail());

                                String nama = json.getJSONObject("data").getString("nama");
                                String email = json.getJSONObject("data").getString("email");

                                // Simpan data login ke SharedPreferences
                                SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("login_session", Context.MODE_PRIVATE);
                                editor = sharedPreferences.edit();
                                editor.putString("nama", nama);
                                editor.putString("email", email);
                                editor.apply();
                            }
                        } catch (JSONException | IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        new AlertDialog.Builder(requireContext())
                                .setMessage("Simpan Gagal, Error: " + t.toString())
                                .setNegativeButton("Retry", null)
                                .create()
                                .show();
                    }
                });
    }

    private void getProfil(String vemail) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        RegisterAPI api = retrofit.create(RegisterAPI.class);

        api.getProfile(vemail).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();

                        if (responseBody.trim().startsWith("{")) {
                            JSONObject json = new JSONObject(responseBody);

                            if ("1".equals(json.optString("result"))) {
                                JSONObject data = json.optJSONObject("data");
                                if (data != null) {
                                    requireActivity().runOnUiThread(() -> {
                                        etProfile_Nama.setText(getValidString(data, "nama"));
                                        etProfile_Alamat.setText(getValidString(data, "alamat"));
                                        etProfile_Kota.setText(getValidString(data, "kota"));
                                        etProfile_Provinsi.setText(getValidString(data, "provinsi"));
                                        etProfile_Telp.setText(getValidString(data, "telp"));
                                        etProfile_Kodepos.setText(getValidString(data, "kodepos"));
                                    });
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private String getValidString(JSONObject json, String key) {
        try {
            if (json.has(key) && !json.isNull(key)) {
                return json.getString(key);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }
}