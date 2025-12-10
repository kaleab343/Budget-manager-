package com.example.bagetmamanger2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    
    private static final String TAG = "LoginActivity";
    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;
    private MaterialButton signInButton;
    
    // Your web client ID from Firebase Console
    private String getWebClientId() {
        // Try to get from string resources first, fallback to placeholder
        try {
            return getString(R.string.web_client_id);
        } catch (Exception e) {
            // Fallback - replace this with your actual Web Client ID
            return "YOUR_WEB_CLIENT_ID_HERE";
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Initialize Credential Manager
        credentialManager = CredentialManager.create(this);
        
        // Initialize UI
        initUI();
        
        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, go to main activity
            navigateToMainActivity();
        }
    }
    
    private void initUI() {
        signInButton = findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(v -> signInWithGoogle());
    }
    
    private void signInWithGoogle() {
        signInButton.setEnabled(false);
        signInButton.setText("Signing in...");
        
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setServerClientId(getWebClientId())
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(true)
                .build();
        
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();
        
        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleSignIn(result);
                    }
                    
                    @Override
                    public void onError(GetCredentialException e) {
                        runOnUiThread(() -> {
                            Log.e(TAG, "Sign in failed", e);
                            Toast.makeText(LoginActivity.this, "Sign in failed: " + e.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                            resetSignInButton();
                        });
                    }
                }
        );
    }
    
    private void handleSignIn(GetCredentialResponse result) {
        try {
            GoogleIdTokenCredential googleIdTokenCredential = 
                    GoogleIdTokenCredential.createFrom(result.getCredential().getData());
            
            String idToken = googleIdTokenCredential.getIdToken();
            
            runOnUiThread(() -> {
                // Authenticate with Firebase
                AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                mAuth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "signInWithCredential:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                Toast.makeText(this, "Welcome " + user.getDisplayName() + "!", 
                                        Toast.LENGTH_SHORT).show();
                                navigateToMainActivity();
                            } else {
                                Log.w(TAG, "signInWithCredential:failure", task.getException());
                                Toast.makeText(this, "Authentication Failed.", 
                                        Toast.LENGTH_SHORT).show();
                                resetSignInButton();
                            }
                        });
            });
            
        } catch (Exception e) {
            runOnUiThread(() -> {
                Log.e(TAG, "Invalid Google ID token", e);
                Toast.makeText(this, "Invalid Google ID token", Toast.LENGTH_SHORT).show();
                resetSignInButton();
            });
        }
    }
    
    private void resetSignInButton() {
        signInButton.setEnabled(true);
        signInButton.setText("Sign in with Google");
    }
    
    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}