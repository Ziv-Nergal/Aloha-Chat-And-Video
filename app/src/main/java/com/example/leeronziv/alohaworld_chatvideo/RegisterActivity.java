package com.example.leeronziv.alohaworld_chatvideo;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;

import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity
{
    private FirebaseAuth mAuth;
    private DatabaseReference mUserDataBase;

    private TextInputLayout mUsername;
    private TextInputLayout mEmail;
    private TextInputLayout mPassword;

    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        mUsername = (TextInputLayout)findViewById(R.id.reg_user_name);
        mEmail = (TextInputLayout)findViewById(R.id.reg_email);
        mPassword = (TextInputLayout)findViewById(R.id.reg_password);

        mProgressBar = (ProgressBar)findViewById(R.id.reg_progress_bar);
    }

    public void signUpBtnClickListener(View view)
    {
        String userNameStr = Objects.requireNonNull(mUsername.getEditText()).getText().toString();
        String emailStr = Objects.requireNonNull(mEmail.getEditText()).getText().toString();
        String passwordStr = Objects.requireNonNull(mPassword.getEditText()).getText().toString();

        if(userNameStr.isEmpty() || emailStr.isEmpty() || passwordStr.isEmpty())
        {
            Toast.makeText(this, "Please fill all details!", Toast.LENGTH_SHORT).show();
        }
        else
        {
            mProgressBar.setVisibility(View.VISIBLE);
            registerNewUser(userNameStr, emailStr, passwordStr);
        }
    }

    private void registerNewUser(final String userNameStr, String emailStr, String passwordStr)
    {
        mAuth.createUserWithEmailAndPassword(emailStr, passwordStr)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful())
                        {
                            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

                            if (currentUser != null)
                            {
                                String uid = currentUser.getUid();
                                mUserDataBase = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);

                                String deviceToken = FirebaseInstanceId.getInstance().getId();

                                HashMap<String, String> userMap = new HashMap<>();
                                userMap.put("name", userNameStr);
                                userMap.put("status", "Hey there! I'm using Aloha");
                                userMap.put("image", "default");
                                userMap.put("thumb_image", "default");
                                userMap.put("device_token", deviceToken);

                                mUserDataBase.setValue(userMap).addOnCompleteListener(new OnCompleteListener<Void>()
                                {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task)
                                    {
                                        if(task.isSuccessful())
                                        {
                                            // Sign in success, update UI with the signed-in user's information
                                            mProgressBar.setVisibility(View.GONE);

                                            mUserDataBase.child("online").setValue(true);

                                            Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
                                            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(mainIntent);
                                            finish();
                                        }
                                    }
                                });
                            }
                        }
                        else
                        {
                            // If sign in fails, display a message to the user.
                            mProgressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(RegisterActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void backBtnClickListener(View view)
    {
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
