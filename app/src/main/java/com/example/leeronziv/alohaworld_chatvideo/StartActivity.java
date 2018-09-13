package com.example.leeronziv.alohaworld_chatvideo;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class StartActivity extends AppCompatActivity
{
    private TextInputLayout mLoginEmail;
    private TextInputLayout mLoginPassword;

    private FirebaseAuth mAuth;

    private ProgressBar signInProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        mAuth = FirebaseAuth.getInstance();

        mLoginEmail = (TextInputLayout) findViewById(R.id.sign_in_email);
        mLoginPassword = (TextInputLayout) findViewById(R.id.sign_in_password);
    }

    public void signUpBtnClickListener(View view)
    {
        startActivity(new Intent(StartActivity.this, RegisterActivity.class));
        finish();
    }

    public void signInBtnClickListener(View view)
    {
        String emailStr = mLoginEmail.getEditText().getText().toString();
        String passwordStr = mLoginPassword.getEditText().getText().toString();

        hideKeyBoard();

        if(emailStr.isEmpty() || passwordStr.isEmpty())
        {
            Toast.makeText(this, "Please fill all details!", Toast.LENGTH_SHORT).show();
        }
        else
        {
            signInProgressBar = (ProgressBar) findViewById(R.id.sign_in_Progress_bar);
            signInProgressBar.setVisibility(View.VISIBLE);
            logIn(emailStr, passwordStr);
        }

    }

    private void hideKeyBoard()
    {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null)
        {
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void logIn(String emailStr, String passwordStr)
    {
        mAuth.signInWithEmailAndPassword(emailStr, passwordStr)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful())
                        {
                            // Sign in success, update UI with the signed-in user's information
                            signInProgressBar.setVisibility(View.GONE);
                            startActivity(new Intent(StartActivity.this, MainActivity.class));
                            finish();
                        }
                        else
                        {
                            // If sign in fails, display a message to the user.
                            signInProgressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(StartActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
