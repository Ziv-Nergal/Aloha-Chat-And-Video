package com.example.leeronziv.alohaworld_chatvideo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * A simple {@link Fragment} subclass.
 */

public class ChatsFragment extends Fragment
{
    private RecyclerView mConversationsListRV;

    private DatabaseReference mConversationsDatabase;
    private DatabaseReference mMessageDatabase;
    private DatabaseReference mUsersDatabase;

    private FirebaseAuth mAuth;

    private String mCurrent_user_id;

    private View mMainView;

    public ChatsFragment() { /* Required empty public constructor */ }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        mMainView = inflater.inflate(R.layout.fragment_chats, container, false);

        mConversationsListRV = (RecyclerView) mMainView.findViewById(R.id.conversation_list);
        mAuth = FirebaseAuth.getInstance();

        if(mAuth.getCurrentUser() != null)
        {
            mCurrent_user_id = mAuth.getCurrentUser().getUid();
        }

        mConversationsDatabase = FirebaseDatabase.getInstance().getReference().child("chat").child(mCurrent_user_id);
        mConversationsDatabase.keepSynced(true);

        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mUsersDatabase.keepSynced(true);

        mMessageDatabase = FirebaseDatabase.getInstance().getReference().child("messages").child(mCurrent_user_id);
        mMessageDatabase.keepSynced(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);

        mConversationsListRV.setHasFixedSize(true);
        mConversationsListRV.setLayoutManager(linearLayoutManager);

        return mMainView;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        FirebaseRecyclerOptions<Conversation> options =
                new FirebaseRecyclerOptions.Builder<Conversation>()
                        .setQuery(mConversationsDatabase.orderByChild("time_stamp"), Conversation.class)
                        .build();

        FirebaseRecyclerAdapter<Conversation, ConversationViewHolder> fireBaseConversationAdapter =
                new FirebaseRecyclerAdapter<Conversation, ConversationViewHolder>(options)
                {
                    @Override
                    protected void onBindViewHolder(@NonNull final ConversationViewHolder conversationViewHolder,
                                                    int position, @NonNull final Conversation conversation)
                    {
                        final String listUserId = getRef(position).getKey();

                        assert listUserId != null;
                        Query lastMessageQuery = mMessageDatabase.child(listUserId).limitToLast(1);

                        lastMessageQuery.addChildEventListener(new ChildEventListener()
                        {
                            @Override
                            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s)
                            {
                                Message message = dataSnapshot.getValue(Message.class);

                                String messageText = dataSnapshot.child("message").getValue().toString();

                                assert message != null;
                                long timeStamp = message.getTime();

                                conversationViewHolder.setTime(timeStamp);
                                conversationViewHolder.setMessage(messageText);
                            }

                            @Override
                            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) {}

                            @Override
                            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}

                            @Override
                            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String s) {}

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {}
                        });

                        mUsersDatabase.child(listUserId).addValueEventListener(new ValueEventListener()
                        {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                            {
                                final String userName = dataSnapshot.child("name").getValue().toString();
                                String userThumb = dataSnapshot.child("thumb_image").getValue().toString();

                                conversationViewHolder.setName(userName);
                                conversationViewHolder.setUserImage(userThumb);

                                conversationViewHolder.mView.setOnClickListener(new View.OnClickListener()
                                {
                                    @Override
                                    public void onClick(View view)
                                    {
                                        Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                        chatIntent.putExtra("user_id", listUserId);
                                        chatIntent.putExtra("user_name", userName);
                                        startActivity(chatIntent);
                                    }
                                });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {}
                        });
                    }

                    @NonNull
                    @Override
                    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
                    {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.conversation_cell, parent, false);

                        return new ConversationViewHolder(view);
                    }
                };

        fireBaseConversationAdapter.startListening();
        mConversationsListRV.setAdapter(fireBaseConversationAdapter);
        mConversationsListRV.addItemDecoration(new SimpleDividerItemDecoration(getContext()));
    }

    public static class ConversationViewHolder extends RecyclerView.ViewHolder
    {
        View mView;

        TextView mUserNameTV;
        TextView mMessageTV;
        TextView mMessageTimeTV;

        CircleImageView mUserImageView;

        ConversationViewHolder(View itemView)
        {
            super(itemView);
            mView = itemView;
        }

        public void setMessage(String message)
        {
            mMessageTV = (TextView) mView.findViewById(R.id.conversation_message_text_view);
            mMessageTV.setText(message);
        }

        public void setName(String name)
        {
            mUserNameTV = (TextView) mView.findViewById(R.id.conversation_user_name_text_view);
            mUserNameTV.setText(name);
        }

        public void setUserImage(String thumb_image)
        {
            mUserImageView = (CircleImageView) mView.findViewById(R.id.conversation_sender_image);
            Picasso.get().load(thumb_image).placeholder(R.drawable.blank_contact).into(mUserImageView);
        }

        public void setTime(long timeStamp)
        {
            mMessageTimeTV = mView.findViewById(R.id.conversation_time_text_view);

            Date date = new Date(timeStamp);
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                    DateFormat.SHORT, Locale.getDefault());

            mMessageTimeTV.setText(dateFormat.format(date));
        }
    }
}
