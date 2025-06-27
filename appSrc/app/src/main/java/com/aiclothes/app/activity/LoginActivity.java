package com.aiclothes.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.aiclothes.app.R;
import com.aiclothes.app.network.ApiService;

import org.json.JSONObject;
import com.google.gson.JsonObject;

public class LoginActivity extends AppCompatActivity {
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private ProgressBar progressBar;
    private ApiService apiService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        initViews();
        initListeners();
        
        apiService = ApiService.getInstance(this);
    }
    
    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);
        // 移除忘记密码功能
        progressBar = findViewById(R.id.progress_bar);
    }
    
    private void initListeners() {
        btnLogin.setOnClickListener(v -> login());
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
        // 忘记密码功能已移除
    }
    
    private void login() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("请输入手机号");
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("请输入密码");
            return;
        }
        
        showLoading(true);
        
        apiService.login(username, password, new ApiService.ApiCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject response) {
                runOnUiThread(() -> {
                    showLoading(false);
                    try {
                        String responseStr = response.toString();
                        JSONObject jsonObject = new JSONObject(responseStr);
                        int code = jsonObject.optInt("code", -1);
                        
                        if (code == 1000) {
                            String token = jsonObject.optString("token");
                            if (!TextUtils.isEmpty(token)) {
                                // 保存token和用户名到SharedPreferences
                                getSharedPreferences("app_prefs", MODE_PRIVATE)
                                    .edit()
                                    .putString("auth_token", token)
                                    .putString("username", username)
                                    .apply();
                                
                                String msg = jsonObject.optString("msg", "登录成功");
                                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                                
                                // 跳转到主页
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, "登录失败：未获取到token", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // 根据接口文档处理不同的错误码
                            String message;
                            switch (code) {
                                case 1022:
                                    message = "该手机号不存在";
                                    break;
                                case 1023:
                                    message = "密码错误";
                                    break;
                                case 1021:
                                case 1024:
                                    message = "登录账号失败，请稍后再试";
                                    break;
                                default:
                                    message = jsonObject.optString("msg", "登录失败");
                                    break;
                            }
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(LoginActivity.this, "登录失败：数据解析错误", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, "登录失败：" + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        etUsername.setEnabled(!show);
        etPassword.setEnabled(!show);
    }
}