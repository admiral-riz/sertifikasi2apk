package com.example.rizqi_elektronik;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Detail extends AppCompatActivity {

    private Button btnKembali;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Product product = (Product) getIntent().getSerializableExtra("produk");

        ImageView imgDetail = findViewById(R.id.imgDetail);
        TextView tvNamaDetail = findViewById(R.id.tvNamaDetail);
        TextView tvHargaDetail = findViewById(R.id.tvHargaDetail);
        TextView tvStokDetail = findViewById(R.id.tvStokDetail);
        TextView tvKategoriDetail = findViewById(R.id.tvKategoriDetail);
        TextView tvDeskripsiDetail = findViewById(R.id.tvDeskripsiDetail);
        TextView tvViewDetail = findViewById(R.id.tvViewdetail);
        Button btnKembali = findViewById(R.id.button);

        if (product != null) {
            Glide.with(this)
                    .load(ServerAPI.BASE_URL_Image + product.getFoto())
                    .into(imgDetail);

            tvNamaDetail.setText(product.getMerk());
            tvHargaDetail.setText(String.format("Rp %,.0f", product.getHargajual()));
            tvStokDetail.setText("Stok: " + product.getStok());
            tvKategoriDetail.setText("Kategori: " + product.getKategori());
            tvDeskripsiDetail.setText("Deskripsi: " + product.getDeskripsi());

            // Fetch viewCount dari API
            RegisterAPI api = RetrofitClientInstance.getRetrofitInstance().create(RegisterAPI.class);
            Call<ResponseBody> callView = api.getViewCount(product.getKode());
            callView.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String responseStr = response.body().string();
                            int viewCount = Integer.parseInt(responseStr.trim());
                            product.setViewCount(viewCount);
                            tvViewDetail.setText("View : " + viewCount);
                        } catch (Exception e) {
                            tvViewDetail.setText("View : " + product.getViewCount());
                        }
                    } else {
                        tvViewDetail.setText("View : " + product.getViewCount());
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    tvViewDetail.setText("View : " + product.getViewCount());
                }
            });
        }

        btnKembali.setOnClickListener(v -> {
            Intent intent = new Intent(Detail.this, MainActivity.class);
            intent.putExtra("fragmentToLoad", "product");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

}
