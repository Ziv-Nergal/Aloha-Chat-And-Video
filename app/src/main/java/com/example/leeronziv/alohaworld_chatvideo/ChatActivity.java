package com.example.leeronziv.alohaworld_chatvideo;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity
{
    private String mChatUserId;
    private String mChatUserName;
    private String mCurrentUserId;

    private DatabaseReference mRootDataBase;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private StorageReference mImageStorage;

    private String mImageDownloadUrl;

    private TextView mToolbarUserNameTv;
    private TextView mToolbarLastSeenTv;
    private ImageButton mToolBarVideoSessionBtn;
    private CircleImageView mToolbarProfileImage;

    private EditText mChatMessageET;

    private RecyclerView mMessagesListRV;
    private SwipeRefreshLayout mChatSwipeLayout;

    private final List<Message> mMessageList = new ArrayList<>();
    private MessageAdapter mAdapter;

    private static final int GALLERY_PICK = 1;
    private static final int TOTAL_MESSAGES_TO_LOAD = 15;
    private int mRefreshCount = 1;

    private int itemPosition = 0;

    private String mLastKey = "";
    private String mPrevKey = "";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mRootDataBase = FirebaseDatabase.getInstance().getReference();
        mImageStorage = FirebaseStorage.getInstance().getReference();

        mChatUserId = getIntent().getStringExtra("user_id");

        if(mChatUserId == null)
        {
            mChatUserId = getIntent().getExtras().getString("sender_id");
        }

        mChatUserName = getIntent().getStringExtra("user_name");

        if(mAuth.getCurrentUser() != null) { mCurrentUserId = mAuth.getCurrentUser().getUid(); }

        setToolBar();
        loadChatItems();
        loadMessages();

        mRootDataBase.child("chat").child(mCurrentUserId).addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if(!dataSnapshot.hasChild(mChatUserId))
                {
                    Map chatAddMap = new HashMap<>();
                    chatAddMap.put("seen", false);
                    chatAddMap.put("time_stamp", ServerValue.TIMESTAMP);

                    Map chatUserMap = new HashMap();
                    chatUserMap.put("chat/" + mCurrentUserId + "/" + mChatUserId, chatAddMap);
                    chatUserMap.put("chat/" + mChatUserId + "/" + mCurrentUserId, chatAddMap);

                    mRootDataBase.updateChildren(chatUserMap, new DatabaseReference.CompletionListener()
                    {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference)
                        {
                            if(databaseError != null)
                            {
                                Log.d("CHAT_LOG", databaseError.getMessage());
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

        mToolBarVideoSessionBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                DatabaseReference userMessagePush = mRootDataBase.child("video_sessions").child(mCurrentUserId).child(mChatUserId).push();
                final String newVideoSessionPushId = userMessagePush.getKey();

                DatabaseReference newNotificationRef = mRootDataBase.child("notifications").child(mChatUserId).push();
                final String newNotificationPushId = newNotificationRef.getKey();

                HashMap<String, String> notificationData = new HashMap<>();
                notificationData.put("from", mCurrentUserId);
                notificationData.put("type", "video_call");

                String currentUserRef = "video_sessions/" + mCurrentUserId + "/" + mChatUserId;
                String chatUserRef = "video_sessions/" + mChatUserId + "/" + mCurrentUserId;

                Map videoCallRequestMap = new HashMap();
                videoCallRequestMap.put(currentUserRef + "/" + newVideoSessionPushId, "calling");
                videoCallRequestMap.put(chatUserRef + "/" + newVideoSessionPushId, "getting_call");
                videoCallRequestMap.put("notifications/" + mChatUserId + "/" + newNotificationPushId, notificationData);

                mRootDataBase.updateChildren(videoCallRequestMap, new DatabaseReference.CompletionListener()
                {
                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference)
                    {
                        if(databaseError == null)
                        {
                            Intent videoSessionIntent = new Intent(ChatActivity.this, VideoSessionActivity.class);
                            videoSessionIntent.putExtra("session_id", newVideoSessionPushId);
                            videoSessionIntent.putExtra("current_user_id", mCurrentUserId);
                            videoSessionIntent.putExtra("friend_user_id", mChatUserId);
                            videoSessionIntent.putExtra("friend_name", mChatUserName);
                            videoSessionIntent.putExtra("call_status", "calling");

                            startActivity(videoSessionIntent);
                        }
                        else
                        {
                            Log.d("CHAT_LOG", databaseError.getMessage());
                        }
                    }
                });
            }
        });
    }

    private void setToolBar()
    {
         Toolbar chatToolBar = (Toolbar) findViewById(R.id.chat_toolbar);

        View toolBarItemsLayout = (View) chatToolBar.findViewById(R.id.chat_toolbar_items);
        toolBarItemsLayout.setVisibility(View.VISIBLE);

        mToolbarUserNameTv = (TextView) chatToolBar.findViewById(R.id.custom_bar_user_name_text_view);
        mToolbarLastSeenTv = (TextView) chatToolBar.findViewById(R.id.custom_bar_last_seen_text_view);
        mToolbarProfileImage = (CircleImageView) chatToolBar.findViewById(R.id.custom_bar_image);
        mToolBarVideoSessionBtn = (ImageButton) chatToolBar.findViewById(R.id.video_session_btn);

        mToolbarUserNameTv.setVisibility(View.VISIBLE);
        mToolbarLastSeenTv.setVisibility(View.VISIBLE);
        mToolbarProfileImage.setVisibility(View.VISIBLE);

        setSupportActionBar(chatToolBar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mRootDataBase.child("Users").child(mChatUserId).child("thumb_image").addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                String thumb_image = dataSnapshot.getValue().toString();
                Picasso.get().load(thumb_image).placeholder(R.drawable.blank_contact).into(mToolbarProfileImage);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

        mToolbarProfileImage.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent profileIntent = new Intent(ChatActivity.this, ProfileActivity.class);
                profileIntent.putExtra("user_id", mChatUserId);
                startActivity(profileIntent);
            }
        });
    }

    private void loadChatItems()
    {
        mChatMessageET = (EditText) findViewById(R.id.chat_message_edit_text);
        mMessagesListRV = (RecyclerView) findViewById(R.id.chat_messages_list);
        mChatSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.chat_swipe_layout);

        LinearLayoutManager linearLayout = new LinearLayoutManager(this);

        mAdapter = new MessageAdapter(mMessageList);

        mMessagesListRV.setHasFixedSize(true);
        mMessagesListRV.setLayoutManager(linearLayout);
        mMessagesListRV.setAdapter(mAdapter);

        mToolbarUserNameTv.setText(mChatUserName);

        mChatSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                mRefreshCount++;
                itemPosition = 0;
                loadPreviousMessages();
            }
        });

        mRootDataBase.child("Users").child(mChatUserId).addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                String online = dataSnapshot.child("online").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();

                if(online.equals("true"))
                {
                    mToolbarLastSeenTv.setText(R.string.online);
                }
                else
                {
                    long lastSeenTimeStamp = Long.parseLong(online);
                    String lastSeenStr = TimeStampConverter.getTimeAgo(lastSeenTimeStamp, getApplicationContext());

                    mToolbarLastSeenTv.setText(lastSeenStr);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void loadMessages()
    {
        mMessagesListRV.scrollToPosition(mAdapter.getItemCount() - 1);

        DatabaseReference messagesDataBase = mRootDataBase.child("messages").child(mCurrentUserId).child(mChatUserId);

        Query messageQuery = messagesDataBase.limitToLast(mRefreshCount * TOTAL_MESSAGES_TO_LOAD);

        messageQuery.addChildEventListener(new ChildEventListener()
        {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s)
            {
                Message message = dataSnapshot.getValue(Message.class);

                itemPosition++;

                if(itemPosition == 1)
                {
                    String messageKey = dataSnapshot.getKey();
                    mLastKey = messageKey;
                    mPrevKey = messageKey;
                }

                mMessageList.add(message);
                mAdapter.notifyItemInserted(mMessageList.size());

                mMessagesListRV.scrollToPosition(mAdapter.getItemCount() - 1);

                mChatSwipeLayout.setRefreshing(false);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    public void loadPreviousMessages()
    {
        DatabaseReference messagesDataBase = mRootDataBase.child("messages").child(mCurrentUserId).child(mChatUserId);

        Query messageQuery = messagesDataBase.orderByKey().endAt(mLastKey).limitToLast(10);

        messageQuery.addChildEventListener(new ChildEventListener()
        {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s)
            {
                Message message = dataSnapshot.getValue(Message.class);
                String messageKey = dataSnapshot.getKey();

                if(!mPrevKey.equals(messageKey))
                {
                    mMessageList.add(itemPosition++, message);
                }
                else
                {
                    mPrevKey = mLastKey;
                }

                if(itemPosition == 1)
                {
                    mLastKey = messageKey;
                }

                mAdapter.notifyDataSetChanged();
                mChatSwipeLayout.setRefreshing(false);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    public void messageEditTextClickListener(View view)
    {
        mMessagesListRV.scrollToPosition(mAdapter.getItemCount() - 1);
    }

    public void sendMessageBtnClickListener(View view)
    {
        String message = mChatMessageET.getText().toString();

        if(!message.isEmpty())
        {
            DatabaseReference userMessagePush = mRootDataBase.child("messages").child(mCurrentUserId).child(mChatUserId).push();
            String pushId = userMessagePush.getKey();

            DatabaseReference newNotificationRef = mRootDataBase.child("notifications").child(mChatUserId).push();
            String newNotificationId = newNotificationRef.getKey();

            String currentUserRef = "messages/" + mCurrentUserId + "/" + mChatUserId;
            String chatUserRef = "messages/" + mChatUserId + "/" + mCurrentUserId;

            HashMap<String, String> notificationData = new HashMap<>();
            notificationData.put("from", mCurrentUserId);
            notificationData.put("type", "message");
            notificationData.put("content", message);

            Map messageMap = new HashMap();
            messageMap.put("message", message);
            messageMap.put("seen", false);
            messageMap.put("type", "text");
            messageMap.put("time", ServerValue.TIMESTAMP);
            messageMap.put("from", mCurrentUserId);

            Map messageUserMap = new HashMap();
            messageUserMap.put(currentUserRef + "/" + pushId, messageMap);
            messageUserMap.put(chatUserRef + "/" + pushId, messageMap);
            messageUserMap.put("notifications/" + mChatUserId + "/" + newNotificationId, notificationData);

            mRootDataBase.updateChildren(messageUserMap, new DatabaseReference.CompletionListener()
            {
                @Override
                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference)
                {
                    if(databaseError != null)
                    {
                        Log.d("CHAT_LOG", databaseError.getMessage());
                    }
                }
            });
        }

        mChatMessageET.setText("");
    }

    public void sendPhotoBtnClickListener(View view)
    {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.OFF)
                .setCropShape(CropImageView.CropShape.RECTANGLE)
                .start(ChatActivity.this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK)
            {
                Uri imageUri = result.getUri();

                final String currentUserRef = "messages/" + mCurrentUserId + "/" + mChatUserId;
                final String chatUserRef = "messages/" + mChatUserId + "/" + mCurrentUserId;

                DatabaseReference userMessagePush = mRootDataBase.child("messages").child(mCurrentUserId).child(mChatUserId).push();

                final String pushId = userMessagePush.getKey();

                final StorageReference filePath = mImageStorage.child("message_images").child(pushId + ".jpg");

                filePath.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>()
                {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task)
                    {
                        if (task.isSuccessful())
                        {
                            filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>()
                            {
                                @Override
                                public void onSuccess(Uri uri)
                                {
                                    mImageDownloadUrl = uri.toString();

                                    Map messageMap = new HashMap();
                                    messageMap.put("message", mImageDownloadUrl);
                                    messageMap.put("seen", false);
                                    messageMap.put("type", "image");
                                    messageMap.put("time", ServerValue.TIMESTAMP);
                                    messageMap.put("from", mCurrentUserId);

                                    Map messageUserMap = new HashMap();
                                    messageUserMap.put(currentUserRef + "/" + pushId, messageMap);
                                    messageUserMap.put(chatUserRef + "/" + pushId, messageMap);

                                    mChatMessageET.setText("");

                                    mRootDataBase.updateChildren(messageUserMap, new DatabaseReference.CompletionListener()
                                    {
                                        @Override
                                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference)
                                        {
                                            if (databaseError != null)
                                            {
                                                Log.d("CHAT_LOG", databaseError.getMessage().toString());
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    }
                });
            }
        }
    }
}
