package com.example.batik_nusantara;

import static com.example.batik_nusantara.ServerAPI.BASE_URL_Image;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.batik_nusantara.ui.profile.ProfileFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EditProfile extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private String currentPhotoPath, foto;

    private ImageView imgProfile;
    private TextView tvWelcome, btnChangePhoto;;
    private EditText etNama, etAlamat, etKota, etProvinsi, etTelp, etKodePos;
    private Button tvBack, btnSubmit;
    private String email;
    private RegisterAPI api; // Retrofit API global

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Inisialisasi UI
        etNama = findViewById(R.id.etProfileNama);
        etAlamat = findViewById(R.id.etProfile_Alamat);
        etKota = findViewById(R.id.etProfile_Kota);
        etProvinsi = findViewById(R.id.etProfile_Provinsi);
        etTelp = findViewById(R.id.etProfile_Telp);
        etKodePos = findViewById(R.id.etProfile_Kodepos);
        tvWelcome = findViewById(R.id.tvProfil_Welcome);
        tvBack = findViewById(R.id.tvProfile_Back);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnChangePhoto = findViewById(R.id.btnChangeImage);
        imgProfile = findViewById(R.id.imgProfile);

        btnChangePhoto.setOnClickListener(v -> showImagePickerDialog());

        // Retrofit setup sekali saja
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ServerAPI.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(RegisterAPI.class);

        // Ambil data dari SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        email = sharedPreferences.getString("email", "");
        String nama = sharedPreferences.getString("nama", "");
        tvWelcome.setText("Welcome : " + nama + " (" + email + ")");

        // Tombol Kembali
        tvBack.setOnClickListener(v -> {
            Intent intent = new Intent(EditProfile.this, ProfileFragment.class);
            intent.putExtra("fragmentToLoad", "profile");
            startActivity(intent);
            finish();
        });

        // Tombol Simpan
        btnSubmit.setOnClickListener(v -> {
            // Validasi input
            if (etNama.getText().toString().isEmpty() ||
                    etAlamat.getText().toString().isEmpty() ||
                    etKota.getText().toString().isEmpty() ||
                    etProvinsi.getText().toString().isEmpty() ||
                    etTelp.getText().toString().isEmpty() ||
                    etKodePos.getText().toString().isEmpty()) {
                Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show();
                return;
            }

            DataPelanggan data = new DataPelanggan();
            data.setNama(etNama.getText().toString());
            data.setAlamat(etAlamat.getText().toString());
            data.setKota(etKota.getText().toString());
            data.setProvinsi(etProvinsi.getText().toString());
            data.setTelp(etTelp.getText().toString());
            data.setKodepos(etKodePos.getText().toString());
            data.setEmail(email);
            updateProfile(data);
        });

        getProfile(email);
    }

    private void showImagePickerDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Pilih Gambar");
        builder.setItems(new CharSequence[]{"Galeri", "Kamera"}, (dialog, which) -> {
            switch (which) {
                case 0:
                    openGallery();
                    break;
                case 1:
                    openCamera();
                    break;
            }
        });
        builder.show();
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                Uri selectedImageUri = data.getData();
                imgProfile.setImageURI(selectedImageUri);
                uploadImage(selectedImageUri);
            } else if (requestCode == CAMERA_REQUEST) {
                File imgFile = new File(currentPhotoPath);
                if (imgFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    imgProfile.setImageBitmap(bitmap);
                    uploadImage(Uri.fromFile(imgFile));
                }
            }
        }
    }

    private void uploadImage(Uri imageUri) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Mengunggah foto...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        try {
            // Ambil input stream dari URI
            Log.d("UploadImage", "Uri: " + imageUri.toString());
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                progressDialog.dismiss();
                Log.e("UploadImage", "Input stream null");
                Toast.makeText(this, "Gagal membaca gambar", Toast.LENGTH_SHORT).show();
                return;
            }

            File imageFile = getFileFromUri(imageUri);

            // Buat RequestBody dan MultipartBody
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
            MultipartBody.Part fotoPart = MultipartBody.Part.createFormData("foto", imageFile.getName(), requestFile);
            RequestBody emailPart = RequestBody.create(MediaType.parse("text/plain"), email);

            // Inisialisasi Retrofit
            ServerAPI urlAPI = new ServerAPI();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(urlAPI.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            RegisterAPI api = retrofit.create(RegisterAPI.class);
            api.uploadFoto(emailPart, fotoPart).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    progressDialog.dismiss();
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String resString = response.body().string();
                            JSONObject json = new JSONObject(resString);
                            Toast.makeText(EditProfile.this, json.getString("message"), Toast.LENGTH_SHORT).show();

                            if (json.getInt("result") == 1) {
                                // Refresh profil atau aksi lain
                                getProfile(email);
                            }
                        } else {
                            // Coba baca errorBody jika response.body() null
                            String error = response.errorBody() != null ? response.errorBody().string() : "Tidak diketahui";
                            Log.e("UploadFoto", "Error body: " + error);
                            Toast.makeText(EditProfile.this, "Gagal mengunggah: " + error, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(EditProfile.this, "Gagal parsing response", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    progressDialog.dismiss();
                    Toast.makeText(EditProfile.this, "Gagal terhubung: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            progressDialog.dismiss();
            e.printStackTrace();
            Toast.makeText(this, "Terjadi kesalahan saat memproses gambar", Toast.LENGTH_SHORT).show();
        }
    }

    private File getFileFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        File tempFile = File.createTempFile("upload", ".jpg", getCacheDir());
        java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile);
        byte[] buf = new byte[1024];
        int len;
        while ((len = inputStream.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.close();
        inputStream.close();
        return tempFile;
    }

    private void updateProfile(DataPelanggan data) {
        Call<ResponseBody> call = api.updateProfile(
                data.getNama(),
                data.getAlamat(),
                data.getKota(),
                data.getProvinsi(),
                data.getTelp(),
                data.getKodepos(),
                data.getEmail()
        );

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject json = new JSONObject(response.body().string());
                        String message = json.optString("message", "Profil berhasil diperbarui");
                        Toast.makeText(EditProfile.this, message, Toast.LENGTH_SHORT).show();

                        SharedPreferences sharedPreferences = getSharedPreferences("user_session", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("nama", data.getNama());
                        editor.putString("email", data.getEmail());
                        editor.apply();
                    } else {
                        Toast.makeText(EditProfile.this, "Gagal menyimpan profil", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(EditProfile.this, "Terjadi kesalahan saat parsing", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                new AlertDialog.Builder(EditProfile.this)
                        .setMessage("Simpan Gagal: " + t.getMessage())
                        .setNegativeButton("Retry", null)
                        .create()
                        .show();
            }
        });
    }

    private void getProfile(String vemail) {
        Call<ResponseBody> call = api.getProfile(vemail);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject json = new JSONObject(response.body().string());
                        if ("1".equals(json.optString("result"))) {
                            JSONObject data = json.getJSONObject("data");
                            etNama.setText(data.optString("nama", ""));
                            etAlamat.setText(data.optString("alamat", ""));
                            etKota.setText(data.optString("kota", ""));
                            etProvinsi.setText(data.optString("provinsi", ""));
                            etTelp.setText(data.optString("telp", ""));
                            etKodePos.setText(data.optString("kodepos", ""));
                            foto = data.optString("foto","");
                            if (!foto.isEmpty()) {
                                Glide.with(EditProfile.this)
                                        .load(BASE_URL_Image + "avatar/" + foto)
                                        .centerCrop()
                                        .placeholder(R.drawable.ic_profile_black_24dp)
                                        .error(R.drawable.ic_profile_black_24dp)
                                        .into(imgProfile);
                            } else {
                                imgProfile.setImageResource(R.drawable.ic_profile_black_24dp);
                            }
                        } else {
                            Toast.makeText(EditProfile.this, "Data profil tidak ditemukan", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(EditProfile.this, "Gagal memuat profil", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(EditProfile.this, "Error parsing data profil", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(EditProfile.this, "Error mengambil profil: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
