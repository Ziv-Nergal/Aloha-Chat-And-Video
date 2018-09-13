package com.example.leeronziv.alohaworld_chatvideo;

import android.app.Activity;
import android.content.Intent;
import android.provider.ContactsContract;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class LoginActivity extends AppCompatActivity
{
    private TextInputLayout mLoginEmail;
    private TextInputLayout mLoginPassword;

    private FirebaseAuth mAuth;

    private ProgressBar mSignInProgressBar;

    private DatabaseReference mUserDataBase;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mUserDataBase = FirebaseDatabase.getInstance().getReference().child("Users");

        mLoginEmail = (TextInputLayout) findViewById(R.id.sign_in_email);
        mLoginPassword = (TextInputLayout) findViewById(R.id.sign_in_password);
    }

    public void signUpBtnClickListener(View view)
    {
        startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
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
            mSignInProgressBar = (ProgressBar) findViewById(R.id.sign_in_Progress_bar);
            mSignInProgressBar.setVisibility(View.VISIBLE);
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
                            mSignInProgressBar.setVisibility(View.GONE);

                            String currentUserID = mAuth.getCurrentUser().getUid();
                            String deviceToken = FirebaseInstanceId.getInstance().getToken();

                            mUserDataBase.child(currentUserID).child("device_token").setValue(deviceToken).addOnCompleteListener(new OnCompleteListener<Void>()
                            {
                                @Override
                                public void onComplete(@NonNull Task<Void> task)
                                {
                                    if(task.isSuccessful())
                                    {
                                        Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(mainIntent);
                                        finish();
                                    }
                                    else
                                    {
                                        Toast.makeText(LoginActivity.this, "Error setting device token", Toast.LENGTH_SHORT).show();
                                        // Error handling
                                    }
                                }
                            });
                        }
                        else
                        {
                            // If sign in fails, display a message to the user.
                            mSignInProgressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
