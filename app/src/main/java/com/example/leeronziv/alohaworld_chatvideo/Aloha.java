package com.example.leeronziv.alohaworld_chatvideo;

import android.app.Application;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

public class Aloha extends Application
{
    private FirebaseAuth mAuth;
    private DatabaseReference mUserDataBase;

    @Override
    public void onCreate()
    {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        Picasso.Builder builder = new Picasso.Builder(this);
        builder.downloader(new OkHttp3Downloader(this, Integer.MAX_VALUE));
        Picasso built = builder.build();
        built.setIndicatorsEnabled(true);
        Picasso.setSingletonInstance(built);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getCurrentUser() != null)
        {
            mUserDataBase = FirebaseDatabase.getInstance().getReference().child("Users").child(mAuth.getCurrentUser().getUid());

            mUserDataBase.addValueEventListener(new ValueEventListener()
            {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                {
                    mUserDataBase.child("online").onDisconnect().setValue(ServerValue.TIMESTAMP);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {}
            });
        }
    }
}
