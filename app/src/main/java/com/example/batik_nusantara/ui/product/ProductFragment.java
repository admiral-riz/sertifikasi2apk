package com.example.batik_nusantara.ui.product;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.batik_nusantara.Product;
import com.example.batik_nusantara.ProductAdapter;
import com.example.batik_nusantara.R;
import com.example.batik_nusantara.RegisterAPI;
import com.example.batik_nusantara.ServerAPI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProductFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private List<Product> allProducts = new ArrayList<>();
    private List<Product> filteredProducts = new ArrayList<>();

    private SearchView searchView;
    private Spinner spinnerKategori;

    private RegisterAPI api;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product, container, false);

        // Inisialisasi UI
        searchView = view.findViewById(R.id.searchView);
        spinnerKategori = view.findViewById(R.id.spinnerKategori);
        recyclerView = view.findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new ProductAdapter(filteredProducts, getContext());
        recyclerView.setAdapter(adapter);

        // Setup Retrofit & API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ServerAPI.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(RegisterAPI.class);

        loadProducts();
        setupSearchListener();

        return view;
    }

    private void loadProducts() {
        Call<List<Product>> call = api.getProducts();

        call.enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allProducts.clear();
                    allProducts.addAll(response.body());

                    filteredProducts.clear();
                    filteredProducts.addAll(allProducts);
                    adapter.notifyDataSetChanged();

                    // Ambil kategori unik (hindari null)
                    Set<String> kategoriSet = new HashSet<>();
                    for (Product p : allProducts) {
                        if (p.getKategori() != null) {
                            kategoriSet.add(p.getKategori());
                        }
                    }

                    List<String> kategoriList = new ArrayList<>();
                    kategoriList.add("Semua");
                    kategoriList.addAll(kategoriSet);

                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, kategoriList);
                    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerKategori.setAdapter(spinnerAdapter);

                    spinnerKategori.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            filterProducts(searchView.getQuery().toString(), kategoriList.get(position));
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });
                } else {
                    Toast.makeText(getContext(), "Gagal memuat data produk", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearchListener() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterProducts(query, spinnerKategori.getSelectedItem().toString());
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Pastikan spinner tidak null sebelum digunakan
                if (spinnerKategori != null && spinnerKategori.getSelectedItem() != null) {
                    filterProducts(newText, spinnerKategori.getSelectedItem().toString());
                }
                return true;
            }
        });
    }

    private void filterProducts(String keyword, String kategori) {
        filteredProducts.clear();
        for (Product p : allProducts) {
            String merk = p.getMerk() != null ? p.getMerk() : "";
            String kat = p.getKategori() != null ? p.getKategori() : "";

            boolean matchKeyword = merk.toLowerCase().contains(keyword.toLowerCase());
            boolean matchKategori = kategori.equals("Semua") || kat.equalsIgnoreCase(kategori);

            if (matchKeyword && matchKategori) {
                filteredProducts.add(p);
            }
        }
        adapter.notifyDataSetChanged();
    }
}