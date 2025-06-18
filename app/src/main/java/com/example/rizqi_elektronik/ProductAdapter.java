package com.example.rizqi_elektronik;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.rizqi_elektronik.ui.order.OrderItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
    private List<Product> productList;
    private Context context;

    public ProductAdapter(List<Product> productList, Context context) {
        this.productList = productList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);

        // Set data produk dasar
        holder.tvMerk.setText(product.getMerk());
        holder.tvPrice.setText(String.format("Rp %,.0f", product.getHargajual()));
        holder.tvStock.setText(product.getStok() <= 0 ? "Stok: Habis" : "Stok: " + product.getStok());

        if (product.getStok() <= 0) {
            holder.tvStock.setTextColor(Color.RED);
            holder.imgbtnCart.setVisibility(View.INVISIBLE);
        } else {
            holder.tvStock.setTextColor(Color.rgb(0, 100, 0));
            holder.imgbtnCart.setVisibility(View.VISIBLE);
        }

        Glide.with(context)
                .load(ServerAPI.BASE_URL_Image + product.getFoto())
                .into(holder.ivProduct);

        // Panggil API untuk mendapatkan viewCount terbaru
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
                        holder.tvViewProduct.setText("View : " + viewCount);
                    } catch (Exception e) {
                        // fallback jika parsing gagal
                        holder.tvViewProduct.setText("View : " + product.getViewCount());
                    }
                } else {
                    holder.tvViewProduct.setText("View : " + product.getViewCount());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                holder.tvViewProduct.setText("View : " + product.getViewCount());
            }
        });

        // event tombol keranjang dll tetap seperti sebelumnya...

        holder.imgbtnCart.setOnClickListener(v -> {
            if (product.getStok() > 0) {
                OrderItem orderItem = new OrderItem(
                        product.getFoto(),
                        product.getMerk(),
                        product.getHargajual(),
                        product.getStok(),
                        1
                );
                saveProductToOrder(orderItem);
            } else {
                Toast.makeText(context, "Stok produk kosong", Toast.LENGTH_SHORT).show();
            }
        });

        holder.imgbtnDeskripsi.setOnClickListener(v -> {
            RegisterAPI api2 = RetrofitClientInstance.getRetrofitInstance().create(RegisterAPI.class);
            Call<ResponseBody> call = api2.updateView(product.getKode());
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        // Update lokal viewCount (tambahkan 1)
                        product.setViewCount(product.getViewCount() + 1);
                        holder.tvViewProduct.setText("View : " + product.getViewCount());

                        Intent intent = new Intent(context, Detail.class);
                        intent.putExtra("produk", product);
                        context.startActivity(intent);
                    } else {
                        Toast.makeText(context, "Gagal update view", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }


    @Override
    public int getItemCount() {
        return productList.size();
    }

    private void saveProductToOrder(OrderItem orderItem) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("OrderPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String orderJson = sharedPreferences.getString("order_items", "[]");
        Type type = new TypeToken<List<OrderItem>>() {}.getType();
        List<OrderItem> orderItems;

        try {
            orderItems = new Gson().fromJson(orderJson, type);
            if (orderItems == null) {
                orderItems = new ArrayList<>();
            }
        } catch (Exception e) {
            orderItems = new ArrayList<>();
            android.util.Log.e("ProductAdapter", "Gagal parsing JSON: " + e.getMessage());
        }

        boolean productExists = false;
        for (OrderItem item : orderItems) {
            if (item.getMerk().equals(orderItem.getMerk())) {
                item.setQty(item.getQty() + 1);
                productExists = true;
                break;
            }
        }

        if (!productExists) {
            orderItems.add(orderItem);
        }

        String updatedOrderJson = new Gson().toJson(orderItems);
        editor.putString("order_items", updatedOrderJson);

        boolean success = editor.commit();
        if (success) {
            Toast.makeText(context, "Berhasil menambahkan ke keranjang", Toast.LENGTH_SHORT).show();
            android.util.Log.d("ProductAdapter", "Data tersimpan ke SharedPreferences: " + updatedOrderJson);
        } else {
            Toast.makeText(context, "Gagal menyimpan ke keranjang", Toast.LENGTH_SHORT).show();
            android.util.Log.e("ProductAdapter", "Gagal menyimpan ke SharedPreferences");
        }
    }

    // Method untuk menghitung total harga keranjang
    public static double calculateTotal(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("OrderPrefs", Context.MODE_PRIVATE);
        String orderJson = sharedPreferences.getString("order_items", "[]");
        Type type = new TypeToken<List<OrderItem>>() {}.getType();
        List<OrderItem> orderItems;

        try {
            orderItems = new Gson().fromJson(orderJson, type);
            if (orderItems == null) {
                orderItems = new ArrayList<>();
            }
        } catch (Exception e) {
            orderItems = new ArrayList<>();
            android.util.Log.e("ProductAdapter", "Gagal parsing JSON: " + e.getMessage());
        }

        double total = 0;
        for (OrderItem item : orderItems) {
            total += item.getHargajual() * item.getQty();  // Harga * Kuantitas
        }
        return total;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProduct, imgbtnCart, imgbtnDeskripsi;
        TextView tvMerk, tvPrice, tvStock, tvViewProduct;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProduct = itemView.findViewById(R.id.imageView);
            tvMerk = itemView.findViewById(R.id.tvNama);
            tvPrice = itemView.findViewById(R.id.tvHarga);
            tvStock = itemView.findViewById(R.id.tvStok);
            tvViewProduct = itemView.findViewById(R.id.tvViewProduct); // Pastikan ada TextView untuk view count di layout XML
            imgbtnCart = itemView.findViewById(R.id.imgbtnCart);
            imgbtnDeskripsi = itemView.findViewById(R.id.imgbtnDeskripsi);
        }
    }
}
