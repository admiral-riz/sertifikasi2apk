package com.example.rizqi_elektronik;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.rizqi_elektronik.ui.order.OrderItem;
import com.google.gson.Gson;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    private List<OrderItem> orderItemList;
    private Context context;
    private OnOrderUpdateListener onOrderUpdateListener;

    public OrderAdapter(List<OrderItem> orderItemList, Context context, OnOrderUpdateListener onOrderUpdateListener) {
        this.orderItemList = orderItemList;
        this.context = context;
        this.onOrderUpdateListener = onOrderUpdateListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderItem orderItem = orderItemList.get(position);

        holder.tvOrderNama.setText(orderItem.getMerk());
        holder.tvOrderHarga.setText(String.format("Rp %,.0f", orderItem.getHargajual()));
        holder.tvQty.setText("" + orderItem.getQty());
        holder.tvOrderSubtotal.setText(String.format("Subtotal: Rp %,.0f", orderItem.getHargajual() * orderItem.getQty()));

        // Load image using Glide
        Glide.with(holder.itemView.getContext())
                .load(ServerAPI.BASE_URL_Image + orderItem.getFoto())
                .into(holder.imgOrder);

        // Handle remove item click
        holder.btnHapus.setOnClickListener(v -> {
            // Show confirmation dialog before removing the item
            new AlertDialog.Builder(context)
                    .setMessage("Yakin ingin menghapus pesanan?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", (dialog, id) -> removeProductFromOrder(position))
                    .setNegativeButton("No", null)
                    .show();
        });

        // Increase or decrease quantity
        holder.btnPlus.setOnClickListener(v -> updateQuantity(position, orderItem.getQty() + 1));
        holder.btnMinus.setOnClickListener(v -> {
            if (orderItem.getQty() > 1) {
                updateQuantity(position, orderItem.getQty() - 1);
            } else if (orderItem.getQty() == 1) {
                removeProductFromOrder(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return orderItemList.size();
    }

    private void removeProductFromOrder(int position) {
        if (position >= 0 && position < orderItemList.size()) {
            orderItemList.remove(position);

            SharedPreferences sharedPreferences = context.getSharedPreferences("OrderPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            String updatedOrderJson = new Gson().toJson(orderItemList);
            editor.putString("order_items", updatedOrderJson);
            editor.apply();

            notifyItemRemoved(position);

            if (orderItemList.isEmpty()) {
                editor.remove("order_items").apply();
            }

            // Notify the fragment to update the total
            if (onOrderUpdateListener != null) {
                onOrderUpdateListener.updateTotal();
            }
        } else {
            Log.e("OrderAdapter", "Invalid position: " + position);
        }
    }

    private void updateQuantity(int position, int newQty) {
        OrderItem orderItem = orderItemList.get(position);
        orderItem.setQty(newQty);

        SharedPreferences sharedPreferences = context.getSharedPreferences("OrderPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String updatedOrderJson = new Gson().toJson(orderItemList);
        editor.putString("order_items", updatedOrderJson);
        editor.apply();

        notifyItemChanged(position);

        // Notify the fragment to update the total
        if (onOrderUpdateListener != null) {
            onOrderUpdateListener.updateTotal();
        }
    }

    public interface OnOrderUpdateListener {
        void updateTotal();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgOrder;
        TextView tvOrderNama, tvOrderHarga, tvQty, tvOrderSubtotal;
        ImageButton btnHapus, btnPlus, btnMinus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgOrder = itemView.findViewById(R.id.imgOrder);
            tvOrderNama = itemView.findViewById(R.id.tvOrderNama);
            tvOrderHarga = itemView.findViewById(R.id.tvOrderHarga);
            tvQty = itemView.findViewById(R.id.tvQty);
            tvOrderSubtotal = itemView.findViewById(R.id.tvOrderSubtotal);
            btnHapus = itemView.findViewById(R.id.btnHapus);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnMinus = itemView.findViewById(R.id.btnMinus);
        }
    }
}
