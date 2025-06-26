package com.aiclothes.app.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiService {
    private static final String TAG = "ApiService";
    private static final String BASE_URL = "http://82.156.59.17:8080";
    private static final String PREF_NAME = "ai_clothes_prefs";
    private static final String TOKEN_KEY = "token";
    
    private static ApiService instance;
    private OkHttpClient client;
    private Gson gson;
    private Context context;
    private SharedPreferences prefs;
    
    private ApiService(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        this.client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .cookieJar(new CookieJar() {
                    private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();
                    
                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                        cookieStore.put(url.host(), cookies);
                        // 保存token到SharedPreferences
                        for (Cookie cookie : cookies) {
                            if ("token".equals(cookie.name())) {
                                saveToken(cookie.value());
                            }
                        }
                    }
                    
                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url) {
                        List<Cookie> cookies = cookieStore.get(url.host());
                        return cookies != null ? cookies : new ArrayList<>();
                    }
                })
                .build();
    }
    
    public static synchronized ApiService getInstance(Context context) {
        if (instance == null) {
            instance = new ApiService(context);
        }
        return instance;
    }
    
    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
    
    public interface StreamCallback {
        void onData(String data);
        void onComplete();
        void onError(String error);
    }
    
    // 保存token
    private void saveToken(String token) {
        prefs.edit().putString(TOKEN_KEY, token).apply();
    }
    
    // 获取token
    private String getToken() {
        return prefs.getString(TOKEN_KEY, "");
    }
    
    // 清除token
    public void clearToken() {
        prefs.edit().remove(TOKEN_KEY).apply();
    }
    
    // 检查是否已登录
    public boolean isLoggedIn() {
        return !getToken().isEmpty();
    }
    
    // 创建带认证的请求
    private Request.Builder createAuthenticatedRequest() {
        Request.Builder builder = new Request.Builder();
        String token = getToken();
        if (!token.isEmpty()) {
            builder.addHeader("Cookie", "token=" + token);
        }
        return builder;
    }
    
    // 用户注册
    public void register(String telephone, String password, String sex, ApiCallback<JsonObject> callback) {
        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("telephone", telephone)
                .addFormDataPart("password", password)
                .addFormDataPart("sex", sex)
                .build();
        
        Request request = new Request.Builder()
                .url(BASE_URL + "/register")
                .post(formBody)
                .build();
        
        executeRequest(request, callback);
    }
    
    // 用户登录
    public void login(String telephone, String password, ApiCallback<JsonObject> callback) {
        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("telephone", telephone)
                .addFormDataPart("password", password)
                .build();
        
        Request request = new Request.Builder()
                .url(BASE_URL + "/login")
                .post(formBody)
                .build();
        
        executeRequest(request, callback);
    }
    
    // 修改密码
    public void changePassword(String newPassword, ApiCallback<JsonObject> callback) {
        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("newpassword", newPassword)
                .build();
        
        Request request = createAuthenticatedRequest()
                .url(BASE_URL + "/user/chanpass")
                .post(formBody)
                .build();
        
        executeRequest(request, callback);
    }
    
    // 退出登录
    public void logout(ApiCallback<JsonObject> callback) {
        Request request = createAuthenticatedRequest()
                .url(BASE_URL + "/user/logout")
                .get()
                .build();
        
        executeRequest(request, new ApiCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject result) {
                clearToken();
                callback.onSuccess(result);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    // 获取天气
    public void getWeather(String code, ApiCallback<JsonObject> callback) {
        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("code", code)
                .build();
        
        Request request = createAuthenticatedRequest()
                .url(BASE_URL + "/user/weather/get")
                .post(formBody)
                .build();
        
        executeRequest(request, callback);
    }
    
    // 根据天气推荐衣物
    public void recommendClothes(String weather, ApiCallback<JsonObject> callback) {
        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("weather", weather)
                .build();
        
        Request request = createAuthenticatedRequest()
                .url(BASE_URL + "/user/weather/make")
                .post(formBody)
                .build();
        
        executeRequest(request, callback);
    }
    
    // 根据推荐生成试穿图片
    public void generateWeatherOutfitImage(String information, ApiCallback<JsonObject> callback) {
        HttpUrl url = HttpUrl.parse(BASE_URL + "/user/weather/makepic")
                .newBuilder()
                .addQueryParameter("information", information)
                .build();
        
        Request request = createAuthenticatedRequest()
                .url(url)
                .get()
                .build();
        
        executeRequest(request, callback);
    }
    
    // 上传衣柜照片
    public void uploadWardrobePhotos(List<File> photos, ApiCallback<JsonObject> callback) {
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
        
        for (File photo : photos) {
            RequestBody fileBody = RequestBody.create(photo, MediaType.parse("image/*"));
            builder.addFormDataPart("pictures", photo.getName(), fileBody);
        }
        
        RequestBody formBody = builder.build();
        
        Request request = createAuthenticatedRequest()
                .url(BASE_URL + "/user/pic/upload")
                .post(formBody)
                .build();
        
        executeRequest(request, callback);
    }
    
    // 解析衣柜照片并推荐（流式输出）
    public void parseWardrobeAndRecommend(String purpose, StreamCallback callback) {
        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("ques", purpose)
                .build();
        
        Request request = createAuthenticatedRequest()
                .url(BASE_URL + "/user/pic/parse")
                .post(formBody)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("HTTP " + response.code());
                    return;
                }
                
                // 处理Server-Sent Events流式响应
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
                    String line;
                    StringBuilder buffer = new StringBuilder();
                    
                    while ((line = reader.readLine()) != null) {
                        // 处理SSE格式：event: message 和 data: {"data": "..."}
                        if (line.startsWith("data: ")) {
                            String jsonData = line.substring(6).trim();
                            if (!jsonData.isEmpty()) {
                                try {
                                    JsonObject json = JsonParser.parseString(jsonData).getAsJsonObject();
                                    if (json.has("data")) {
                                        String dataContent = json.get("data").getAsString();
                                        callback.onData(dataContent);
                                    }
                                } catch (Exception e) {
                                    // 忽略JSON解析错误，继续处理下一行
                                }
                            }
                        }
                        // 忽略event行和空行
                    }
                    callback.onComplete();
                } catch (Exception e) {
                    callback.onError("流式响应处理失败: " + e.getMessage());
                }
            }
        });
    }
    
    // 生成衣柜推荐图片
    public void generateWardrobeImage(ApiCallback<JsonObject> callback) {
        Request request = createAuthenticatedRequest()
                .url(BASE_URL + "/user/pic/makepic")
                .get()
                .build();
        
        executeRequest(request, callback);
    }
    
    // 有模特试套装
    public void tryOnWithModelSingle(File personImage, File outfit, ApiCallback<JsonObject> callback) {
        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("personImage", personImage.getName(), 
                    RequestBody.create(personImage, MediaType.parse("image/*")))
                .addFormDataPart("topGarment", outfit.getName(), 
                    RequestBody.create(outfit, MediaType.parse("image/*")))
                .build();
        
        Request request = createAuthenticatedRequest()
                .url(BASE_URL + "/user/try/single")
                .post(formBody)
                .build();
        
        executeRequest(request, callback);
    }
    
    // 有模特试上衣+裤子
    public void tryOnWithModelDouble(File personImage, File topGarment, File bottomGarment, ApiCallback<JsonObject> callback) {
        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("personImage", personImage.getName(), 
                    RequestBody.create(personImage, MediaType.parse("image/*")))
                .addFormDataPart("topGarment", topGarment.getName(), 
                    RequestBody.create(topGarment, MediaType.parse("image/*")))
                .addFormDataPart("bottomGarment", bottomGarment.getName(), 
                    RequestBody.create(bottomGarment, MediaType.parse("image/*")))
                .build();
        
        Request request = createAuthenticatedRequest()
                .url(BASE_URL + "/user/try/double")
                .post(formBody)
                .build();
        
        executeRequest(request, callback);
    }
    
    // 无模特试套装
    public void tryOnWithoutModelSingle(File outfit, String sex, ApiCallback<JsonObject> callback) {
        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("topGarment", outfit.getName(), 
                    RequestBody.create(outfit, MediaType.parse("image/*")))
                .addFormDataPart("sex", sex)
                .build();
        
        Request request = createAuthenticatedRequest()
                .url(BASE_URL + "/user/try/nomodel1")
                .post(formBody)
                .build();
        
        executeRequest(request, callback);
    }
    
    // 无模特试上衣+裤子
    public void tryOnWithoutModelDouble(File topGarment, File bottomGarment, String sex, ApiCallback<JsonObject> callback) {
        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("topGarment", topGarment.getName(), 
                    RequestBody.create(topGarment, MediaType.parse("image/*")))
                .addFormDataPart("bottomGarment", bottomGarment.getName(), 
                    RequestBody.create(bottomGarment, MediaType.parse("image/*")))
                .addFormDataPart("sex", sex)
                .build();
        
        Request request = createAuthenticatedRequest()
                .url(BASE_URL + "/user/try/nomodel2")
                .post(formBody)
                .build();
        
        executeRequest(request, callback);
    }
    
    // 执行请求的通用方法
    private void executeRequest(Request request, ApiCallback<JsonObject> callback) {
        Log.d(TAG, "发送请求到: " + request.url());
        Log.d(TAG, "请求方法: " + request.method());
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "网络请求失败 - URL: " + request.url() + ", 错误: " + e.getMessage(), e);
                callback.onError("网络请求失败: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "收到响应 - 状态码: " + response.code() + ", URL: " + request.url());
                
                if (!response.isSuccessful()) {
                    Log.e(TAG, "HTTP错误 - 状态码: " + response.code() + ", 消息: " + response.message());
                    callback.onError("HTTP错误: " + response.code() + " " + response.message());
                    return;
                }
                
                String responseBody = response.body().string();
                Log.d(TAG, "响应内容: " + responseBody);
                
                if (responseBody == null || responseBody.trim().isEmpty()) {
                    Log.e(TAG, "响应内容为空");
                    callback.onError("服务器响应为空");
                    return;
                }
                
                try {
                    JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
                    
                    if (!jsonObject.has("code")) {
                        Log.e(TAG, "响应中缺少code字段");
                        callback.onError("服务器响应格式错误：缺少code字段");
                        return;
                    }
                    
                    int code = jsonObject.get("code").getAsInt();
                    Log.d(TAG, "响应码: " + code);
                    
                    // 检查是否需要重新登录
                    if (code == 1101 || code == 1102) {
                        Log.w(TAG, "需要重新登录，清除token");
                        clearToken();
                        callback.onError("请重新登录");
                        return;
                    }
                    
                    callback.onSuccess(jsonObject);
                } catch (Exception e) {
                    Log.e(TAG, "解析响应失败 - 响应内容: " + responseBody, e);
                    callback.onError("解析响应失败: " + e.getMessage());
                }
            }
        });
    }
}