package com.example.rizqi_elektronik.ui.order;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rizqi_elektronik.Checkout;
import com.example.rizqi_elektronik.MainLogin;
import com.example.rizqi_elektronik.OrderAdapter;
import com.example.rizqi_elektronik.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class OrderFragment extends Fragment implements OrderAdapter.OnOrderUpdateListener {

    private RecyclerView recyclerView;
    private OrderAdapter adapter;
    private List<OrderItem> orderItemList = new ArrayList<>();
    private TextView tvTotal;
    private Button btnCheckout;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order, container, false);

        // Inisialisasi komponen
        recyclerView = view.findViewById(R.id.rvCart);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        tvTotal = view.findViewById(R.id.tvTotal);
        btnCheckout = view.findViewById(R.id.btnCheckout);

        // Inisialisasi adapter dengan listener ke fragment ini
        adapter = new OrderAdapter(orderItemList, requireContext(), this);
        recyclerView.setAdapter(adapter);

        // Load item dari SharedPreferences
        loadOrderItems();

        // Set listener tombol checkout
        btnCheckout.setOnClickListener(v -> {
            SharedPreferences sp = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
            boolean isLoggedIn = sp.contains("email");  // cek login berdasar ada email di session

            if (!isLoggedIn) {
                // Tampilkan dialog konfirmasi login dulu dan arahkan ke login
                new AlertDialog.Builder(requireContext())
                        .setTitle("Akses Ditolak")
                        .setMessage("Anda harus login terlebih dahulu untuk melakukan checkout.")
                        .setPositiveButton("Login", (dialog, which) -> {
                            Intent intent = new Intent(getActivity(), MainLogin.class);
                            // Clear stack agar tidak bisa kembali ke OrderFragment tanpa login
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        })
                        .setNegativeButton("Batal", null)
                        .show();
            } else {
                // Jika sudah login, arahkan ke halaman Checkout
                Intent intent = new Intent(getActivity(), Checkout.class);
                startActivity(intent);
            }
        });

        return view;
    }

    private void loadOrderItems() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("OrderPrefs", Context.MODE_PRIVATE);
        String orderJson = sharedPreferences.getString("order_items", "[]");

        // Konversi JSON ke list
        Type type = new TypeToken<List<OrderItem>>() {}.getType();
        List<OrderItem> orderItems = new Gson().fromJson(orderJson, type);

        // Update list dan adapter
        orderItemList.clear();
        orderItemList.addAll(orderItems);
        adapter.notifyDataSetChanged();

        // Update total harga
        updateTotal();
    }

    @Override
    public void updateTotal() {
        double total = 0;
        for (OrderItem item : orderItemList) {
            total += item.getHargajual() * item.getQty();
        }
        tvTotal.setText("Total: Rp " + String.format("%,.0f", total));
    }
}