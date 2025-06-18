package com.example.rizqi_elektronik.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.rizqi_elektronik.Product;
import com.example.rizqi_elektronik.R;
import com.example.rizqi_elektronik.ServerAPI;
import com.example.rizqi_elektronik.RegisterAPI;
import com.example.rizqi_elektronik.RetrofitClientInstance;
import com.example.rizqi_elektronik.Detail;
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

public class BestSellerAdapter extends RecyclerView.Adapter<BestSellerAdapter.BestSellerViewHolder> {

    private List<Product> productList = new ArrayList<>();

    public void setProductList(List<Product> products) {
        products.sort((p1, p2) -> Integer.compare(p2.getViewCount(), p1.getViewCount())); // descending
        this.productList = products;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BestSellerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_best_seller, parent, false);
        return new BestSellerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BestSellerViewHolder holder, int position) {
        holder.bind(productList.get(position));
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class BestSellerViewHolder extends RecyclerView.ViewHolder {

        private ImageView imgBestSeller, imgBtnBestAddToCart, imgBtnBestDescription;
        private TextView tvBestSellerName, tvBestSellerPrice, tvBestSellerViews;
        private Context context;

        public BestSellerViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();

            imgBestSeller = itemView.findViewById(R.id.imgBestSeller);
            imgBtnBestAddToCart = itemView.findViewById(R.id.imgBtnBestAddToCart);
            imgBtnBestDescription = itemView.findViewById(R.id.imgBtnBestDescription); // pastikan ada di layout
            tvBestSellerName = itemView.findViewById(R.id.tvBestSellerName);
            tvBestSellerPrice = itemView.findViewById(R.id.tvBestSellerPrice);
            tvBestSellerViews = itemView.findViewById(R.id.tvBestSellerViews);
        }

        public void bind(Product product) {
            tvBestSellerName.setText(product.getMerk());
            tvBestSellerPrice.setText("Rp " + String.format("%,.0f", product.getHargajual()));

            String imageUrl = ServerAPI.BASE_URL_Image + product.getFoto();
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.color.primary)
                    .error(R.drawable.ic_produk_black_24dp)
                    .into(imgBestSeller);

            RegisterAPI api = RetrofitClientInstance.getRetrofitInstance().create(RegisterAPI.class);

            // Ambil view count dari server
            Call<ResponseBody> callView = api.getViewCount(product.getKode());
            callView.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String responseStr = response.body().string();
                            int viewCount = Integer.parseInt(responseStr.trim());
                            product.setViewCount(viewCount);
                            tvBestSellerViews.setText("View: " + viewCount);
                        } catch (Exception e) {
                            tvBestSellerViews.setText("View: " + product.getViewCount());
                        }
                    } else {
                        tvBestSellerViews.setText("View: " + product.getViewCount());
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    tvBestSellerViews.setText("View: " + product.getViewCount());
                }
            });

            imgBtnBestAddToCart.setOnClickListener(v -> {
                if (product.getStok() > 0) {
                    saveProductToOrder(product);
                } else {
                    Toast.makeText(context, "Stok produk kosong", Toast.LENGTH_SHORT).show();
                }
            });

            imgBtnBestDescription.setOnClickListener(v -> {
                // Update view count di server saat buka detail
                Call<ResponseBody> callUpdateView = api.updateView(product.getKode());
                callUpdateView.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            product.setViewCount(product.getViewCount() + 1);
                            tvBestSellerViews.setText("View: " + product.getViewCount());

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

        private void saveProductToOrder(Product product) {
            SharedPreferences sharedPreferences = context.getSharedPreferences("OrderPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            String orderJson = sharedPreferences.getString("order_items", "[]");
            Type type = new TypeToken<List<OrderItem>>() {}.getType();
            List<OrderItem> orderItems;

            try {
                orderItems = new Gson().fromJson(orderJson, type);
                if (orderItems == null) orderItems = new ArrayList<>();
            } catch (Exception e) {
                orderItems = new ArrayList<>();
            }

            boolean productExists = false;
            for (OrderItem item : orderItems) {
                if (item.getMerk().equals(product.getMerk())) {
                    item.setQty(item.getQty() + 1);
                    productExists = true;
                    break;
                }
            }

            if (!productExists) {
                orderItems.add(new OrderItem(
                        product.getFoto(),
                        product.getMerk(),
                        product.getHargajual(),
                        product.getStok(),
                        1
                ));
            }

            String updatedOrderJson = new Gson().toJson(orderItems);
            editor.putString("order_items", updatedOrderJson);
            boolean success = editor.commit();

            if (success) {
                Toast.makeText(context, "Berhasil menambahkan ke keranjang", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Gagal menyimpan ke keranjang", Toast.LENGTH_SHORT).show();
            }
        }
    }
}