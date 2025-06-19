package com.example.batik_nusantara;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.batik_nusantara.ui.order.OrderItem;
import com.google.gson.Gson;
// Penting untuk mapping JSON ke Java Object
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

// Tidak lagi dibutuhkan untuk parsing JSON utama

// Tidak lagi dibutuhkan untuk parsing JSON utama

// Hanya untuk parsing profil jika masih menggunakan JSONObject
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Checkout extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Spinner spinnerProvince, spinnerCity, spinnerCourier;
    private TextView tvShippingCost, tvTotalAll, tvSubtotal, tvEstimatedDelivery;
    private Button btnCheckout, btnCheckTax;
    private RadioGroup rgPaymentMethod;
    private TextView etFullName, etAddress, etPhoneNumber, etKodepos;

    private List<OrderItem> orderItems;

    private List<Province> provinceList = new ArrayList<>();
    private List<City> cityList = new ArrayList<>();
    private List<String> courierList = Arrays.asList("jne", "tiki", "pos");

    private int selectedProvinceId = -1;
    private int selectedCityId = -1;
    private String selectedCourier = "jne";
    private int totalWeight = 1000;
    private double taxAmount = 0;
    private double shippingCost = 0;
    private double subtotal = 0;
    private String estimatedDelivery = "-";
    private String selectedPaymentMethod = "COD";

    private static final int originCityId = 181;
    private static final String PHP_API_URL = ServerAPI.BASE_URL + "ongkir.php";
    private static final String PROVINCE_API_URL = ServerAPI.BASE_URL + "get_provinsi.php";
    // Ubah URL kota agar bisa menerima parameter province_id di PHP Anda
    private static final String CITY_API_URL = ServerAPI.BASE_URL + "get_kota.php";

    private OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private String userEmail;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewOrders);
        spinnerProvince = findViewById(R.id.spinnerProvince);
        spinnerCity = findViewById(R.id.spinnerCity);
        spinnerCourier = findViewById(R.id.spinnerCourier);
        tvShippingCost = findViewById(R.id.tvShippingCost);
        tvTotalAll = findViewById(R.id.tvTotalAll);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        btnCheckout = findViewById(R.id.btnCheckout);
        btnCheckTax = findViewById(R.id.btnCheckTax);
        tvEstimatedDelivery = findViewById(R.id.tvEstimatedDelivery);

        etFullName = findViewById(R.id.tvFullName);
        etAddress = findViewById(R.id.tvAddress);
        etPhoneNumber = findViewById(R.id.tvPhoneNumber);
        etKodepos = findViewById(R.id.tvkodepos);

        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);

        // Get user email from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        userEmail = sharedPreferences.getString("email", "");

        // Load user profile
        if (!userEmail.isEmpty()) {
            getProfileForCheckout(userEmail);
        } else {
            Toast.makeText(this, "Email pengguna tidak ditemukan. Data profil tidak dapat dimuat.", Toast.LENGTH_LONG).show();
        }

        // Set payment method listener
        rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selectedRadioButton = findViewById(checkedId);
            if (selectedRadioButton != null) {
                selectedPaymentMethod = selectedRadioButton.getText().toString().equals("Cash On Delivery (COD)") ? "COD" : "Transfer Bank";
            }
        });

        // Get order items from intent
        String json = getIntent().getStringExtra("order_items");
        if (json != null && !json.isEmpty()) {
            try {
                orderItems = new Gson().fromJson(json, new TypeToken<List<OrderItem>>(){}.getType());
            } catch (Exception e) {
                Log.e("Checkout", "Error parsing order list JSON: " + e.getMessage());
                orderItems = new ArrayList<>();
                Toast.makeText(this, "Gagal memuat daftar pesanan.", Toast.LENGTH_LONG).show();
            }
        } else {
            orderItems = new ArrayList<>();
            Toast.makeText(this, "Daftar pesanan kosong.", Toast.LENGTH_SHORT).show();
        }

        // Setup recyclerview
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new OrderAdapterCheckout(this, orderItems));

        // Calculate subtotal
        calculateTotals();

        // Setup courier spinner
        ArrayAdapter<String> courierAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, courierList);
        courierAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourier.setAdapter(courierAdapter);

        spinnerCourier.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCourier = courierList.get(position);
                shippingCost = 0;
                taxAmount = 0;
                estimatedDelivery = "-";
                updatePaymentDetails();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Load provinces
        loadProvinces();

        // Province spinner listener
        spinnerProvince.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    Province p = provinceList.get(position - 1);
                    selectedProvinceId = Integer.parseInt(p.getProvince_id());
                    // Panggil loadCities setelah provinsi dipilih, sekarang kirim ID provinsi
                    loadCities(selectedProvinceId);
                } else {
                    cityList.clear(); // Bersihkan daftar kota jika "Pilih Provinsi" dipilih
                    updateCitySpinner();
                    selectedProvinceId = -1;
                    selectedCityId = -1; // Reset city ID saat provinsi tidak dipilih
                }
                shippingCost = 0;
                taxAmount = 0;
                estimatedDelivery = "-";
                updatePaymentDetails();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // City spinner listener
        spinnerCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    City c = cityList.get(position - 1);
                    try {
                        // Sekarang city_id seharusnya sudah ada dan valid dari JSON
                        selectedCityId = Integer.parseInt(c.getCity_id());
                        Log.d("CitySpinner", "Selected City: " + c.getCity_name() + ", ID: " + selectedCityId);
                    } catch (NumberFormatException e) {
                        selectedCityId = -1;
                        Log.e("CitySpinner", "Invalid city ID format for " + c.getCity_name() + ": " + c.getCity_id());
                        Toast.makeText(Checkout.this, "ID kota tidak valid: " + c.getCity_name(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    selectedCityId = -1;
                }
                shippingCost = 0;
                taxAmount = 0;
                estimatedDelivery = "-";
                updatePaymentDetails();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Check tax button listener
        btnCheckTax.setOnClickListener(v -> {
            if (selectedProvinceId == -1) {
                Toast.makeText(Checkout.this, "Pilih provinsi tujuan terlebih dahulu", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedCityId == -1) {
                Toast.makeText(Checkout.this, "Pilih kota tujuan terlebih dahulu", Toast.LENGTH_SHORT).show();
                return;
            }
            getCost();
        });

        // Checkout button listener
        btnCheckout.setOnClickListener(v -> {
            // Validate personal information
            if (etFullName.getText().toString().trim().isEmpty() ||
                    etAddress.getText().toString().trim().isEmpty() ||
                    etPhoneNumber.getText().toString().trim().isEmpty() ||
                    etKodepos.getText().toString().trim().isEmpty()) {
                Toast.makeText(Checkout.this, "Mohon lengkapi informasi pribadi Anda", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedProvinceId == -1 || selectedCityId == -1) {
                Toast.makeText(Checkout.this, "Pilih provinsi dan kota tujuan terlebih dahulu", Toast.LENGTH_SHORT).show();
                return;
            }
            if (shippingCost == 0 && totalWeight > 0) { // Jika berat ada, tapi ongkir 0, berarti belum cek ongkir
                Toast.makeText(Checkout.this, "Silakan cek ongkir", Toast.LENGTH_SHORT).show();
                return;
            }
            double finalTotalPayment = subtotal + shippingCost + taxAmount;
            String checkoutMessage = String.format("Checkout berhasil!\nNama: %s\nAlamat: %s\nTelepon: %s\nMetode Pembayaran: %s\nTotal: Rp %,.0f",
                    etFullName.getText().toString().trim(),
                    etAddress.getText().toString().trim(),
                    etPhoneNumber.getText().toString().trim(),
                    etKodepos.getText().toString().trim(),
                    selectedPaymentMethod,
                    finalTotalPayment);
            Toast.makeText(Checkout.this, checkoutMessage, Toast.LENGTH_LONG).show();
        });
    }

    private void getProfileForCheckout(String email) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ServerAPI.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RegisterAPI api = retrofit.create(RegisterAPI.class);
        api.getProfile(email).enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        if (json.getString("result").equals("1")) {
                            JSONObject data = json.getJSONObject("data");
                            etFullName.setText(data.getString("nama"));
                            etAddress.setText(data.getString("alamat"));
                            etPhoneNumber.setText(data.getString("telp"));
                            etKodepos.setText(data.getString("kodepos"));
                            Log.d("CheckoutProfile", "Data profil berhasil dimuat dari server.");
                        } else {
                            Toast.makeText(Checkout.this, "Data profil tidak ditemukan.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("CheckoutProfile", "Error parsing profile data: " + e.getMessage());
                        Toast.makeText(Checkout.this, "Terjadi kesalahan saat memuat data profil.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(Checkout.this, "Gagal mengambil data profil dari server.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("CheckoutProfile", "Gagal terhubung ke server untuk profil: " + t.getMessage());
                Toast.makeText(Checkout.this, "Terjadi kesalahan koneksi saat memuat profil.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateTotals() {
        subtotal = 0;
        for (OrderItem order : orderItems) {
            subtotal += order.getHargajual() * order.getQty();
        }
        tvSubtotal.setText(String.format("Subtotal: Rp %,.0f", subtotal));
        updatePaymentDetails();
    }


    private void loadProvinces() {
        Request request = new Request.Builder()
                .url(PROVINCE_API_URL)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(Checkout.this, "Gagal terhubung ke server provinsi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("LoadProvinces", "Connection error", e);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }

                    String resStr = response.body().string();
                    Log.d("ProvinceResponse", "Raw JSON: " + resStr);

                    // --- Menggunakan Gson untuk mem-parse JSON ---
                    // Perhatikan bahwa kelas RajaOngkirResponseProvince dan Province harus sesuai dengan struktur JSON
                    RajaOngkirResponseProvince provResponse = new Gson().fromJson(resStr, RajaOngkirResponseProvince.class);

                    if (provResponse != null && provResponse.getRajaongkir() != null && provResponse.getRajaongkir().getResults() != null) {
                        provinceList = provResponse.getRajaongkir().getResults();
                        runOnUiThread(() -> {
                            List<String> names = new ArrayList<>();
                            names.add("Pilih Provinsi");
                            for (Province p : provinceList) {
                                names.add(p.getProvince());
                            }
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(Checkout.this,
                                    android.R.layout.simple_spinner_item, names);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerProvince.setAdapter(adapter);
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(Checkout.this, "Data provinsi kosong atau tidak valid.", Toast.LENGTH_SHORT).show());
                    }

                } catch (Exception e) {
                    runOnUiThread(() -> {
                        String errorMsg = "Error parsing provinsi JSON: " + e.getMessage();
                        Toast.makeText(Checkout.this, errorMsg, Toast.LENGTH_LONG).show();
                        Log.e("ParseProvinces", errorMsg, e);
                    });
                } finally {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }


    private void loadCities(int provinceId) {
        // Panggil API PHP kota Anda dengan parameter province_id
        // Asumsi PHP API Anda sudah dimodifikasi untuk menerima '?province=ID'
        Request request = new Request.Builder()
                .url(CITY_API_URL + "?province_id=" + provinceId) // Mengirim ID provinsi ke PHP API
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(Checkout.this, "Gagal load kota: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String resStr = response.body().string();
                    Log.d("CityResponse", "Raw JSON: " + resStr);
                    try {
                        // --- Menggunakan Gson untuk mem-parse JSON ---
                        RajaOngkirResponseCity cityRes = new Gson().fromJson(resStr, RajaOngkirResponseCity.class);

                        if (cityRes != null && cityRes.getRajaongkir() != null && cityRes.getRajaongkir().getResults() != null) {
                            cityList = cityRes.getRajaongkir().getResults();
                            runOnUiThread(() -> updateCitySpinner());
                        } else {
                            runOnUiThread(() -> Toast.makeText(Checkout.this, "Data kota kosong atau tidak valid.", Toast.LENGTH_SHORT).show());
                            cityList.clear(); // Bersihkan daftar kota jika tidak ada data
                            updateCitySpinner();
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(Checkout.this, "Error parsing kota JSON: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        Log.e("ParseCities", "Error parsing JSON: " + e.getMessage(), e);
                        cityList.clear(); // Bersihkan daftar kota jika error parsing
                        updateCitySpinner();
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(Checkout.this, "Gagal mendapatkan data kota dari server. Kode: " + response.code(), Toast.LENGTH_SHORT).show();
                        cityList.clear(); // Bersihkan daftar kota jika respons tidak berhasil
                        updateCitySpinner();
                    });
                }
            }
        });
    }

    private void updateCitySpinner() {
        List<String> cityNames = new ArrayList<>();
        cityNames.add("Pilih Kota");
        for (City c : cityList) {
            // Gabungkan type dan city_name untuk tampilan yang informatif
            cityNames.add(c.getType() + " " + c.getCity_name());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, cityNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCity.setAdapter(adapter);
    }

    // Metode getCityIdFromName() dihapus karena tidak lagi relevan
    // karena city.city_id sekarang langsung didapat dari parsing JSON.

    private void getCost() {
        if (selectedCityId == -1) {
            Toast.makeText(Checkout.this, "ID kota tujuan belum valid. Harap pastikan Anda memilih kota dan API kota Anda mengembalikan ID yang benar.", Toast.LENGTH_LONG).show();
            return;
        }

        String formBody = "origin=" + originCityId +
                "&destination=" + selectedCityId +
                "&weight=" + totalWeight +
                "&courier=" + selectedCourier;

        RequestBody body = RequestBody.create(
                MediaType.parse("application/x-www-form-urlencoded"),
                formBody
        );

        Request request = new Request.Builder()
                .url(PHP_API_URL)
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(Checkout.this, "Gagal terhubung ke server: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    resetShippingCost();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String resStr = response.body().string();
                    try {
                        // Pastikan CostResponse dan struktur kelasnya sesuai dengan output ongkir.php
                        CostResponse costResponse = new Gson().fromJson(resStr, CostResponse.class);
                        if (costResponse != null && costResponse.rajaongkir != null) {
                            if (costResponse.rajaongkir.results != null && !costResponse.rajaongkir.results.isEmpty()) {
                                // RajaOngkir bisa mengembalikan beberapa kurir/layanan
                                // Kita ambil yang pertama atau cari yang spesifik jika ada kriteria
                                CostResponse.Result result = costResponse.rajaongkir.results.get(0);
                                if (result.costs != null && !result.costs.isEmpty()) {
                                    CostResponse.Service service = result.costs.get(0); // Ambil layanan pertama (misal reguler)
                                    if (service.cost != null && !service.cost.isEmpty()) {
                                        shippingCost = service.cost.get(0).value;
                                        estimatedDelivery = service.cost.get(0).etd;
                                        runOnUiThread(() -> updatePaymentDetails());
                                        return; // Berhasil mendapatkan ongkir
                                    }
                                }
                            }
                        }
                        // Jika sampai sini, berarti data ongkir tidak ditemukan atau tidak valid
                        runOnUiThread(() -> {
                            Toast.makeText(Checkout.this, "Tidak dapat menemukan layanan pengiriman untuk tujuan ini.", Toast.LENGTH_SHORT).show();
                            resetShippingCost();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            Toast.makeText(Checkout.this, "Error parsing response ongkir: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            resetShippingCost();
                        });
                        Log.e("GetCost", "Error parsing ongkir JSON: " + e.getMessage(), e);
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(Checkout.this, "Gagal mendapatkan ongkir dari server. Kode: " + response.code(), Toast.LENGTH_SHORT).show();
                        resetShippingCost();
                    });
                    Log.e("GetCost", "Failed to get ongkir, response code: " + response.code());
                }
            }
        });
    }

    private void resetShippingCost() {
        shippingCost = 0;
        taxAmount = 0; // Pastikan taxAmount juga direset jika tidak ada ongkir
        estimatedDelivery = "-";
        runOnUiThread(() -> updatePaymentDetails());
    }

    private void updatePaymentDetails() {
        tvShippingCost.setText(String.format("Ongkir: Rp %,.0f", shippingCost));
        tvEstimatedDelivery.setText(String.format("Estimasi Pengiriman: %s hari", estimatedDelivery));

        // Jika Anda memiliki pajak, tambahkan di sini
        // taxAmount = subtotal * taxRate; // Contoh perhitungan pajak

        double totalPayment = subtotal + shippingCost + taxAmount;
        tvTotalAll.setText(String.format("Total: Rp %,.0f", totalPayment));
    }


    // Kelas ini merepresentasikan struktur respons JSON utama dari RajaOngkir
    // Untuk API Provinsi: {"rajaongkir": { "results": [...] }}
    public static class RajaOngkirResponseProvince {
        private Rajaongkir rajaongkir; // Perhatikan nama field harus cocok dengan JSON
        public Rajaongkir getRajaongkir() { return rajaongkir; }

        public static class Rajaongkir {
            private List<Province> results; // List dari objek Province
            public List<Province> getResults() { return results; }
        }
    }

    // Kelas ini merepresentasikan objek Provinsi di dalam array 'results'
    public static class Province {
        @SerializedName("province_id") // Pastikan mapping jika nama field JSON berbeda dengan nama variabel Java
        private String province_id;
        @SerializedName("province")
        private String province;

        public String getProvince_id() { return province_id; }
        public String getProvince() { return province; }
    }

    // Kelas ini merepresentasikan struktur respons JSON utama dari RajaOngkir
    // Untuk API Kota: {"rajaongkir": { "results": [...] }}
    public static class RajaOngkirResponseCity {
        private Rajaongkir rajaongkir;
        public Rajaongkir getRajaongkir() { return rajaongkir; }

        public static class Rajaongkir {
            private List<City> results; // List dari objek City
            public List<City> getResults() { return results; }
        }
    }

    // Kelas ini merepresentasikan objek Kota di dalam array 'results'
    public static class City {
        @SerializedName("city_id") // Mapping ke 'city_id' dari JSON
        private String city_id;
        @SerializedName("province_id") // Mapping ke 'province_id' dari JSON
        private String province_id;
        @SerializedName("province") // Mapping ke 'province' (nama provinsi) dari JSON
        private String province;
        @SerializedName("type")
        private String type;
        @SerializedName("city_name") // Mapping ke 'city_name' dari JSON
        private String city_name;
        @SerializedName("postal_code")
        private String postal_code;

        public String getCity_id() { return city_id; }
        public String getProvince_id() { return province_id; }
        public String getProvince() { return province; }
        public String getType() { return type; }
        public String getCity_name() { return city_name; }
        public String getPostal_code() { return postal_code; }

        // Setter (tidak selalu dibutuhkan jika data hanya dibaca, tapi bisa ditambahkan)
        // public void setCity_id(String city_id) { this.city_id = city_id; }
        // ...
    }

    // Kelas-kelas untuk respons ongkir (tidak ada perubahan signifikan jika struktur RajaOngkir sama)
    public static class CostResponse {
        Rajaongkir rajaongkir;
        public static class Rajaongkir {
            List<Result> results;
        }
        public static class Result {
            List<Service> costs;
        }
        public static class Service {
            List<CostDetail> cost;
        }
        public static class CostDetail {
            int value;
            String etd;
        }
    }
}