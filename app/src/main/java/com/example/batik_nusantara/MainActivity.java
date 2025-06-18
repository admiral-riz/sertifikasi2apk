package com.example.batik_nusantara;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPreferences = getSharedPreferences("user_session", Context.MODE_PRIVATE);
        boolean isGuest = sharedPreferences.getBoolean("is_guest", false);

        if (isGuest) {
            Toast.makeText(this, "Anda sedang masuk sebagai Tamu", Toast.LENGTH_SHORT).show();
            // Di sini Anda bisa menonaktifkan fitur tertentu jika guest
        }

        BottomNavigationView navView = findViewById(R.id.nav_view);

        // Mengambil NavController dari nav_host_fragment
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        // Menghubungkan BottomNavigationView dengan NavController supaya bisa pindah fragment otomatis
        NavigationUI.setupWithNavController(navView, navController);
    }
}
