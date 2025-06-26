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

public class RegisterActivity extends AppCompatActivity {
    private EditText etUsername, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;
    private ApiService apiService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        
        initViews();
        initListeners();
        
        apiService = ApiService.getInstance(this);
    }
    
    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        tvLogin = findViewById(R.id.tv_login);
        progressBar = findViewById(R.id.progress_bar);
    }
    
    private void initListeners() {
        btnRegister.setOnClickListener(v -> register());
        tvLogin.setOnClickListener(v -> {
            finish();
        });
    }
    
    private void register() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("请输入用户名");
            return;
        }
        
        if (username.length() < 3) {
            etUsername.setError("用户名至少3个字符");
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("请输入密码");
            return;
        }
        
        if (password.length() < 6) {
            etPassword.setError("密码至少6个字符");
            return;
        }
        
        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("请确认密码");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("两次输入的密码不一致");
            return;
        }
        
        showLoading(true);
        
        apiService.register(username, password, "male", new ApiService.ApiCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject response) {
                String responseStr = response.toString();
                runOnUiThread(() -> {
                    showLoading(false);
                    try {
                        JSONObject jsonObject = new JSONObject(responseStr);
                        int code = jsonObject.optInt("code", -1);
                        
                        if (code == 1000) {
                            String msg = jsonObject.optString("msg", "注册成功，请登录");
                            Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            String message = jsonObject.optString("msg", "注册失败");
                            Toast.makeText(RegisterActivity.this, "注册失败：" + message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(RegisterActivity.this, "注册失败：数据解析错误", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(RegisterActivity.this, "注册失败：" + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!show);
        etUsername.setEnabled(!show);
        etPassword.setEnabled(!show);
        etConfirmPassword.setEnabled(!show);
    }
}