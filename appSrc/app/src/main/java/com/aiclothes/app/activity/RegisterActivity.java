package com.aiclothes.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
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
    private RadioGroup rgGender;
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
        rgGender = findViewById(R.id.rg_gender);
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
            etUsername.setError("请输入手机号");
            return;
        }
        
        if (username.length() < 11) {
            etUsername.setError("请输入正确的手机号");
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
        
        // 获取选择的性别
        String gender = "male"; // 默认值
        int checkedId = rgGender.getCheckedRadioButtonId();
        if (checkedId == R.id.rb_female) {
            gender = "female";
        }
        
        showLoading(true);
        
        apiService.register(username, password, gender, new ApiService.ApiCallback<JsonObject>() {
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
                            // 根据接口文档处理不同的错误码
                            String message;
                            switch (code) {
                                case 1012:
                                    message = "该手机号已被注册";
                                    break;
                                case 1016:
                                    message = "性别参数错误";
                                    break;
                                case 1011:
                                case 1013:
                                case 1014:
                                case 1015:
                                    message = "注册账号失败，请稍后再试";
                                    break;
                                default:
                                    message = jsonObject.optString("msg", "注册失败");
                                    break;
                            }
                            Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
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
        rgGender.setEnabled(!show);
    }
}