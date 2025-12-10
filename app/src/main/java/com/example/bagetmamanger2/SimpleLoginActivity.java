package com.example.bagetmamanger2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import androidx.appcompat.app.AlertDialog;

public class SimpleLoginActivity extends AppCompatActivity {

    private static final String TAG = "SimpleLoginActivity";
    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private MaterialButton signInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize UI
        signInButton = findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(v -> signIn());

        MaterialButton skipLoginButton = findViewById(R.id.skip_login_button);
        skipLoginButton.setOnClickListener(v -> navigateToMainActivity());

        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToMainActivity();
        }

        // Debug Google Sign-In configuration
        debugGoogleSignInConfiguration();
    }

    private void signIn() {
        signInButton.setEnabled(false);
        signInButton.setText("Signing in...");

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed (Ask Gemini)", e);
                Log.e(TAG, "Error code: " + e.getStatusCode());
                Log.e(TAG, "Error message: " + e.getMessage());

                String errorMessage = "Google sign in failed";
                switch (e.getStatusCode()) {
                    case 10: // DEVELOPER_ERROR
                        errorMessage = "Configuration error. Check Firebase setup in console.\nFor now, you can skip login to continue.";
                        // Temporary development bypass
                        showSkipLoginOption();
                        break;
                    case 12500: // SIGN_IN_CANCELLED
                        errorMessage = "Sign in was cancelled by user.";
                        break;
                    case 7: // NETWORK_ERROR
                        errorMessage = "Network error. Check internet connection.";
                        break;
                    default:
                        errorMessage = "Sign in failed with code: " + e.getStatusCode();
                        break;
                }

                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                resetSignInButton();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Welcome " + user.getDisplayName() + "!",
                                Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(this, "Authentication Failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        resetSignInButton();
                    }
                });
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

    private void debugGoogleSignInConfiguration() {
        // Check if Google Play Services is available
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(this);

        Log.d(TAG, "Google Play Services status: " + status);

        if (status != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services not available");
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(this, status, 2404).show();
            }
            return;
        }

        // Log configuration details
        Log.d(TAG, "Web Client ID: " + getString(R.string.web_client_id));
        Log.d(TAG, "Package name: " + getPackageName());

        // Check if user is already signed in
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        Log.d(TAG, "Last signed in account: " + (account != null ? account.getEmail() : "null"));
    }

    private void showSkipLoginOption() {
        new AlertDialog.Builder(this)
                .setTitle("Login Issue")
                .setMessage("There's a configuration issue with Google Sign-In. Would you like to skip login for now and continue to the app?")
                .setPositiveButton("Skip Login", (dialog, which) -> {
                    // Create a temporary anonymous user for testing
                    signInAnonymously();
                })
                .setNegativeButton("Try Again", (dialog, which) -> {
                    // Let user try again
                    dialog.dismiss();
                })
                .show();
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInAnonymously:success");
                        Toast.makeText(this, "Signed in anonymously for testing", Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    } else {
                        Log.w(TAG, "signInAnonymously:failure", task.getException());
                        Toast.makeText(this, "Anonymous sign-in failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}