package com.example.batik_nusantara;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.batik_nusantara.ui.order.OrderItem;

import java.util.ArrayList;
import java.util.List;

public class OrderAdapterCheckout extends RecyclerView.Adapter<OrderAdapterCheckout.ViewHolder> {

    private Context context;
    private List<OrderItem> orderList;

    public OrderAdapterCheckout(Context context, List<OrderItem> orderItems) {
        this.context = context;
        // Pemeriksaan defensif: pastikan orderList tidak pernah null
        this.orderList = (orderItems != null) ? orderItems : new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_checkout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderItem item = orderList.get(position);
        holder.txtProductName.setText(item.getMerk()); // Menggunakan getMerk() dari OrderItem Anda
        holder.txtProductPrice.setText("Rp " + String.format("%,.0f", item.getHargajual())); // Menggunakan getHargajual()
        holder.txtProductQty.setText("Qty: " + item.getQty()); // Menggunakan getQuantity()

        Glide.with(context)
                .load(new ServerAPI().BASE_URL_Image + item.getFoto())
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(holder.imgProduct);
    }

    @Override
    public int getItemCount() {
        return orderList.size(); // Aman karena orderList dijamin tidak null
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView txtProductName, txtProductPrice, txtProductQty;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            txtProductName = itemView.findViewById(R.id.txtProductName);
            txtProductPrice = itemView.findViewById(R.id.txtProductPrice);
            txtProductQty = itemView.findViewById(R.id.txtProductQty);
        }
    }
}