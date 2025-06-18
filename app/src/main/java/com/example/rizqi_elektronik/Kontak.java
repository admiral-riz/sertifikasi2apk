package com.example.rizqi_elektronik;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

public class Kontak extends AppCompatActivity {

    private Button btnBacktoProfile;
    private ImageView ivMap, ivWhatsapp, ivEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kontak);

        btnBacktoProfile = findViewById(R.id.buttonKembali);
        ivMap = findViewById(R.id.ivMap);
        ivWhatsapp = findViewById(R.id.ivWhatsapp);
        ivEmail = findViewById(R.id.ivEmail);

        // Tombol Kembali
        btnBacktoProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Kontak.this, MainActivity.class);
            intent.putExtra("fragmentToLoad", "profile"); // Untuk membuka ProfileFragment lewat MainActivity
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish(); // Menutup Activity saat ini
        });

        // Mengarahkan ke Google Maps
        ivMap.setOnClickListener(v -> {
            Uri gmmIntentUri = Uri.parse("geo:-6.200000,106.816666?q=Rizqi Elektronik");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                // Jika Maps tidak ada, buka browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://g.co/kgs/VzCFwx8"));
                startActivity(browserIntent);
            }
        });


        // Mengarahkan ke WhatsApp
        ivWhatsapp.setOnClickListener(v -> {
            String phoneNumber = "+6287731090526";
            Intent waIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + phoneNumber));
            startActivity(waIntent);
        });

        // Mengarahkan ke Email
        ivEmail.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:roswanto@gmail.com"));
            startActivity(emailIntent);
        });
    }
}
