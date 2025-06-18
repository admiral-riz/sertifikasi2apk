package com.example.batik_nusantara;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainRegister extends AppCompatActivity {
    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnRegister;
    private TextView tvLoginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_register);

        // Inisialisasi View
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmailReg);
        etPassword = findViewById(R.id.etPasswordReg);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        // Aksi tombol Register
        btnRegister.setOnClickListener(v -> prosesSubmit(
                etEmail.getText().toString().trim(),
                etName.getText().toString().trim(),
                etPassword.getText().toString().trim(),
                etConfirmPassword.getText().toString().trim()
        ));

        // Kembali ke Login
        tvLoginLink.setOnClickListener(v -> startActivity(
                new Intent(MainRegister.this, MainLogin.class)
        ));

        // Edge-to-Edge padding
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (view, insets) -> {
                    Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    view.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom);
                    return insets;
                }
        );
    }

    private boolean isEmailValid(String email) {
        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[a-zA-Z]{2,}$";
        Pattern pattern = Pattern.compile(expression);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    private void prosesSubmit(String email, String name, String password, String confirmPassword) {
        if (email.isEmpty() || name.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showMessage("Semua kolom harus diisi!");
            return;
        }

        if (!isEmailValid(email)) {
            showMessage("Email Tidak Valid!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showMessage("Password tidak sama!");
            return;
        }

        // Siapkan Retrofit
        String BASE_URL = new ServerAPI().BASE_URL;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        RegisterAPI api = retrofit.create(RegisterAPI.class);

        // Panggil API Register
        api.register(email, name, password).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String body = response.body().string();
                        Log.d("API Register", body);

                        if (body.startsWith("{")) {
                            JSONObject json = new JSONObject(body);
                            if ("1".equals(json.optString("status"))) {
                                if ("1".equals(json.optString("result"))) {
                                    showMessage("Register Berhasil");
                                    // Bersihkan field
                                    etName.setText("");
                                    etEmail.setText("");
                                    etPassword.setText("");
                                    etConfirmPassword.setText("");
                                } else {
                                    showMessage("Simpan Gagal");
                                }
                            } else {
                                showMessage("User Sudah Ada");
                            }
                        } else {
                            showMessage("Gagal Register! Response bukan JSON.");
                        }
                    } catch (JSONException | IOException e) {
                        e.printStackTrace();
                        showMessage("Error Parsing Response: " + e.getMessage());
                    }
                } else {
                    showMessage("Gagal Register! Response tidak valid.");
                    Log.e("Register Error", "Code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showMessage("Koneksi gagal: " + t.getMessage());
                Log.e("RegisterFailure", t.getMessage(), t);
            }
        });
    }

    private void showMessage(String message) {
        new AlertDialog.Builder(MainRegister.this)
                .setMessage(message)
                .setNegativeButton("OK", null)
                .create()
                .show();
    }
}
