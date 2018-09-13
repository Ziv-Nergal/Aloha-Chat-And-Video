package com.example.leeronziv.alohaworld_chatvideo;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ProfileActivity extends AppCompatActivity
{
    enum eFriendShipStates
    {
        not_friends,
        request_sent,
        request_received,
        friends
    }

    private ImageView mProfileImageView;

    private TextView mProfileUserNameTV;
    private TextView mProfileStatusTV;

    private DatabaseReference mRootDataBase;
    private DatabaseReference mUserDataBase;
    private DatabaseReference mFriendRequestDataBase;
    private DatabaseReference mFriendsDataBase;

    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private eFriendShipStates mCurrentFriendshipState;

    private String mCurrentUserId;
    private String mFriendId;

    private Button mFriendBtn;
    private Button mDeclineFriendRequestBtn;

    private ProgressBar mProfileProgressBar;

    LinearLayout mDetailsLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        setToolBar();

        if (mAuth.getCurrentUser() != null)
        {
            mCurrentUserId = mAuth.getCurrentUser().getUid();
        }

        mFriendId = getIntent().getStringExtra("user_id");

        if(mFriendId == null) // This means that the applications in running in background!
        {
            mFriendId = getIntent().getExtras().getString("sender_id");
        }

        mRootDataBase = FirebaseDatabase.getInstance().getReference();
        mRootDataBase.keepSynced(true);

        mUserDataBase = FirebaseDatabase.getInstance().getReference().child("Users").child(mFriendId);

        mFriendRequestDataBase = FirebaseDatabase.getInstance().getReference().child("friend_requests");
        mFriendRequestDataBase.keepSynced(true);

        mFriendsDataBase = FirebaseDatabase.getInstance().getReference().child("friends");
        mFriendsDataBase.keepSynced(true);

        mProfileImageView = (ImageView) findViewById(R.id.profile_user_image);
        mProfileUserNameTV = (TextView) findViewById(R.id.profile_user_name);
        mProfileStatusTV = (TextView) findViewById(R.id.profile_user_status);
        mFriendBtn = (Button) findViewById(R.id.profile_send_friend_request_btn);
        mDeclineFriendRequestBtn = (Button) findViewById(R.id.decline_friend_request_btn);
        mProfileProgressBar = (ProgressBar) findViewById(R.id.profile_progress_bar);
        mDetailsLayout = (LinearLayout) findViewById(R.id.profile_details_layout);

        mDetailsLayout.setVisibility(View.INVISIBLE);
        mProfileProgressBar.setVisibility(View.VISIBLE);

        mCurrentFriendshipState = eFriendShipStates.not_friends;

        mUserDataBase.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                String userNameStr = Objects.requireNonNull(dataSnapshot.child("name").getValue()).toString();
                String statusStr = Objects.requireNonNull(dataSnapshot.child("status").getValue()).toString();
                String imageStr = Objects.requireNonNull(dataSnapshot.child("image").getValue()).toString();

                mProfileUserNameTV.setText(userNameStr);
                mProfileStatusTV.setText(statusStr);

                Picasso.get().load(imageStr).placeholder(R.drawable.progress_animation).into(mProfileImageView);

                /* --------------- Friend List / Request Features --------------- */

                mFriendRequestDataBase.child(mCurrentUserId).addListenerForSingleValueEvent(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                    {
                        if(dataSnapshot.hasChild(mFriendId))
                        {
                            String requestTypeStr = dataSnapshot.child(mFriendId).child("request_type").getValue().toString();

                            switch (requestTypeStr)
                            {
                                case "request_received":
                                {
                                    mCurrentFriendshipState = eFriendShipStates.request_received;
                                    mFriendBtn.setText(R.string.accept_friend_request);

                                    mDeclineFriendRequestBtn.setVisibility(View.VISIBLE);
                                    mDeclineFriendRequestBtn.setEnabled(true);

                                    break;
                                }
                                case "request_sent":
                                {
                                    mCurrentFriendshipState = eFriendShipStates.request_sent;
                                    mFriendBtn.setText(R.string.cancel_friend_request);

                                    mDeclineFriendRequestBtn.setVisibility(View.INVISIBLE);
                                    mDeclineFriendRequestBtn.setEnabled(false);

                                    break;
                                }
                            }

                            mDetailsLayout.setVisibility(View.VISIBLE);
                            mProfileProgressBar.setVisibility(View.INVISIBLE);
                        }
                        else // User is already my friend
                        {
                            mFriendsDataBase.child(mCurrentUserId).addListenerForSingleValueEvent(new ValueEventListener()
                            {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                                {
                                    if(dataSnapshot.hasChild(mFriendId))
                                    {
                                        mCurrentFriendshipState = eFriendShipStates.friends;
                                        mFriendBtn.setText(R.string.unfriend);

                                        mDeclineFriendRequestBtn.setVisibility(View.INVISIBLE);
                                        mDeclineFriendRequestBtn.setEnabled(false);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError){}
                            });

                            mDetailsLayout.setVisibility(View.VISIBLE);
                            mProfileProgressBar.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    public void friendRequestBtnClickListener(View view)
    {
        mFriendBtn.setEnabled(false);

        switch (mCurrentFriendshipState)
        {
            case not_friends:
                sendFriendRequest();
                break;

            case request_sent:
                cancelOrDeclineRequest();
                break;

            case request_received:
                acceptFriendRequest();
                break;

            case friends:
                cancelFriendship();
                break;
        }

        mFriendBtn.setEnabled(true);
    }

    private void sendFriendRequest()
    {
        DatabaseReference newNotificationRef = mRootDataBase.child("notifications").child(mFriendId).push();
        String newNotificationId = newNotificationRef.getKey();

        HashMap<String, String> notificationData = new HashMap<>();
        notificationData.put("from", mCurrentUserId);
        notificationData.put("type", "friend_request");

        Map requestMap = new HashMap<>();
        requestMap.put("friend_requests/" + mCurrentUserId + "/" + mFriendId + "/request_type", "request_sent");
        requestMap.put("friend_requests/" + mFriendId + "/" + mCurrentUserId + "/request_type", "request_received");
        requestMap.put("notifications/" + mFriendId + "/" + newNotificationId, notificationData);

        mRootDataBase.updateChildren(requestMap, new DatabaseReference.CompletionListener()
        {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference)
            {
                if(databaseError == null)
                {
                    mCurrentFriendshipState = eFriendShipStates.request_sent;
                    mFriendBtn.setText(R.string.cancel_friend_request);

                    mDeclineFriendRequestBtn.setVisibility(View.INVISIBLE);
                    mDeclineFriendRequestBtn.setEnabled(false);
                }
                else
                {
                    Toast.makeText(ProfileActivity.this, "There was an error", Toast.LENGTH_SHORT).show();
                    // Error handling
                }
            }
        });
    }

    private void setToolBar()
    {
        Toolbar toolBar;
        toolBar = (Toolbar) findViewById(R.id.profile_toolbar);
        TextView toolbarTitleTV = (TextView) toolBar.findViewById(R.id.title_text_view);
        toolbarTitleTV.setText(R.string.profile);
        toolbarTitleTV.setVisibility(View.VISIBLE);
        setSupportActionBar(toolBar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void cancelOrDeclineRequest()
    {
        Map cancelOrDeclineRequestMap = new HashMap<>();
        cancelOrDeclineRequestMap.put("friend_requests/" + mCurrentUserId + "/" + mFriendId, null);
        cancelOrDeclineRequestMap.put("friend_requests/" + mFriendId + "/" + mCurrentUserId, null);

        mRootDataBase.updateChildren(cancelOrDeclineRequestMap, new DatabaseReference.CompletionListener()
        {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference)
            {
                if(databaseError == null)
                {
                    mCurrentFriendshipState = eFriendShipStates.not_friends;
                    mFriendBtn.setText(R.string.send_friend_request);

                    mDeclineFriendRequestBtn.setVisibility(View.INVISIBLE);
                    mDeclineFriendRequestBtn.setEnabled(false);
                }
                else
                {
                    Toast.makeText(ProfileActivity.this, "There was an error", Toast.LENGTH_SHORT).show();
                    // Error handling
                }
            }
        });
    }

    private void acceptFriendRequest()
    {
        final String currentDate = DateFormat.getDateInstance().format(new Date());

        Map newFriendshipMap = new HashMap<>();
        newFriendshipMap.put("friends/" + mCurrentUserId + "/" + mFriendId + "/date", currentDate);
        newFriendshipMap.put("friends/" + mFriendId + "/" + mCurrentUserId + "/date", currentDate);
        newFriendshipMap.put("friend_requests/" + mCurrentUserId + "/" + mFriendId, null);
        newFriendshipMap.put("friend_requests/" + mFriendId + "/" + mCurrentUserId, null);

        mRootDataBase.updateChildren(newFriendshipMap, new DatabaseReference.CompletionListener()
        {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference)
            {
                if (databaseError == null)
                {
                    mFriendBtn.setEnabled(true);
                    mCurrentFriendshipState = eFriendShipStates.friends;
                    mFriendBtn.setText(R.string.unfriend);

                    mDeclineFriendRequestBtn.setVisibility(View.INVISIBLE);
                    mDeclineFriendRequestBtn.setEnabled(false);
                }
                else
                {
                    Toast.makeText(ProfileActivity.this, "There was an error", Toast.LENGTH_SHORT).show();
                    // Error handling
                }
            }
        });
    }

    private void cancelFriendship()
    {
        Map cancelFriendshipMap = new HashMap<>();
        cancelFriendshipMap.put("friends/" + mCurrentUserId + "/" + mFriendId, null);
        cancelFriendshipMap.put("friends/" + mFriendId + "/" + mCurrentUserId, null);

        mRootDataBase.updateChildren(cancelFriendshipMap, new DatabaseReference.CompletionListener()
        {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference)
            {
                if(databaseError == null)
                {
                    mCurrentFriendshipState = eFriendShipStates.not_friends;
                    mFriendBtn.setText(R.string.send_friend_request);

                    mDeclineFriendRequestBtn.setVisibility(View.INVISIBLE);
                    mDeclineFriendRequestBtn.setEnabled(false);
                }
                else
                {
                    Toast.makeText(ProfileActivity.this, "There was an error", Toast.LENGTH_SHORT).show();
                    // Error handling
                }
            }
        });
    }

    public void declineFriendRequestBtnClickListener(View view)
    {
        cancelOrDeclineRequest();
    }
}
