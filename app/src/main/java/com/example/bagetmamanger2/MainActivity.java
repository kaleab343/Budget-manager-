package com.example.bagetmamanger2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.appbar.MaterialToolbar;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved theme before view inflation
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        int savedMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedMode);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        if (topAppBar != null) {
            setSupportActionBar(topAppBar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Beget Manager");
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout), (v, insets) -> {
            WindowInsetsCompat systemInsets = insets;
            v.setPadding(
                    systemInsets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    systemInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    systemInsets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    systemInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            );
            return insets;
        });

        if (savedInstanceState == null) {
            loadFragment(BegetManagerFragment.newInstance());
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.replace(R.id.fragment_container_view, fragment);
        transaction.commit();
    }

    // ---------------- Options Menu ----------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_toggle_theme) {
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            int newMode;
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                newMode = AppCompatDelegate.MODE_NIGHT_NO;
                Toast.makeText(this, "Switched to Light mode", Toast.LENGTH_SHORT).show();
            } else {
                newMode = AppCompatDelegate.MODE_NIGHT_YES;
                Toast.makeText(this, "Switched to Dark mode", Toast.LENGTH_SHORT).show();
            }
            AppCompatDelegate.setDefaultNightMode(newMode);
            getSharedPreferences("prefs", MODE_PRIVATE).edit().putInt("theme_mode", newMode).apply();
            recreate();
            return true;
        } else if (id == R.id.action_about) {
            // Launch the prereation activity when "graph" is clicked
            Intent intent = new Intent(this, prereation.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
