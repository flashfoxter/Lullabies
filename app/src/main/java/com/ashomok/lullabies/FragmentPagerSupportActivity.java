package com.ashomok.lullabies;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.ashomok.lullabies.services.MediaPlayerServiceTools;
import com.viewpagerindicator.library.CircleView;

//import com.squareup.picasso.Picasso;

/**
 * Created by Iuliia on 31.03.2016.
 */
public class FragmentPagerSupportActivity extends AppCompatActivity {

    //seems not safe to use
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    protected static final int NUM_ITEMS = FragmentFactory.musicFragmentSettingsList.size();

    private static final String TAG = FragmentPagerSupportActivity.class.getSimpleName();

    private MyAdapter mAdapter;
    private SeekBar volumeSeekbar;
    private ViewPager mPager;

    private ToggleButton fab;
    private ToggleButton volumeButton;
    private ToggleButton airplanemodeButton;

    private AudioManager audioManager;

    private MediaPlayerServiceTools mService;

    public static final String PAGE_NUMBER_KEY = "page_number";
    public static final String IS_PLAYING_KEY = "is_playing";

    private boolean isPlaying;
    private int currentPageNumber;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.fragment_pager);

            currentPageNumber = 0;
            isPlaying = false;

            if (savedInstanceState != null) {
                currentPageNumber = savedInstanceState.getInt(PAGE_NUMBER_KEY);
                isPlaying = savedInstanceState.getBoolean(IS_PLAYING_KEY);
            }

            mService = MediaPlayerServiceTools.getInstance(getApplicationContext());

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();



        mAdapter = new MyAdapter(getFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(currentPageNumber);
        mPager.addOnPageChangeListener(new OnPageChangeListenerImpl());

        CircleView circleView = (CircleView) findViewById(R.id.circle_view);
        circleView.setColorAccent(getResources().getColor(R.color.colorAccent)); //Optional
        circleView.setColorBase(getResources().getColor(R.color.colorPrimary)); //Optional
        circleView.setViewPager(mPager);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        initFab(currentPageNumber);
        initSeekbar();
        initVolumeBtn();
        initAirplanemodeBtn();
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        currentPageNumber = mPager.getCurrentItem();
        savedInstanceState.putInt(PAGE_NUMBER_KEY, currentPageNumber);
        savedInstanceState.putBoolean(IS_PLAYING_KEY, isPlaying);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {

        int currentPageNumber = mPager.getCurrentItem();
        if (currentPageNumber > 0) {
            mPager.setCurrentItem(--currentPageNumber);
        } else {
            super.onBackPressed();
        }

    }

    @SuppressWarnings("deprecation")
    private void initAirplanemodeBtn() {
        airplanemodeButton = (ToggleButton) findViewById(R.id.airplanemode_btn);
        airplanemodeButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (android.os.Build.VERSION.SDK_INT < 17) {
                    try {
                        // read the airplane mode setting
                        boolean isEnabled = Settings.System.getInt(
                                getContentResolver(),
                                Settings.System.AIRPLANE_MODE_ON, 0) == 1;

                        // toggle airplane mode
                        Settings.System.putInt(
                                getContentResolver(),
                                Settings.System.AIRPLANE_MODE_ON, isEnabled ? 0 : 1);

                        // Post an intent to reload
                        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                        intent.putExtra("state", !isEnabled);
                        sendBroadcast(intent);
                    } catch (ActivityNotFoundException e) {
                        Log.e(TAG, e.getMessage());
                    }
                } else {
                    try {
                        Intent intent = new Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        try {
                            Intent intent = new Intent("android.settings.WIRELESS_SETTINGS");
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } catch (ActivityNotFoundException ex) {
                            Toast.makeText(buttonView.getContext(), R.string.not_able_set_airplane, Toast.LENGTH_SHORT).show();
                        }
                    }
                }

            }
        });
    }

    private void initVolumeBtn() {
        volumeButton = (ToggleButton) findViewById(R.id.volume_btn);
        volumeButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                            0, 0);
                } else {
                    if (volumeSeekbar != null) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                                volumeSeekbar.getProgress(), 0);
                    } else {
                        Log.e(TAG, "volumeSeekbar not initialized, call initSeekbar befor");
                    }
                }

            }
        });
    }

    private void initSeekbar() {
        try {
            volumeSeekbar = (SeekBar) findViewById(R.id.seekbar);
            volumeSeekbar.setMax(audioManager
                    .getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            volumeSeekbar.setProgress(audioManager
                    .getStreamVolume(AudioManager.STREAM_MUSIC));


            volumeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar arg0) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar arg0) {
                }

                @Override
                public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                            progress, 0);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void initFab(final int pageNumber) {
        fab = (ToggleButton) findViewById(R.id.fab);
        fab.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    try {
                        // music is playing
                        isPlaying = true;
                        MusicFragmentSettings settings = FragmentFactory.musicFragmentSettingsList.get(pageNumber);
                        int track = settings.getTrack();
                        mService.play(track, mPager.getCurrentItem());
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }

                } else {

                    isPlaying = false;
                    mService.pause();
                }
            }
        });
    }

    private class OnPageChangeListenerImpl implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(final int position) {
            initFab(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    }

    public static class MyAdapter extends FragmentStatePagerAdapter {

        public MyAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }


        @Override
        public Fragment getItem(int position) {
            if (position < 0) {
                throw new IllegalStateException("Number of page < 0.");
            }
            return FragmentFactory.newInstance(position);
        }
    }
}