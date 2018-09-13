package com.example.leeronziv.alohaworld_chatvideo;

import android.Manifest;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class VideoSessionActivity extends AppCompatActivity implements Session.SessionListener, PublisherKit.PublisherListener
{
    private static String API_KEY;
    private String SESSION_ID;
    private String TOKEN;

    private String mCurrentUserId;
    private String mFriendName;
    private String mFriendUserId;
    private String mCallStatus;
    private String mDataBaseSessionId;

    private MediaPlayer mRingingSound;

    private LinearLayout mAnimationLayout;
    AnimationDrawable mAnimationDrawable;

    private static final String LOG_TAG = VideoSessionActivity.class.getSimpleName();
    private static final int RC_VIDEO_APP_PERM = 124;

    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;

    private FrameLayout mPublisherViewContainer;
    private FrameLayout mSubscriberViewContainer;

    private ProgressBar mProgressBar;

    private TextView mCallingUserNameTV;

    private DatabaseReference mVideoSessionDataBase;

    public void fetchSessionConnectionData()
    {
        RequestQueue reqQueue = Volley.newRequestQueue(this);
        reqQueue.add(new JsonObjectRequest(Request.Method.GET,
                "https://alohachatapp.herokuapp.com" + "/session",
                null, new Response.Listener<JSONObject>()
        {

            @Override
            public void onResponse(JSONObject response)
            {
                try
                {
                    API_KEY = response.getString("apiKey");
                    SESSION_ID = response.getString("sessionId");
                    TOKEN = response.getString("token");

                    Log.i(LOG_TAG, "API_KEY: " + API_KEY);
                    Log.i(LOG_TAG, "SESSION_ID: " + SESSION_ID);
                    Log.i(LOG_TAG, "TOKEN: " + TOKEN);

                    mSession = new Session.Builder(VideoSessionActivity.this, API_KEY, SESSION_ID).build();
                    mSession.setSessionListener(VideoSessionActivity.this);
                    mSession.connect(TOKEN);
                }
                catch (JSONException error)
                {
                    Log.e(LOG_TAG, "Web Service error: " + error.getMessage());
                }
            }
        }, new Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError error)
            {
                Log.e(LOG_TAG, "Web Service error: " + error.getMessage());
            }
        }));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_session);

        mPublisherViewContainer = (FrameLayout)findViewById(R.id.publisher_container);
        mSubscriberViewContainer = (FrameLayout)findViewById(R.id.subscriber_container);

        mCallingUserNameTV = (TextView) findViewById(R.id.video_session_calling_to);

        mAnimationLayout = (LinearLayout) findViewById(R.id.video_session_animation_layout);
        mAnimationDrawable = (AnimationDrawable) mAnimationLayout.getBackground();
        mAnimationDrawable.setEnterFadeDuration(4500);
        mAnimationDrawable.setExitFadeDuration(4500);
        mAnimationDrawable.start();

        mProgressBar = (ProgressBar) findViewById(R.id.video_session_progress_bar);

        mCurrentUserId = getIntent().getStringExtra("current_user_id");
        mFriendUserId = getIntent().getStringExtra("friend_user_id");
        mFriendName = getIntent().getStringExtra("friend_name");
        mCallStatus = getIntent().getStringExtra("call_status");
        mDataBaseSessionId = getIntent().getStringExtra("session_id");

        mVideoSessionDataBase = FirebaseDatabase.getInstance().getReference().child("video_sessions");

        mRingingSound = MediaPlayer.create(VideoSessionActivity.this, R.raw.ringing_sound);

        updateLoadingUI(true);

        if(getIntent().getStringExtra("call_status") != null)
        {
            mRingingSound.start();

            String text = "calling " + mFriendName + "...";
            mCallingUserNameTV.setText(text);
            mCallingUserNameTV.setVisibility(View.VISIBLE);
        }

        requestPermissions();
        fetchSessionConnectionData();
    }

    ////////////////////////////////////////////////// Request Permissions //////////////////////////////////////////////////


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions()
    {
        String[] perms = { android.Manifest.permission.INTERNET, android.Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO };

        if (!EasyPermissions.hasPermissions(this, perms))
        {
            EasyPermissions.requestPermissions(this, "This app needs access to your camera and mic to make video calls", RC_VIDEO_APP_PERM, perms);
        }
    }


    ////////////////////////////////////////////////// SessionListener methods //////////////////////////////////////////////////


    @Override
    public void onConnected(Session session)
    {
        Log.i(LOG_TAG, "Session Connected");
        Toast.makeText(this, "onConnected", Toast.LENGTH_SHORT).show();

        mPublisher = new Publisher.Builder(this).build();
        mPublisher.setPublisherListener(VideoSessionActivity.this);

        mPublisherViewContainer.addView(mPublisher.getView());
        mSession.publish(mPublisher);
    }

    @Override
    public void onDisconnected(Session session)
    {
        Log.i(LOG_TAG, "Session Disconnected");
        Toast.makeText(this, "onDisconnected", Toast.LENGTH_SHORT).show();

        updateDataBase();
    }

    @Override
    public void onStreamReceived(Session session, Stream stream)
    {
        Log.i(LOG_TAG, "Stream Received");
        Toast.makeText(this, "onStreamReceived", Toast.LENGTH_SHORT).show();

        if (mSubscriber == null)
        {
            mSubscriber = new Subscriber.Builder(this, stream).build();
            mSession.subscribe(mSubscriber);
            mSubscriberViewContainer.addView(mSubscriber.getView());

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateLoadingUI(false);

                    if(mRingingSound.isPlaying())
                    {
                        mRingingSound.stop();
                    }
                }
            }, 2000);
        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream)
    {
        Log.i(LOG_TAG, "Stream Dropped");

        if (mSubscriber != null)
        {
            mSubscriber = null;
            mSubscriberViewContainer.removeAllViews();
        }

        Toast.makeText(this, "Call Ended", Toast.LENGTH_SHORT).show();

        updateDataBase();
        if(mRingingSound.isPlaying()) { mRingingSound.release(); }
        finish();
    }

    private void updateDataBase()
    {
        if(mCurrentUserId != null)
        {
            Map videoSessionDataBaseMap = new HashMap();
            videoSessionDataBaseMap.put(mCurrentUserId + "/" + mFriendUserId + "/" + mDataBaseSessionId, null);
            videoSessionDataBaseMap.put(mFriendUserId + "/" + mCurrentUserId + "/" + mDataBaseSessionId, null);

            mVideoSessionDataBase.updateChildren(videoSessionDataBaseMap, new DatabaseReference.CompletionListener()
            {
                @Override
                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference)
                {
                    if(databaseError != null)
                    {
                        Toast.makeText(VideoSessionActivity.this, "Error updating data base", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @Override
    public void onError(Session session, OpentokError opentokError)
    {
        Log.e(LOG_TAG, "Session error: " + opentokError.getMessage());
        Toast.makeText(this, "onError", Toast.LENGTH_SHORT).show();
    }


    ////////////////////////////////////////////////// PublisherListener methods //////////////////////////////////////////////////


    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream)
    {
        Log.i(LOG_TAG, "Publisher onStreamCreated");
        Toast.makeText(this, "onStreamCreated", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream)
    {
        Log.i(LOG_TAG, "Publisher onStreamDestroyed");
        Toast.makeText(this, "onStreamDestroyed", Toast.LENGTH_SHORT).show();

        updateDataBase();
    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError)
    {
        Log.e(LOG_TAG, "Publisher error: " + opentokError.getMessage());
        Toast.makeText(this, "onError", Toast.LENGTH_SHORT).show();
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public void endCallBtnClickListener(View view)
    {
        if(mSession != null)
        {
            mSession.disconnect();
        }

        if(mRingingSound.isPlaying()) { mRingingSound.stop(); }
        finish();
    }

    private void updateLoadingUI(boolean isLoading)
    {
        if(isLoading)
        {
            mPublisherViewContainer.setVisibility(View.INVISIBLE);
            mSubscriberViewContainer.setVisibility(View.INVISIBLE);

            mProgressBar.setVisibility(View.VISIBLE);
            mAnimationLayout.setVisibility(View.VISIBLE);

            mFriendName = getIntent().getStringExtra("friend_name");
        }
        else
        {
            mPublisherViewContainer.setVisibility(View.VISIBLE);
            mSubscriberViewContainer.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.INVISIBLE);
            mCallingUserNameTV.setVisibility(View.INVISIBLE);
            mAnimationLayout.setVisibility(View.INVISIBLE);
            mAnimationDrawable.stop();
        }
    }
}
