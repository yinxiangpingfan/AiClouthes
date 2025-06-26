package com.aiclothes.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.aiclothes.app.R;
import com.aiclothes.app.fragment.HomeFragment;
import com.aiclothes.app.fragment.ProfileFragment;
import com.aiclothes.app.fragment.TryOnFragment;
import com.aiclothes.app.fragment.WardrobeFragment;
import com.aiclothes.app.fragment.WeatherFragment;
import com.aiclothes.app.network.ApiService;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    private BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager;
    private ApiService apiService;
    
    // Fragment实例
    private HomeFragment homeFragment;
    private WeatherFragment weatherFragment;
    private WardrobeFragment wardrobeFragment;
    private TryOnFragment tryOnFragment;
    private ProfileFragment profileFragment;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        apiService = ApiService.getInstance(this);
        
        // 检查登录状态
        if (!checkLoginStatus()) {
            redirectToLogin();
            return;
        }
        
        initViews();
        initFragments();
        
        // 只在首次创建时显示首页Fragment，Activity重新创建时Fragment会自动恢复
        if (savedInstanceState == null) {
            showFragment(homeFragment);
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } else {
            // Activity重新创建时，根据当前显示的Fragment设置底部导航状态
            setBottomNavigationForCurrentFragment();
        }
        
        // 在Fragment初始化完成后设置监听器，避免重复触发
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
    }
    
    private boolean checkLoginStatus() {
        return apiService.isLoggedIn();
    }
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fragmentManager = getSupportFragmentManager();
    }
    
    private void setBottomNavigationForCurrentFragment() {
        // 检查当前显示的Fragment并设置对应的底部导航项
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof HomeFragment) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } else if (currentFragment instanceof WeatherFragment) {
            bottomNavigationView.setSelectedItemId(R.id.nav_weather);
        } else if (currentFragment instanceof WardrobeFragment) {
            bottomNavigationView.setSelectedItemId(R.id.nav_wardrobe);
        } else if (currentFragment instanceof TryOnFragment) {
            bottomNavigationView.setSelectedItemId(R.id.nav_try_on);
        } else if (currentFragment instanceof ProfileFragment) {
            bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        } else {
            // 默认选中首页
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }
    
    private void initFragments() {
        // 检查是否已经存在Fragment实例（Activity重新创建时）
        homeFragment = (HomeFragment) fragmentManager.findFragmentByTag("HomeFragment");
        if (homeFragment == null) {
            homeFragment = new HomeFragment();
        }
        
        weatherFragment = (WeatherFragment) fragmentManager.findFragmentByTag("WeatherFragment");
        if (weatherFragment == null) {
            weatherFragment = new WeatherFragment();
        }
        
        wardrobeFragment = (WardrobeFragment) fragmentManager.findFragmentByTag("WardrobeFragment");
        if (wardrobeFragment == null) {
            wardrobeFragment = new WardrobeFragment();
        }
        
        tryOnFragment = (TryOnFragment) fragmentManager.findFragmentByTag("TryOnFragment");
        if (tryOnFragment == null) {
            tryOnFragment = new TryOnFragment();
        }
        
        profileFragment = (ProfileFragment) fragmentManager.findFragmentByTag("ProfileFragment");
        if (profileFragment == null) {
            profileFragment = new ProfileFragment();
        }
    }
    
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.nav_home) {
            showFragment(homeFragment);
            return true;
        } else if (itemId == R.id.nav_weather) {
            showFragment(weatherFragment);
            return true;
        } else if (itemId == R.id.nav_wardrobe) {
            showFragment(wardrobeFragment);
            return true;
        } else if (itemId == R.id.nav_try_on) {
            showFragment(tryOnFragment);
            return true;
        } else if (itemId == R.id.nav_profile) {
            showFragment(profileFragment);
            return true;
        }
        
        return false;
    }
    
    private void showFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        
        // 隐藏所有Fragment
        hideAllFragments(transaction);
        
        // 如果Fragment还没有添加，则添加它
        if (!fragment.isAdded()) {
            String tag = getFragmentTag(fragment);
            transaction.add(R.id.fragment_container, fragment, tag);
        } else {
            transaction.show(fragment);
        }
        
        transaction.commit();
    }
    
    private String getFragmentTag(Fragment fragment) {
        if (fragment instanceof HomeFragment) {
            return "HomeFragment";
        } else if (fragment instanceof WeatherFragment) {
            return "WeatherFragment";
        } else if (fragment instanceof WardrobeFragment) {
            return "WardrobeFragment";
        } else if (fragment instanceof TryOnFragment) {
            return "TryOnFragment";
        } else if (fragment instanceof ProfileFragment) {
            return "ProfileFragment";
        }
        return fragment.getClass().getSimpleName();
    }
    
    private void hideAllFragments(FragmentTransaction transaction) {
        if (homeFragment != null && homeFragment.isAdded()) {
            transaction.hide(homeFragment);
        }
        if (weatherFragment != null && weatherFragment.isAdded()) {
            transaction.hide(weatherFragment);
        }
        if (wardrobeFragment != null && wardrobeFragment.isAdded()) {
            transaction.hide(wardrobeFragment);
        }
        if (tryOnFragment != null && tryOnFragment.isAdded()) {
            transaction.hide(tryOnFragment);
        }
        if (profileFragment != null && profileFragment.isAdded()) {
            transaction.hide(profileFragment);
        }
    }
    
    @Override
    public void onBackPressed() {
        // 如果当前不是首页，则切换到首页
        if (bottomNavigationView.getSelectedItemId() != R.id.nav_home) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } else {
            // 双击退出应用
            super.onBackPressed();
        }
    }
}