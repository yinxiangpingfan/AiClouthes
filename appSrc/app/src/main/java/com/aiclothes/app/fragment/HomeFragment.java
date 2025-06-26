package com.aiclothes.app.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.aiclothes.app.R;

public class HomeFragment extends Fragment {
    private CardView cardWeather, cardWardrobe, cardTryOn, cardProfile;
    private TextView tvTitle, tvSubtitle;
    private ImageView ivBanner;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initViews(view);
        initListeners();
        return view;
    }
    
    private void initViews(View view) {
        cardWeather = view.findViewById(R.id.card_weather);
        cardWardrobe = view.findViewById(R.id.card_wardrobe);
        cardTryOn = view.findViewById(R.id.card_try_on);
        cardProfile = view.findViewById(R.id.card_profile);
        tvTitle = view.findViewById(R.id.tv_title);
        tvSubtitle = view.findViewById(R.id.tv_subtitle);
        ivBanner = view.findViewById(R.id.iv_banner);
    }
    
    private void initListeners() {
        cardWeather.setOnClickListener(v -> {
            // 切换到天气穿搭页面
            if (getActivity() != null) {
                getActivity().findViewById(R.id.nav_weather).performClick();
            }
        });
        
        cardWardrobe.setOnClickListener(v -> {
            // 切换到衣柜搭配页面
            if (getActivity() != null) {
                getActivity().findViewById(R.id.nav_wardrobe).performClick();
            }
        });
        
        cardTryOn.setOnClickListener(v -> {
            // 切换到AI试衣页面
            if (getActivity() != null) {
                getActivity().findViewById(R.id.nav_try_on).performClick();
            }
        });
        
        cardProfile.setOnClickListener(v -> {
            // 切换到个人信息页面
            if (getActivity() != null) {
                getActivity().findViewById(R.id.nav_profile).performClick();
            }
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 可以在这里刷新数据
    }
}