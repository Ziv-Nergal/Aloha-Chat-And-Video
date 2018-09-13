package com.example.leeronziv.alohaworld_chatvideo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class UsersActivity extends AppCompatActivity
{
    private RecyclerView mUsersList;

    private DatabaseReference mAllUsersDataBase;

    @Override
    public void onStart()
    {
        super.onStart();

        FirebaseRecyclerOptions<Friend> options =
                new FirebaseRecyclerOptions.Builder<Friend>()
                        .setQuery(mAllUsersDataBase, Friend.class)
                        .build();

        FirebaseRecyclerAdapter<Friend, FriendsFragment.FriendViewHolder> friendsRecyclerViewAdapter = new FirebaseRecyclerAdapter<Friend, FriendsFragment.FriendViewHolder>(options)
        {
            @Override
            protected void onBindViewHolder(@NonNull final FriendsFragment.FriendViewHolder friendViewHolder, int position, @NonNull final Friend friend)
            {
                final String userId = getRef(position).getKey();

                assert userId != null;
                mAllUsersDataBase.child(userId).addValueEventListener(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                    {
                        final String userName = Objects.requireNonNull(dataSnapshot.child("name").getValue()).toString();
                        String status = Objects.requireNonNull(dataSnapshot.child("status").getValue()).toString();
                        String userThumbImage = Objects.requireNonNull(dataSnapshot.child("thumb_image").getValue()).toString();

                        if (dataSnapshot.hasChild("online"))
                        {
                            String userConnectedStatus = dataSnapshot.child("online").getValue().toString();
                            friendViewHolder.setUserConnectedStatus(userConnectedStatus);
                        }

                        friendViewHolder.setName(userName);
                        friendViewHolder.setStatus(status);
                        friendViewHolder.setUserThumbImage(userThumbImage);

                        ImageView statusIV = (ImageView) friendViewHolder.mView.findViewById(R.id.friends_connected_icon);
                        statusIV.setVisibility(View.INVISIBLE);

                        friendViewHolder.mView.setOnClickListener(new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                Intent userProfileIntent = new Intent(UsersActivity.this, ProfileActivity.class);
                                userProfileIntent.putExtra("user_id", userId);
                                startActivity(userProfileIntent);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });
            }

            @NonNull
            @Override
            public FriendsFragment.FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                // Create a new instance of the ViewHolder, in this case we are using a custom
                // layout called R.layout.message for each item
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.friend_cell, parent, false);

                return new FriendsFragment.FriendViewHolder(view);
            }
        };

        friendsRecyclerViewAdapter.startListening();
        mUsersList.setAdapter(friendsRecyclerViewAdapter);
        mUsersList.addItemDecoration(new SimpleDividerItemDecoration(getApplicationContext()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        mAllUsersDataBase = FirebaseDatabase.getInstance().getReference().child("Users");

        setToolBar();

        mUsersList = (RecyclerView) findViewById(R.id.users_list);
        mUsersList.setHasFixedSize(true);
        mUsersList.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setToolBar()
    {
        Toolbar toolBar;
        toolBar = (Toolbar) findViewById(R.id.users_toolbar);
        TextView toolbarTitleTV = (TextView) toolBar.findViewById(R.id.title_text_view);
        toolbarTitleTV.setText(R.string.all_users);
        toolbarTitleTV.setVisibility(View.VISIBLE);
        setSupportActionBar(toolBar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
