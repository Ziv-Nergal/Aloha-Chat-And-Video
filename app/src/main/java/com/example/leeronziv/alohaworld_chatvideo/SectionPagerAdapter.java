package com.example.leeronziv.alohaworld_chatvideo;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

class SectionPagerAdapter extends FragmentPagerAdapter
{
    public SectionPagerAdapter(FragmentManager fm)
    {
        super(fm);
    }

    @Override
    public Fragment getItem(int position)
    {
        switch (position)
        {
            case 0:
                return new ChatsFragment();
            case 1:
                return new RequestsFragment();
            case 2:
                return new FriendsFragment();
            default:
                return null;
        }
    }

    @Override
    public int getCount()
    {
        return 3;
    }

    public CharSequence getPageTitle(int position)
    {
        switch (position)
        {
            case 0:
                return "CHATS";
            case 1:
                return "REQUESTS";
            case 2:
                return "FRIENDS";
            default:
                return null;
        }
    }
}
