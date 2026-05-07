package com.womensafety.shajt3ch;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

public class Safety extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager viewPager;

    private ArrayList<Fragment> fragments;
    private final String[] titles = {"First Aid", "Self Defence"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safety);

        tabLayout = findViewById(R.id.tabs);
        viewPager = findViewById(R.id.vp);

        fragments = new ArrayList<>();
        fragments.add(DataHolder.getInstance("First_Aid"));
        fragments.add(DataHolder.getInstance("Self_defence"));

        MyPagerAdapter adapter =
                new MyPagerAdapter(getSupportFragmentManager(),
                        fragments,
                        titles);

        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
    }
}