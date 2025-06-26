package com.aiclothes.app.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.core.app.ActivityCompat;

import com.aiclothes.app.model.CityData;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationUtils {
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1分钟
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10米
    
    public interface LocationCallback {
        void onLocationReceived(double latitude, double longitude, String cityName);
        void onLocationFailed(String error);
    }
    
    public static void getCurrentLocation(Context context, LocationCallback callback) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        
        if (locationManager == null) {
            callback.onLocationFailed("定位服务不可用");
            return;
        }
        
        // 检查权限
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback.onLocationFailed("没有定位权限");
            return;
        }
        
        // 检查GPS是否可用
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        
        if (!isGPSEnabled && !isNetworkEnabled) {
            callback.onLocationFailed("GPS和网络定位都不可用");
            return;
        }
        
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                locationManager.removeUpdates(this);
                
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                
                // 反向地理编码获取城市名称
                getCityNameFromLocation(context, latitude, longitude, callback);
            }
            
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            
            @Override
            public void onProviderEnabled(String provider) {}
            
            @Override
            public void onProviderDisabled(String provider) {}
        };
        
        try {
            // 优先使用GPS
            if (isGPSEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    locationListener
                );
                
                // 尝试获取最后已知位置
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null) {
                    double latitude = lastKnownLocation.getLatitude();
                    double longitude = lastKnownLocation.getLongitude();
                    getCityNameFromLocation(context, latitude, longitude, callback);
                    return;
                }
            }
            
            // 使用网络定位
            if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    locationListener
                );
                
                // 尝试获取最后已知位置
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (lastKnownLocation != null) {
                    double latitude = lastKnownLocation.getLatitude();
                    double longitude = lastKnownLocation.getLongitude();
                    getCityNameFromLocation(context, latitude, longitude, callback);
                    return;
                }
            }
            
            // 如果没有获取到位置，设置超时
            new android.os.Handler().postDelayed(() -> {
                locationManager.removeUpdates(locationListener);
                callback.onLocationFailed("定位超时");
            }, 30000); // 30秒超时
            
        } catch (Exception e) {
            callback.onLocationFailed("定位异常: " + e.getMessage());
        }
    }
    
    private static void getCityNameFromLocation(Context context, double latitude, double longitude, LocationCallback callback) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String cityName = address.getLocality(); // 城市
                String adminArea = address.getAdminArea(); // 省份
                
                // 如果城市名为空，尝试使用其他字段
                if (TextUtils.isEmpty(cityName)) {
                    cityName = address.getSubAdminArea(); // 区县
                }
                
                if (TextUtils.isEmpty(cityName)) {
                    cityName = adminArea; // 省份
                }
                
                if (!TextUtils.isEmpty(cityName)) {
                    // 处理城市名称，去掉"市"字
                    if (cityName.endsWith("市")) {
                        cityName = cityName.substring(0, cityName.length() - 1);
                    }
                    callback.onLocationReceived(latitude, longitude, cityName);
                } else {
                    callback.onLocationFailed("无法获取城市信息");
                }
            } else {
                callback.onLocationFailed("反向地理编码失败");
            }
        } catch (IOException e) {
            callback.onLocationFailed("地理编码服务异常: " + e.getMessage());
        } catch (Exception e) {
            callback.onLocationFailed("获取城市信息异常: " + e.getMessage());
        }
    }
    
    /**
     * 根据城市名称查找对应的行政编码
     */
    public static String getAdcodeByCity(String cityName, List<CityData> cityDataList) {
        if (TextUtils.isEmpty(cityName) || cityDataList == null) {
            return "";
        }
        
        // 清理城市名称
        String cleanCityName = cityName.trim();
        if (cleanCityName.endsWith("市")) {
            cleanCityName = cleanCityName.substring(0, cleanCityName.length() - 1);
        }
        
        // 遍历所有省份和城市
        for (CityData provinceData : cityDataList) {
            if (provinceData.getCity() != null) {
                for (CityData.City city : provinceData.getCity()) {
                    String cityNameInData = city.getName();
                    if (cityNameInData.endsWith("市")) {
                        cityNameInData = cityNameInData.substring(0, cityNameInData.length() - 1);
                    }
                    
                    // 精确匹配
                    if (cleanCityName.equals(cityNameInData)) {
                        return city.getAdcode();
                    }
                    
                    // 模糊匹配
                    if (cleanCityName.contains(cityNameInData) || cityNameInData.contains(cleanCityName)) {
                        return city.getAdcode();
                    }
                }
            }
        }
        
        // 如果没有找到，尝试匹配省份名称
        for (CityData provinceData : cityDataList) {
            String provinceName = provinceData.getProvince();
            if (provinceName.endsWith("省") || provinceName.endsWith("市") || provinceName.endsWith("区")) {
                provinceName = provinceName.substring(0, provinceName.length() - 1);
            }
            
            if (cleanCityName.equals(provinceName) || cleanCityName.contains(provinceName)) {
                // 返回省会城市的编码
                if (provinceData.getCity() != null && !provinceData.getCity().isEmpty()) {
                    return provinceData.getCity().get(0).getAdcode();
                }
            }
        }
        
        return "";
    }
}