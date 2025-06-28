package com.aiclothes.app.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonObject;
import com.aiclothes.app.R;
import com.aiclothes.app.activity.LoginActivity;
import android.content.Intent;
import com.aiclothes.app.network.ApiService;
import com.aiclothes.app.utils.CacheManager;

import org.json.JSONObject;

public class ProfileFragment extends Fragment {
    private TextView tvUsername, tvCacheSize;
    private CardView cardChangePassword, cardLogout, cardAbout, cardCacheManagement;
    private ProgressBar progressBar;
    
    private ApiService apiService;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        initViews(view);
        initData();
        initListeners();
        return view;
    }
    
    private void initViews(View view) {
        tvUsername = view.findViewById(R.id.tv_username);
        tvCacheSize = view.findViewById(R.id.tv_cache_size);
        cardChangePassword = view.findViewById(R.id.card_change_password);
        cardLogout = view.findViewById(R.id.card_logout);
        cardAbout = view.findViewById(R.id.card_about);
        cardCacheManagement = view.findViewById(R.id.card_cache_management);
        progressBar = view.findViewById(R.id.progress_bar);
    }
    
    private void initData() {
        apiService = ApiService.getInstance(getContext());
        
        // 从SharedPreferences获取用户名（电话号）
        String username = getContext().getSharedPreferences("app_prefs", getContext().MODE_PRIVATE)
                .getString("username", "用户");
        tvUsername.setText(username);
        
        // 更新缓存大小显示
        updateCacheSize();
    }
    
    private void initListeners() {
        cardChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        cardLogout.setOnClickListener(v -> showLogoutDialog());
        cardAbout.setOnClickListener(v -> showAboutDialog());
        cardCacheManagement.setOnClickListener(v -> showCacheManagementDialog());
    }
    
    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, null);
        EditText etNewPassword = dialogView.findViewById(R.id.et_new_password);
        EditText etConfirmPassword = dialogView.findViewById(R.id.et_confirm_password);
        
        // 原密码输入框已从布局中移除，接口不需要原密码
        
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("修改密码")
                .setView(dialogView)
                .setPositiveButton("确认", null)
                .setNegativeButton("取消", null)
                .create();
        
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String newPassword = etNewPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();
                
                if (TextUtils.isEmpty(newPassword)) {
                    etNewPassword.setError("请输入新密码");
                    return;
                }
                
                if (newPassword.length() < 6) {
                    etNewPassword.setError("密码至少6个字符");
                    return;
                }
                
                if (TextUtils.isEmpty(confirmPassword)) {
                    etConfirmPassword.setError("请确认新密码");
                    return;
                }
                
                if (!newPassword.equals(confirmPassword)) {
                    etConfirmPassword.setError("两次输入的密码不一致");
                    return;
                }
                
                changePassword(newPassword, dialog);
            });
        });
        
        dialog.show();
    }
    
    private void changePassword(String newPassword, AlertDialog dialog) {
        showLoading(true);
        
        apiService.changePassword(newPassword, new ApiService.ApiCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject response) {
                String responseStr = response.toString();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        try {
                            JSONObject jsonObject = new JSONObject(responseStr);
                            int code = jsonObject.optInt("code", -1);
                            String msg = jsonObject.optString("msg", "");
                            
                            switch (code) {
                                case 1000:
                                    // 修改密码成功
                                    Toast.makeText(getContext(), msg.isEmpty() ? "修改密码成功" : msg, Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    break;
                                case 1031:
                                case 1032:
                                case 1034:
                                case 1035:
                                    // 修改密码失败，请稍后重试
                                    Toast.makeText(getContext(), msg.isEmpty() ? "修改密码失败，请稍后重试" : msg, Toast.LENGTH_SHORT).show();
                                    break;
                                case 1033:
                                    // 新密码与旧密码相同
                                    Toast.makeText(getContext(), msg.isEmpty() ? "新密码与旧密码相同" : msg, Toast.LENGTH_SHORT).show();
                                    break;
                                case 1101:
                                case 1102:
                                    // 登录过期处理已由ApiService统一处理
                                    // 这里不需要额外处理，ApiService会自动跳转到登录页面
                                    break;
                                default:
                                    // 其他错误
                                    Toast.makeText(getContext(), msg.isEmpty() ? "修改密码失败" : msg, Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "密码修改失败：数据解析错误", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(getContext(), "网络错误：" + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    
    private void showLogoutDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("退出登录")
                .setMessage("确定要退出登录吗？")
                .setPositiveButton("确定", (dialog, which) -> logout())
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void logout() {
        showLoading(true);
        
        apiService.logout(new ApiService.ApiCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        // 清除本地token
                        apiService.clearToken();
                        
                        Toast.makeText(getContext(), "已退出登录", Toast.LENGTH_SHORT).show();
                        
                        // 跳转到登录页面
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        getActivity().finish();
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        // 即使退出登录失败，也清除本地token
                        apiService.clearToken();
                        
                        Toast.makeText(getContext(), "已退出登录", Toast.LENGTH_SHORT).show();
                        
                        // 跳转到登录页面
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        getActivity().finish();
                    });
                }
            }
        });
    }
    
    private void showAboutDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("关于应用")
                .setMessage("AI智能穿搭助手\n\n功能特色：\n• AI根据天气推荐穿搭\n• 智能识别衣柜并推荐搭配\n• AI试衣功能\n• 个性化穿搭建议\n\n版本：1.0.0")
                .setPositiveButton("确定", null)
                .show();
    }
    
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    
    private void updateCacheSize() {
        new Thread(() -> {
            String cacheInfo = CacheManager.getCacheSizeInfo(getContext());
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (tvCacheSize != null) {
                        tvCacheSize.setText(cacheInfo);
                    }
                });
            }
        }).start();
    }
    
    private void showCacheManagementDialog() {
        String[] options = {"清理衣橱缓存", "清理试衣缓存", "清理所有缓存", "查看缓存详情"};
        
        new AlertDialog.Builder(getContext())
                .setTitle("缓存管理")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            clearWardrobeCache();
                            break;
                        case 1:
                            clearTryOnCache();
                            break;
                        case 2:
                            clearAllCache();
                            break;
                        case 3:
                            showCacheDetails();
                            break;
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void clearWardrobeCache() {
        new AlertDialog.Builder(getContext())
                .setTitle("清理衣橱缓存")
                .setMessage("确定要清理衣橱相关的缓存图片吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    showLoading(true);
                    new Thread(() -> {
                        CacheManager.clearWardrobeCache(getContext());
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                showLoading(false);
                                Toast.makeText(getContext(), "衣橱缓存已清理", Toast.LENGTH_SHORT).show();
                                updateCacheSize();
                            });
                        }
                    }).start();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void clearTryOnCache() {
        new AlertDialog.Builder(getContext())
                .setTitle("清理试衣缓存")
                .setMessage("确定要清理试衣相关的缓存图片吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    showLoading(true);
                    new Thread(() -> {
                        CacheManager.clearTryOnCache(getContext(), false);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                showLoading(false);
                                Toast.makeText(getContext(), "试衣缓存已清理", Toast.LENGTH_SHORT).show();
                                updateCacheSize();
                            });
                        }
                    }).start();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void clearAllCache() {
        new AlertDialog.Builder(getContext())
                .setTitle("清理所有缓存")
                .setMessage("确定要清理所有缓存数据吗？这将释放更多存储空间。")
                .setPositiveButton("确定", (dialog, which) -> {
                    showLoading(true);
                    new Thread(() -> {
                        CacheManager.clearAllImageCache(getContext());
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                showLoading(false);
                                Toast.makeText(getContext(), "所有缓存已清理", Toast.LENGTH_SHORT).show();
                                updateCacheSize();
                            });
                        }
                    }).start();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void showCacheDetails() {
        showLoading(true);
        new Thread(() -> {
            String details = CacheManager.getCacheSizeInfo(getContext());
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    new AlertDialog.Builder(getContext())
                            .setTitle("缓存详情")
                            .setMessage(details)
                            .setPositiveButton("确定", null)
                            .show();
                });
            }
        }).start();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 页面恢复时更新缓存大小
        updateCacheSize();
    }
}