package com.aiclothes.app.utils;

import android.content.Context;
import android.content.res.AssetManager;

import com.aiclothes.app.model.CityData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CityPickerUtils {
    private static List<CityData> cityDataList;
    
    // 从assets加载城市数据
    public static List<CityData> loadCityData(Context context) {
        if (cityDataList != null) {
            return cityDataList;
        }
        
        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("city_data.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            
            StringBuilder jsonString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }
            
            reader.close();
            inputStream.close();
            
            Gson gson = new Gson();
            Type listType = new TypeToken<List<CityData>>(){}.getType();
            cityDataList = gson.fromJson(jsonString.toString(), listType);
            
            return cityDataList;
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    // 获取所有省份名称
    public static List<String> getProvinceNames(List<CityData> cityDataList) {
        List<String> provinceNames = new ArrayList<>();
        for (CityData cityData : cityDataList) {
            provinceNames.add(cityData.getProvince());
        }
        return provinceNames;
    }
    
    // 根据省份获取城市列表
    public static List<CityData.City> getCitiesByProvince(List<CityData> cityDataList, String provinceName) {
        for (CityData cityData : cityDataList) {
            if (cityData.getProvince().equals(provinceName)) {
                return cityData.getCity();
            }
        }
        return new ArrayList<>();
    }
    
    // 获取城市名称列表
    public static List<String> getCityNames(List<CityData.City> cities) {
        List<String> cityNames = new ArrayList<>();
        for (CityData.City city : cities) {
            cityNames.add(city.getName());
        }
        return cityNames;
    }
    
    // 根据城市名称获取adcode
    public static String getAdcodeByCity(List<CityData.City> cities, String cityName) {
        for (CityData.City city : cities) {
            if (city.getName().equals(cityName)) {
                return city.getAdcode();
            }
        }
        return "";
    }
    
    // 根据adcode获取城市信息
    public static CityData.City getCityByAdcode(List<CityData> cityDataList, String adcode) {
        for (CityData cityData : cityDataList) {
            for (CityData.City city : cityData.getCity()) {
                if (city.getAdcode().equals(adcode)) {
                    return city;
                }
            }
        }
        return null;
    }
    
    // 根据adcode获取省份信息
    public static String getProvinceByAdcode(List<CityData> cityDataList, String adcode) {
        for (CityData cityData : cityDataList) {
            for (CityData.City city : cityData.getCity()) {
                if (city.getAdcode().equals(adcode)) {
                    return cityData.getProvince();
                }
            }
        }
        return "";
    }
}