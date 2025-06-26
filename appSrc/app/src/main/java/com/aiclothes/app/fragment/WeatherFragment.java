package com.aiclothes.app.fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.aiclothes.app.R;
import com.aiclothes.app.model.CityData;
import com.aiclothes.app.model.ClothingRecommendation;
import com.aiclothes.app.model.WeatherData;
import com.aiclothes.app.network.ApiService;
import com.aiclothes.app.utils.CityPickerUtils;
import com.aiclothes.app.utils.LocationUtils;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;

import org.json.JSONObject;
import com.google.gson.JsonObject;

import java.util.List;

public class WeatherFragment extends Fragment {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    
    private TextView tvCityName, tvWeatherInfo, tvTemperature, tvRecommendation;
    private ImageView ivRecommendationImage;
    private Button btnGetLocation, btnSelectCity, btnGetRecommendation, btnGenerateImage;
    private CardView cardWeather, cardRecommendation, cardGeneratedImage;
    private ProgressBar progressBar;
    
    private ApiService apiService;
    private List<CityData> cityDataList;
    private String selectedAdcode = "";
    private String selectedCityName = "";
    private WeatherData currentWeatherData;
    private ClothingRecommendation currentRecommendation;
    private boolean isLocationMode = false; // 标记是否使用定位模式
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather, container, false);
        initViews(view);
        initData();
        initListeners();
        return view;
    }
    
    private void initViews(View view) {
        tvCityName = view.findViewById(R.id.tv_city_name);
        tvWeatherInfo = view.findViewById(R.id.tv_weather_info);
        tvTemperature = view.findViewById(R.id.tv_temperature);
        tvRecommendation = view.findViewById(R.id.tv_recommendation);
        ivRecommendationImage = view.findViewById(R.id.iv_recommendation_image);
        btnGetLocation = view.findViewById(R.id.btn_get_location);
        btnSelectCity = view.findViewById(R.id.btn_select_city);
        btnGetRecommendation = view.findViewById(R.id.btn_get_recommendation);
        btnGenerateImage = view.findViewById(R.id.btn_generate_image);
        cardWeather = view.findViewById(R.id.card_weather);
        cardRecommendation = view.findViewById(R.id.card_recommendation);
        cardGeneratedImage = view.findViewById(R.id.card_generated_image);
        progressBar = view.findViewById(R.id.progress_bar);
    }
    
    private void initData() {
        apiService = ApiService.getInstance(getContext());
        cityDataList = CityPickerUtils.loadCityData(getContext());
        
        // 初始状态显示提示信息
        tvCityName.setText("请选择城市");
        showSelectCityPrompt();
    }
    
    private void initListeners() {
        btnGetLocation.setOnClickListener(v -> requestLocationAndGetWeather());
        btnSelectCity.setOnClickListener(v -> showCityPicker());
        btnGetRecommendation.setOnClickListener(v -> getClothingRecommendation());
        btnGenerateImage.setOnClickListener(v -> generateRecommendationImage());
    }
    
    private void showCityPicker() {
        List<String> provinceNames = CityPickerUtils.getProvinceNames(cityDataList);
        String[] provinces = provinceNames.toArray(new String[0]);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("选择省份");
        builder.setItems(provinces, (dialog, which) -> {
            String selectedProvince = provinces[which];
            showCityPicker(selectedProvince);
        });
        builder.show();
    }
    
    private void showCityPicker(String provinceName) {
        List<CityData.City> cities = CityPickerUtils.getCitiesByProvince(cityDataList, provinceName);
        List<String> cityNames = CityPickerUtils.getCityNames(cities);
        String[] cityArray = cityNames.toArray(new String[0]);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("选择城市");
        builder.setItems(cityArray, (dialog, which) -> {
            selectedCityName = cityArray[which];
            selectedAdcode = CityPickerUtils.getAdcodeByCity(cities, selectedCityName);
            isLocationMode = false; // 手动选择城市，重置定位模式
            tvCityName.setText(selectedCityName);
            
            // 获取新城市的天气
            getWeather();
        });
        builder.show();
    }
    
    private void requestLocationAndGetWeather() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 请求定位权限
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 
                             LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        
        // 已有权限，开始定位
        startLocationProcess();
    }
    
    private void startLocationProcess() {
        showLoading(true);
        isLocationMode = true;
        
        LocationUtils.getCurrentLocation(requireContext(), new LocationUtils.LocationCallback() {
            @Override
            public void onLocationReceived(double latitude, double longitude, String cityName) {
                // 根据城市名称查找行政编码
                String adcode = LocationUtils.getAdcodeByCity(cityName, cityDataList);
                if (!TextUtils.isEmpty(adcode)) {
                    selectedAdcode = adcode;
                    selectedCityName = cityName;
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            tvCityName.setText(selectedCityName + " (定位)");
                            getWeather();
                        });
                    }
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(requireContext(), "无法识别当前城市，请手动选择", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
            
            @Override
            public void onLocationFailed(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        isLocationMode = false;
                        Toast.makeText(requireContext(), "定位失败: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationProcess();
            } else {
                Toast.makeText(requireContext(), "需要定位权限才能获取当前位置", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void getWeather() {
        if (TextUtils.isEmpty(selectedAdcode)) {
            Toast.makeText(getContext(), "请先选择城市", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showLoading(true);
        
        apiService.getWeather(selectedAdcode, new ApiService.ApiCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        try {
                            // 检查响应状态
                            int code = response.get("code").getAsInt();
                            if (code == 1000) {
                                // 提取data字段
                                JsonObject dataObject = response.getAsJsonObject("data");
                                Gson gson = new Gson();
                                currentWeatherData = gson.fromJson(dataObject, WeatherData.class);
                            } else {
                                String msg = response.has("msg") ? response.get("msg").getAsString() : "获取天气失败";
                                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            if (currentWeatherData != null && 
                                currentWeatherData.getForecasts() != null && 
                                !currentWeatherData.getForecasts().isEmpty()) {
                                
                                WeatherData.Forecast forecast = currentWeatherData.getForecasts().get(0);
                                if (forecast.getCasts() != null && !forecast.getCasts().isEmpty()) {
                                    WeatherData.Cast todayCast = forecast.getCasts().get(0);
                                    updateWeatherUI(todayCast);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "天气数据解析失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(getContext(), "获取天气失败：" + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    private void updateWeatherUI(WeatherData.Cast cast) {
        // 更新城市名称（删除横杠）
        tvCityName.setText(selectedCityName);
        
        // 构建详细的天气信息
        StringBuilder weatherInfo = new StringBuilder();
        weatherInfo.append("今日天气 ").append(cast.getDayweather());
        if (!cast.getDayweather().equals(cast.getNightweather())) {
            weatherInfo.append(" 转 ").append(cast.getNightweather());
        }
        weatherInfo.append("\n");
        weatherInfo.append("风向：").append(cast.getDaywind()).append("风 ").append(cast.getDaypower()).append("级\n");
        weatherInfo.append("日期：").append(cast.getDate());
        
        tvWeatherInfo.setText(weatherInfo.toString());
        
        // 优化温度显示
        String tempText = String.format("%s℃ ~ %s℃", cast.getNighttemp(), cast.getDaytemp());
        tvTemperature.setText(tempText);
        
        // 显示天气卡片并启用功能按钮
        cardWeather.setVisibility(View.VISIBLE);
        cardRecommendation.setVisibility(View.VISIBLE); // 确保推荐卡片可见
        btnGetRecommendation.setEnabled(true);
        btnGenerateImage.setEnabled(false); // 需要先获取推荐才能生成图片
    }
    
    private void getClothingRecommendation() {
        if (currentWeatherData == null) {
            Toast.makeText(getContext(), "请先获取天气信息", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showLoading(true);
        
        // 构建天气描述
        WeatherData.Forecast forecast = currentWeatherData.getForecasts().get(0);
        WeatherData.Cast todayCast = forecast.getCasts().get(0);
        String weatherDescription = todayCast.getWeatherDescription();
        
        apiService.recommendClothes(weatherDescription, new ApiService.ApiCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        try {
                            // 检查响应状态
                            int code = response.get("code").getAsInt();
                            if (code == 1000) {
                                // 提取data字段
                                JsonObject dataObject = response.getAsJsonObject("data");
                                Gson gson = new Gson();
                                currentRecommendation = gson.fromJson(dataObject, ClothingRecommendation.class);
                            } else {
                                String msg = response.has("msg") ? response.get("msg").getAsString() : "推荐衣物失败";
                                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            if (currentRecommendation != null && 
                                currentRecommendation.getClothingMatch() != null) {
                                updateRecommendationUI();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "推荐数据解析失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(getContext(), "获取推荐失败：" + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    private void updateRecommendationUI() {
        ClothingRecommendation.ClothingMatch match = currentRecommendation.getClothingMatch();
        tvRecommendation.setText(match.getRecommendationText());
        
        cardRecommendation.setVisibility(View.VISIBLE);
        btnGenerateImage.setEnabled(true);
    }
    
    private void generateRecommendationImage() {
        if (currentRecommendation == null) {
            Toast.makeText(getContext(), "请先获取穿搭推荐", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showLoading(true);
        
        ClothingRecommendation.ClothingMatch match = currentRecommendation.getClothingMatch();
        
        String information = match.getUpperGarment() + "," + match.getBottoms();
        apiService.generateWeatherOutfitImage(information, new ApiService.ApiCallback<JsonObject>() {
                @Override
                public void onSuccess(JsonObject response) {
                    String responseStr = response.toString();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showLoading(false);
                            try {
                                JSONObject jsonObject = new JSONObject(responseStr);
                                String imageUrl = jsonObject.optString("url").trim();
                                
                                if (!TextUtils.isEmpty(imageUrl)) {
                                    // 使用Glide加载图片
                                    Glide.with(WeatherFragment.this)
                                            .load(imageUrl)
                                            .placeholder(android.R.drawable.ic_menu_gallery)
                                            .error(android.R.drawable.ic_dialog_alert)
                                            .into(ivRecommendationImage);
                                    
                                    ivRecommendationImage.setVisibility(View.VISIBLE);
                                    cardGeneratedImage.setVisibility(View.VISIBLE);
                                } else {
                                    Toast.makeText(getContext(), "生成图片失败", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(getContext(), "图片数据解析失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
                
                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(getContext(), "生成图片失败：" + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        );
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    
    private void showSelectCityPrompt() {
        // 显示天气卡片但内容为提示信息
        cardWeather.setVisibility(View.VISIBLE);
        cardRecommendation.setVisibility(View.VISIBLE);
        
        // 在天气信息区域显示友好提示
        tvWeatherInfo.setText("🌤️ 欢迎使用智能穿搭助手\n\n📍 请选择您的城市获取天气信息\n🔍 点击上方按钮选择城市或获取当前位置");
        tvTemperature.setText("--°C");
        
        // 在推荐区域显示提示
        tvRecommendation.setText("选择城市后，我将为您推荐合适的穿搭 👔");
        
        // 禁用推荐按钮
        btnGetRecommendation.setEnabled(false);
        btnGenerateImage.setEnabled(false);
    }
}