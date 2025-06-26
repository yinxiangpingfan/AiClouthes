package com.aiclothes.app.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStream;
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
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .cache(null) // 禁用缓存以支持流式响应
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
    
    // 处理SSE格式的单行数据
    private void processSSELine(String line, StreamCallback callback) {
        Log.d(TAG, "处理SSE行: " + line);
        
        // 处理SSE格式：event: message 和 data: {"data": "..."}
        if (line.startsWith("data: ")) {
            String jsonData = line.substring(6).trim();
            Log.d(TAG, "提取的JSON数据: " + jsonData);
            
            if (!jsonData.isEmpty()) {
                // 检查是否为结束标记
                if (jsonData.equals("[DONE]")) {
                    Log.d(TAG, "收到流式结束标记");
                    return;
                }
                
                try {
                    JsonObject json = JsonParser.parseString(jsonData).getAsJsonObject();
                    if (json.has("data")) {
                        String dataContent = json.get("data").getAsString();
                        Log.d(TAG, "解析到数据内容: " + dataContent);
                        
                        // 立即切换到主线程回调，实现真正的流式输出
                        new Handler(Looper.getMainLooper()).post(() -> {
                            callback.onData(dataContent);
                        });
                    } else {
                        Log.w(TAG, "JSON中没有data字段: " + jsonData);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "JSON解析错误，跳过此行: " + jsonData, e);
                    // 忽略JSON解析错误，继续处理下一行
                }
            }
        } else if (line.startsWith("event: ")) {
            Log.d(TAG, "收到事件行: " + line);
        } else if (line.trim().isEmpty()) {
            Log.d(TAG, "收到空行");
        } else {
            Log.d(TAG, "收到其他格式行: " + line);
        }
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
        Log.d(TAG, "开始解析衣橱并推荐 - 目的: " + purpose);
        
        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("ques", purpose)
                .build();
        
        Request request = createAuthenticatedRequest()
                .url(BASE_URL + "/user/pic/parse")
                .post(formBody)
                .addHeader("Accept", "text/event-stream")
                .addHeader("Cache-Control", "no-cache")
                .addHeader("Connection", "keep-alive")
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "流式请求失败: " + e.getMessage(), e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onError("网络请求失败: " + e.getMessage());
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "流式响应状态码: " + response.code());
                
                if (!response.isSuccessful()) {
                    Log.e(TAG, "流式响应HTTP错误: " + response.code());
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callback.onError("HTTP错误: " + response.code() + " " + response.message());
                    });
                    return;
                }
                
                // 检查Content-Type是否为text/event-stream
                String contentType = response.header("Content-Type");
                Log.d(TAG, "响应Content-Type: " + contentType);
                
                // 处理Server-Sent Events流式响应 - 仿照前端实现
                InputStream inputStream = null;
                try {
                    inputStream = response.body().byteStream();
                    byte[] buffer = new byte[1024];
                    StringBuilder dataBuffer = new StringBuilder();
                    
                    while (true) {
                        int bytesRead = inputStream.read(buffer);
                        if (bytesRead == -1) {
                            Log.d(TAG, "流式响应读取完成");
                            break;
                        }
                        
                        // 将读取的字节转换为字符串
                        String chunk = new String(buffer, 0, bytesRead, "UTF-8");
                        Log.d(TAG, "收到数据块: " + chunk);
                        
                        dataBuffer.append(chunk);
                        
                        // 按行处理数据
                        String[] lines = dataBuffer.toString().split("\n");
                        
                        // 保留最后一行（可能不完整）
                        if (lines.length > 0) {
                            dataBuffer = new StringBuilder();
                            String lastLine = lines[lines.length - 1];
                            
                            // 如果最后一行不以换行符结尾，说明可能不完整
                            if (!chunk.endsWith("\n")) {
                                dataBuffer.append(lastLine);
                                // 处理除最后一行外的所有行
                                for (int i = 0; i < lines.length - 1; i++) {
                                    processSSELine(lines[i], callback);
                                }
                            } else {
                                // 处理所有行
                                for (String line : lines) {
                                    processSSELine(line, callback);
                                }
                            }
                        }
                    }
                    
                    // 处理缓冲区中剩余的数据
                    if (dataBuffer.length() > 0) {
                        processSSELine(dataBuffer.toString(), callback);
                    }
                    
                    Log.d(TAG, "流式响应完成");
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callback.onComplete();
                    });
                    
                } catch (Exception e) {
                    Log.e(TAG, "流式响应处理失败", e);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callback.onError("流式响应处理失败: " + e.getMessage());
                    });
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            Log.w(TAG, "关闭inputStream失败", e);
                        }
                    }
                    if (response.body() != null) {
                        response.body().close();
                    }
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
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onError("网络请求失败: " + e.getMessage());
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "收到响应 - 状态码: " + response.code() + ", URL: " + request.url());
                
                if (!response.isSuccessful()) {
                    Log.e(TAG, "HTTP错误 - 状态码: " + response.code() + ", 消息: " + response.message());
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callback.onError("HTTP错误: " + response.code() + " " + response.message());
                    });
                    return;
                }
                
                String responseBody = response.body().string();
                Log.d(TAG, "响应内容: " + responseBody);
                
                if (responseBody == null || responseBody.trim().isEmpty()) {
                    Log.e(TAG, "响应内容为空");
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callback.onError("服务器响应为空");
                    });
                    return;
                }
                
                try {
                    JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
                    
                    if (!jsonObject.has("code")) {
                        Log.e(TAG, "响应中缺少code字段");
                        new Handler(Looper.getMainLooper()).post(() -> {
                            callback.onError("服务器响应格式错误：缺少code字段");
                        });
                        return;
                    }
                    
                    int code = jsonObject.get("code").getAsInt();
                    Log.d(TAG, "响应码: " + code);
                    
                    // 检查是否需要重新登录
                    if (code == 1101 || code == 1102) {
                        Log.w(TAG, "需要重新登录，清除token");
                        clearToken();
                        new Handler(Looper.getMainLooper()).post(() -> {
                            callback.onError("请重新登录");
                        });
                        return;
                    }
                    
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callback.onSuccess(jsonObject);
                    });
                } catch (Exception e) {
                    Log.e(TAG, "解析响应失败 - 响应内容: " + responseBody, e);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callback.onError("解析响应失败: " + e.getMessage());
                    });
                }
            }
        });
    }
}