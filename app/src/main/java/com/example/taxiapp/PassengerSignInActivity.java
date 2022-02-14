package com.example.taxiapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class PassengerSignInActivity extends AppCompatActivity {
    public static final String TAG = "Passenger";

    private TextInputLayout textInputEmail, textInputName, textInputPassword, textInputConfirmPassword;
    private Button signUpButton;
    private TextView toggleSignUpLoginTextView;
    private boolean isLoginModeActive;

    private FirebaseAuth mAuth;
    FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_sign_in);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance("https://taxi-app-d2353-default-rtdb.europe-west1.firebasedatabase.app/");

        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(PassengerSignInActivity.this, PassengerMapsActivity.class));
        }

        textInputEmail = findViewById(R.id.textInputEmail);
        textInputName = findViewById(R.id.textInputName);
        textInputPassword = findViewById(R.id.textInputPassword);
        textInputConfirmPassword = findViewById(R.id.textInputConfirmPassword);
        signUpButton = findViewById(R.id.signUpButton);
        toggleSignUpLoginTextView = findViewById(R.id.toggleSignUpLoginTextView);
    }

    private boolean validateEmail() {
        String emailInput = textInputEmail.getEditText().getText().toString().trim();

        if (emailInput.isEmpty()) {
            textInputEmail.setError("Please input your email");
            return false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            textInputEmail.setError("Please input correct email");
            return false;
        } else {
            textInputEmail.setError(null);
            return true;
        }
    }

    private boolean validateName() {
        String nameInput = textInputName.getEditText().getText().toString().trim();

        if (nameInput.isEmpty()) {
            textInputName.setError("Please input your name");
            return false;
        } else if (nameInput.length() > 15) {
            textInputName.setError("Name is too long");
            return false;
        } else {
            textInputName.setError(null);
            return true;
        }
    }

    private boolean validatePassword() {
        String passwordInput = textInputPassword.getEditText().getText().toString().trim();
        String confirmPasswordInput = textInputConfirmPassword.getEditText().getText().toString().trim();

        if (passwordInput.isEmpty()) {
            textInputPassword.setError("Please input your password");
            return false;
        } else if (passwordInput.length() < 6) {
            textInputPassword.setError("Password must have at least 6 chars");
            return false;
        } else if (!passwordInput.equals(confirmPasswordInput)) {
            textInputPassword.setError(null);
            textInputConfirmPassword.setError("Password is not match");
            return false;
        } else {
            textInputPassword.setError(null);
            textInputConfirmPassword.setError(null);
            return true;
        }
    }

    public void signUp(View view) {
        if (!isLoginModeActive) {
            if (!validateEmail() | !validateName() | !validatePassword()) {
                return;
            } else {
                if (!validateEmail() | !validatePassword()) {
                    return;
                }
            }
        }

        if (isLoginModeActive) {
            mAuth.signInWithEmailAndPassword(textInputEmail.getEditText().getText().toString().trim(),
                    textInputPassword.getEditText().getText().toString().trim())
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            startActivity(new Intent(PassengerSignInActivity.this, PassengerMapsActivity.class));
                            //updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(PassengerSignInActivity.this, "Authentication failed.",
                                    Toast.LENGTH_LONG).show();
                            //updateUI(null);
                        }
                    });
        } else {
            mAuth.createUserWithEmailAndPassword(textInputEmail.getEditText().getText().toString().trim(),
                    textInputPassword.getEditText().getText().toString().trim())
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            startActivity(new Intent(PassengerSignInActivity.this, PassengerMapsActivity.class));
                            //updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(PassengerSignInActivity.this, "Authentication failed.",
                                    Toast.LENGTH_LONG).show();
                            //updateUI(null);
                        }
                    });
        }
    }

    public void toggleSignUpLogin(View view) {
        if (!isLoginModeActive) {
            signUpButton.setText("login");
            toggleSignUpLoginTextView.setText("Tap to Sign up");
            textInputName.setVisibility(View.GONE);
            textInputConfirmPassword.setVisibility(View.GONE);
            isLoginModeActive = true;
        } else {
            signUpButton.setText("sign up");
            toggleSignUpLoginTextView.setText("Tap to Login");
            textInputName.setVisibility(View.VISIBLE);
            textInputConfirmPassword.setVisibility(View.VISIBLE);
            isLoginModeActive = false;
        }
    }
}