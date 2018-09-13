package com.example.leeronziv.alohaworld_chatvideo;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder>
{
    private List<Message> mMessageList;
    private DatabaseReference mUserDataBase;

    MessageAdapter(List<Message> mMessageList)
    {
        this.mMessageList = mMessageList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.messages_cell, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder messageViewHolder, int position)
    {
        final Message message = mMessageList.get(position);

        String senderId = message.getFrom();
        String messageType = message.getType();

        mUserDataBase = FirebaseDatabase.getInstance().getReference().child("Users").child(senderId);
        mUserDataBase.keepSynced(true);

        mUserDataBase.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                String name = dataSnapshot.child("name").getValue().toString();
                String image = dataSnapshot.child("thumb_image").getValue().toString();
                long timeStamp = message.getTime();

                Date date = new Date(timeStamp);
                DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                        DateFormat.SHORT, Locale.getDefault());

                messageViewHolder.messageUserName.setText(name);
                messageViewHolder.messageTime.setText(dateFormat.format(date));

                Picasso.get().load(image).placeholder(R.drawable.blank_contact).into(messageViewHolder.profileImage);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

        if(messageType.equals("text"))
        {
            messageViewHolder.messageText.setText(message.getMessage());
            messageViewHolder.messageImage.setVisibility(View.GONE);
        }
        else
        {
            messageViewHolder.messageText.setVisibility(View.INVISIBLE);
            messageViewHolder.messageImage.setVisibility(View.VISIBLE);
            Picasso
                    .get()
                    .load(message.getMessage())
                    .fit()
                    .centerInside()
                    .placeholder(R.drawable.progress_animation)
                    .into(messageViewHolder.messageImage);
        }

        messageViewHolder.messageText.setText(message.getMessage());
    }

    @Override
    public int getItemCount()
    {
        return mMessageList.size();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder
    {
        TextView messageUserName;
        TextView messageText;
        TextView messageTime;
        CircleImageView profileImage;
        ImageView messageImage;

        MessageViewHolder(View itemView)
        {
            super(itemView);

            messageUserName = (TextView) itemView.findViewById(R.id.messages_user_name_text_view);
            messageText = (TextView) itemView.findViewById(R.id.messages_message_text_view);
            messageTime = (TextView) itemView.findViewById(R.id.messages_time_text_view);
            profileImage = (CircleImageView) itemView.findViewById(R.id.messages_sender_image);
            messageImage = (ImageView) itemView.findViewById(R.id.message_image_message);
        }
    }
}
