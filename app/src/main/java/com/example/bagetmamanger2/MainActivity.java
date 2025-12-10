package com.example.bagetmamanger2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
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

        // Check if user is authenticated
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        if (savedInstanceState == null) {
            loadFragment(BegetManagerFragment.newInstance());
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
        
        // Setup user avatar if user is logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            setupUserAvatar(menu, currentUser);
        }
        
        return true;
    }
    
    private void setupUserAvatar(Menu menu, FirebaseUser user) {
        MenuItem userMenuItem = menu.findItem(R.id.action_user_profile);
        if (userMenuItem != null) {
            TextView userAvatarView = (TextView) userMenuItem.getActionView();
            if (userAvatarView != null) {
                // Get user initials
                String userInitials = getUserInitials(user.getDisplayName());
                
                // Configure the avatar TextView
                userAvatarView.setText(userInitials);
                userAvatarView.setTextSize(12);
                userAvatarView.setTextColor(getResources().getColor(android.R.color.white, getTheme()));
                userAvatarView.setGravity(android.view.Gravity.CENTER);
                userAvatarView.setWidth(96); // 32dp converted to pixels roughly
                userAvatarView.setHeight(96);
                
                // Create circular background
                GradientDrawable background = new GradientDrawable();
                background.setShape(GradientDrawable.OVAL);
                background.setColor(getResources().getColor(com.google.android.material.R.color.design_default_color_primary, getTheme()));
                userAvatarView.setBackground(background);
                
                // Add click listener for user profile actions
                userAvatarView.setOnClickListener(v -> {
                    Toast.makeText(this, "User: " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                });
            }
        }
    }
    
    private String getUserInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "U"; // Default for "User"
        }
        
        String[] nameParts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        
        for (int i = 0; i < Math.min(2, nameParts.length); i++) {
            if (!nameParts[i].isEmpty()) {
                initials.append(nameParts[i].charAt(0));
            }
        }
        
        return initials.toString().toUpperCase();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_user_profile) {
            // Handle user profile click
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String userName = currentUser.getDisplayName();
                String userEmail = currentUser.getEmail();
                Toast.makeText(this, "Logged in as: " + (userName != null ? userName : userEmail), Toast.LENGTH_LONG).show();
            }
            return true;
        } else if (id == R.id.action_toggle_theme) {
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
        } else if (id == R.id.action_logout) {
            // Sign out the user
            mAuth.signOut();
            Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();
            redirectToLogin();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
