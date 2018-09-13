package com.example.leeronziv.alohaworld_chatvideo;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.support.v7.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity
{
    private static final int RC_VIDEO_APP_PERM = 124;

    private FirebaseAuth mAuth;
    private DatabaseReference mUserDataBase;

    @Override
    public void onStart()
    {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.

        if(mAuth.getCurrentUser() == null)
        {
            goToStartPage();
        }
        else
        {
            mUserDataBase.child("online").setValue("true");
        }
    }

    private void goToStartPage()
    {
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions()
    {
        String[] perms = { android.Manifest.permission.INTERNET, android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO };

        if (!EasyPermissions.hasPermissions(this, perms))
        {
            EasyPermissions.requestPermissions(this, "This app needs access to your camera and mic to make video calls", RC_VIDEO_APP_PERM, perms);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null)
        {
            mUserDataBase = FirebaseDatabase.getInstance().getReference().child("Users").child(mAuth.getCurrentUser().getUid());
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_page_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("");

        ViewPager viewPager = (ViewPager) findViewById(R.id.main_tab_pager);
        SectionPagerAdapter sectionPagerAdapter = new SectionPagerAdapter(getSupportFragmentManager());

        viewPager.setAdapter(sectionPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.main_activity_tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.log_out_menu_item:
            {
                if(mAuth.getCurrentUser() != null)
                {
                    mUserDataBase.child("online").setValue(false);
                }

                FirebaseAuth.getInstance().signOut();
                goToStartPage();
                return true;
            }
            case R.id.account_settings_menu_item:
            {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            }
            case R.id.all_users_menu_item:
            {
                startActivity(new Intent(MainActivity.this, UsersActivity.class));
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}