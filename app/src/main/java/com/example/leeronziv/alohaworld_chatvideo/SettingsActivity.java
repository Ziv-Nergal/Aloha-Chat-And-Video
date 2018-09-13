package com.example.leeronziv.alohaworld_chatvideo;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class SettingsActivity extends AppCompatActivity
{
    private DatabaseReference mUserDataBase;
    private FirebaseUser mCurrentUser;

    private CircleImageView mProfilePic;
    private TextView mUserNameTV;
    private TextView mStatusTV;

    private StorageReference mImageStorage;

    private ProgressBar mSettingsProgressBar;

    private String mCurrentUserID;
    private String mImageDownloadUrl;
    private String mThumbImageDownloadUrl;

    private Typeface mFont;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mProfilePic = (CircleImageView) findViewById(R.id.settings_image);
        mUserNameTV = (TextView) findViewById(R.id.user_name_text_view);
        mStatusTV = (TextView) findViewById(R.id.status_text_view);

        mFont = Typeface.createFromAsset(getAssets(), "fonts/atlas_bold.otf");

        setToolBar();

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (mCurrentUser != null) { mCurrentUserID = mCurrentUser.getUid(); }

        mUserDataBase = FirebaseDatabase.getInstance().getReference().child("Users").child(mCurrentUserID);
        mUserDataBase.keepSynced(true);

        mImageStorage = FirebaseStorage.getInstance().getReference();

        mUserDataBase.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                String userNameStr = Objects.requireNonNull(dataSnapshot.child("name").getValue()).toString();
                String imageStr = Objects.requireNonNull(dataSnapshot.child("image").getValue()).toString();
                String statusStr = Objects.requireNonNull(dataSnapshot.child("status").getValue()).toString();
                final String thumbImageStr = Objects.requireNonNull(dataSnapshot.child("thumb_image").getValue()).toString();

                mUserNameTV.setText(userNameStr);
                mStatusTV.setText(statusStr);

                if(!imageStr.equals("default"))
                {
                    Picasso.get().load(thumbImageStr)
                        .networkPolicy(NetworkPolicy.OFFLINE)
                        .placeholder(R.drawable.blank_contact)
                            .into(mProfilePic, new Callback()
                            {
                                @Override
                                public void onSuccess() {}

                                @Override
                                public void onError(Exception e)
                                {
                                    Picasso.get().load(thumbImageStr).placeholder(R.drawable.blank_contact).into(mProfilePic);
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void setToolBar()
    {
        Toolbar toolBar;
        toolBar = (Toolbar) findViewById(R.id.users_toolbar);
        TextView toolbarTitleTV = (TextView) toolBar.findViewById(R.id.title_text_view);
        toolbarTitleTV.setText(R.string.account_settings);
        toolbarTitleTV.setVisibility(View.VISIBLE);
        setSupportActionBar(toolBar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void changePhotoBtnClickListener(View view)
    {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setCropShape(CropImageView.CropShape.OVAL)
                .setAspectRatio(1,1)
                .start(SettingsActivity.this);
    }

    public void editStatusBtnClickListener(View view)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.settings_dialog, null);

        final EditText newInputEditText = (EditText) dialogView.findViewById(R.id.new_input);
        newInputEditText.setHint("New Status");

        builder.setView(dialogView).setPositiveButton(Html.fromHtml("<font color='#000'>Done</font>"), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                String newStatus = newInputEditText.getText().toString();

                if(!newStatus.isEmpty())
                {
                    mUserDataBase.child("status").setValue(newStatus);
                }

                dialog.dismiss();
            }
        }).setNegativeButton(Html.fromHtml("<font color='#000'>Cancel</font>"), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });

        builder.create();
        AlertDialog alertDialog = builder.create();

        alertDialog.show();

        Button negativeButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        negativeButton.setTypeface(mFont);
        negativeButton.setTextSize(18);

        Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.setTypeface(mFont);
        positiveButton.setTextSize(18);
    }

    public void editUserNameBtnClickListener(View view)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.settings_dialog, null);

        final EditText newInputEditText = (EditText) dialogView.findViewById(R.id.new_input);
        newInputEditText.setHint("New User Name");

        builder.setView(dialogView).setPositiveButton(Html.fromHtml("<font color='#000'>Done</font>"), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                String newUserName = newInputEditText.getText().toString();

                if(!newUserName.isEmpty())
                {
                    mUserDataBase.child("name").setValue(newUserName);
                }

                dialog.dismiss();
            }
        }).setNegativeButton(Html.fromHtml("<font color='#000'>Cancel</font>"), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });

        builder.create();
        AlertDialog alertDialog = builder.create();

        alertDialog.show();

        Button negativeButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        negativeButton.setTypeface(mFont);
        negativeButton.setTextSize(18);

        Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.setTypeface(mFont);
        positiveButton.setTextSize(18);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK)
            {
                mSettingsProgressBar = (ProgressBar) findViewById(R.id.settings_Progress_bar);
                mSettingsProgressBar.setVisibility(View.VISIBLE);

                Uri resultUri = result.getUri();
                try
                {
                    File thumbFilePath = new File(resultUri.getPath());

                    Bitmap thumbNailBitmap = new Compressor(this)
                            .setMaxWidth(200)
                            .setMaxHeight(200)
                            .setQuality(30)
                            .compressToBitmap(thumbFilePath);

                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    thumbNailBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                    byte[] thumbByteArray = byteArrayOutputStream.toByteArray();

                    uploadImagesToDataBase(resultUri, thumbByteArray);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private void uploadImagesToDataBase(Uri resultUri, final byte[] thumbByteArray)
    {
        StorageReference imagePath = mImageStorage.child("profile_images").child(mCurrentUserID + ".jpg");
        final StorageReference thumbImagePath = mImageStorage.child("profile_images").child("thumb_images").child(mCurrentUserID + ".jpg");

        imagePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>()
        {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task)
            {
                if(task.isSuccessful())
                {
                    mImageStorage.child("profile_images").child(mCurrentUserID + ".jpg")
                            .getDownloadUrl()
                            .addOnSuccessListener(new OnSuccessListener<Uri>()
                    {
                        @Override
                        public void onSuccess(Uri uri)
                        {
                            mImageDownloadUrl = uri.toString();
                        }
                    });

                    UploadTask uploadTask = thumbImagePath.putBytes(thumbByteArray);
                    uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>()
                    {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task)
                        {
                            final Task<UploadTask.TaskSnapshot> thumbImageUploadTask = task;

                            mImageStorage
                                    .child("profile_images").child("thumb_images")
                                    .child(mCurrentUserID + ".jpg")
                                    .getDownloadUrl()
                                    .addOnSuccessListener(new OnSuccessListener<Uri>()
                            {
                                @Override
                                public void onSuccess(Uri uri)
                                {
                                    mThumbImageDownloadUrl = uri.toString();

                                    if(thumbImageUploadTask.isSuccessful())
                                    {
                                        updateUserUrlsInDataBase();
                                    }
                                }
                            });
                        }
                    });
                }
                else
                {
                    mSettingsProgressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(SettingsActivity.this, "Error uploading image", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateUserUrlsInDataBase()
    {
        Map imageUrlMap = new HashMap();
        imageUrlMap.put("image", mImageDownloadUrl);
        imageUrlMap.put("thumb_image", mThumbImageDownloadUrl);

        mUserDataBase.updateChildren(imageUrlMap, new DatabaseReference.CompletionListener()
        {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference)
            {
                if (databaseError != null)
                {
                    Toast.makeText(SettingsActivity.this, "Error uploading image!", Toast.LENGTH_SHORT).show();
                }
                mSettingsProgressBar.setVisibility(View.INVISIBLE);
            }
        });
    }
}
