package com.aiclothes.app.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import androidx.core.content.FileProvider;
import android.text.TextUtils;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.aiclothes.app.R;
import com.aiclothes.app.network.ApiService;
import com.aiclothes.app.utils.ImageUtils;
import com.aiclothes.app.utils.PermissionUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.DataSource;
import android.graphics.drawable.Drawable;

import org.json.JSONObject;
import com.google.gson.JsonObject;
import io.noties.markwon.Markwon;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WardrobeFragment extends Fragment {
    private ImageView ivWardrobeImage, ivRecommendationImage, ivRecommendationDisplay;
    private Button btnSelectImage, btnTakePhoto, btnAnalyzeWardrobe, btnGenerateImage, btnGenerateRecommendation;
    private TextView tvAnalysisResult;
    private EditText tvPurposeInput;
    private ProgressBar progressBar;
    private LinearLayout llImageContainer;
    private CardView cardAnalysisResult, cardRecommendationImage;
    private ScrollView scrollView;
    
    private ApiService apiService;
    private List<File> selectedImageFiles;
    private String wardrobeAnalysisResult;
    private Markwon markwon;
    private StringBuilder analysisTextBuilder = new StringBuilder();
    private static final int MAX_IMAGES = 5;
    private File currentPhotoFile;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wardrobe, container, false);
        initViews(view);
        initData();
        initListeners();
        return view;
    }
    
    private void initViews(View view) {
        ivWardrobeImage = view.findViewById(R.id.iv_wardrobe_image);
        ivRecommendationImage = view.findViewById(R.id.iv_recommendation_image);
        ivRecommendationDisplay = view.findViewById(R.id.iv_recommendation_display);
        btnSelectImage = view.findViewById(R.id.btn_select_image);
        btnTakePhoto = view.findViewById(R.id.btn_take_photo);
        btnAnalyzeWardrobe = view.findViewById(R.id.btn_analyze_wardrobe);
        btnGenerateImage = view.findViewById(R.id.btn_generate_image);
        btnGenerateRecommendation = view.findViewById(R.id.btn_generate_recommendation);
        tvAnalysisResult = view.findViewById(R.id.tv_analysis_result);
        tvPurposeInput = view.findViewById(R.id.tv_purpose_input);
        progressBar = view.findViewById(R.id.progress_bar);
        llImageContainer = view.findViewById(R.id.ll_image_container);
        cardAnalysisResult = view.findViewById(R.id.card_analysis_result);
        cardRecommendationImage = view.findViewById(R.id.card_recommendation_image);
        scrollView = view.findViewById(R.id.scroll_view);
        
        // WebView相关功能已移除，使用TextView显示流式内容
    }
    
    private void initData() {
        apiService = ApiService.getInstance(getContext());
        selectedImageFiles = new ArrayList<>();
        
        // 初始化Markdown处理器
        markwon = Markwon.create(getContext());
        
        // 初始时隐藏分析结果卡片和推荐图片卡片
        if (cardAnalysisResult != null) {
            cardAnalysisResult.setVisibility(View.GONE);
        }
        if (cardRecommendationImage != null) {
            cardRecommendationImage.setVisibility(View.GONE);
        }
    }
    
    private void initListeners() {
        btnSelectImage.setOnClickListener(v -> {
            if (PermissionUtils.hasStoragePermission(getActivity())) {
                selectImageFromGallery();
            } else {
                PermissionUtils.requestPermissions(getActivity());
            }
        });
        
        btnTakePhoto.setOnClickListener(v -> {
            if (PermissionUtils.hasCameraPermission(getActivity()) && 
                PermissionUtils.hasStoragePermission(getActivity())) {
                takePhoto();
            } else {
                PermissionUtils.requestPermissions(getActivity());
            }
        });
        
        btnAnalyzeWardrobe.setOnClickListener(v -> analyzeWardrobe());
        btnGenerateImage.setOnClickListener(v -> generateRecommendationImage());
        btnGenerateRecommendation.setOnClickListener(v -> generateRecommendationImage());
    }
    
    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, ImageUtils.REQUEST_IMAGE_PICK);
    }
    
    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                currentPhotoFile = photoFile;
                Uri photoURI = FileProvider.getUriForFile(getActivity(),
                        getActivity().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, ImageUtils.REQUEST_IMAGE_CAPTURE);
            }
        } else {
            Toast.makeText(getActivity(), "没有找到相机应用", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ImageUtils.REQUEST_IMAGE_PICK && data != null) {
                // 处理多选图片
                if (data.getClipData() != null) {
                    // 多张图片
                    int count = data.getClipData().getItemCount();
                    int remainingSlots = MAX_IMAGES - selectedImageFiles.size();
                    
                    if (count > remainingSlots) {
                        Toast.makeText(getContext(), "最多只能上传" + MAX_IMAGES + "张图片，当前还可以选择" + remainingSlots + "张", Toast.LENGTH_LONG).show();
                        count = remainingSlots;
                    }
                    
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        File imageFile = ImageUtils.getFileFromUri(getContext(), imageUri);
                        
                        // 预检查文件大小
                        if (imageFile != null && !ImageUtils.isWardrobeFileSizeValid(imageFile)) {
                            String fileName = imageFile.getName();
                            String fileSize = ImageUtils.getFileSizeDescription(imageFile);
                            Toast.makeText(getContext(), "文件 " + fileName + " (" + fileSize + ") 超过10MB限制，已跳过", Toast.LENGTH_LONG).show();
                            continue;
                        }
                        
                        addSelectedImage(imageFile);
                    }
                } else if (data.getData() != null) {
                    // 单张图片
                    Uri selectedImageUri = data.getData();
                    File imageFile = ImageUtils.getFileFromUri(getContext(), selectedImageUri);
                    
                    // 预检查文件大小
                    if (imageFile != null && !ImageUtils.isWardrobeFileSizeValid(imageFile)) {
                        String fileName = imageFile.getName();
                        String fileSize = ImageUtils.getFileSizeDescription(imageFile);
                        Toast.makeText(getContext(), "文件 " + fileName + " (" + fileSize + ") 超过10MB限制", Toast.LENGTH_LONG).show();
                        return;
                    }
                    
                    addSelectedImage(imageFile);
                }
            } else if (requestCode == ImageUtils.REQUEST_IMAGE_CAPTURE) {
                if (currentPhotoFile != null) {
                    addSelectedImage(currentPhotoFile);
                }
            }
        }
    }
    
    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private void addSelectedImage(File imageFile) {
        if (imageFile == null || !imageFile.exists()) {
            Log.w("WardrobeFragment", "图片文件为空或不存在");
            return;
        }
        
        Log.d("WardrobeFragment", "尝试添加图片: " + imageFile.getName() + ", 当前已有: " + selectedImageFiles.size() + " 张");
        
        // 检查是否已达到最大图片数量
        if (selectedImageFiles.size() >= MAX_IMAGES) {
            Toast.makeText(getContext(), "最多只能上传" + MAX_IMAGES + "张图片", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查图片格式和大小
        if (!ImageUtils.isImageFormatValid(imageFile.getName())) {
            Toast.makeText(getContext(), "不支持的图片格式，仅支持.jpg、.png、.jpeg", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!ImageUtils.isWardrobeFileSizeValid(imageFile)) {
            Toast.makeText(getContext(), "图片大小不符合要求（10MB以下）", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 添加到列表
        selectedImageFiles.add(imageFile);
        Log.d("WardrobeFragment", "成功添加图片: " + imageFile.getName() + ", 总数: " + selectedImageFiles.size());
        displaySelectedImages();
    }
    
    private void displaySelectedImages() {
        // 清空容器
        llImageContainer.removeAllViews();
        
        // 显示所有选中的图片
        for (int i = 0; i < selectedImageFiles.size(); i++) {
            File imageFile = selectedImageFiles.get(i);
            final int index = i;
            
            // 创建图片容器
            LinearLayout imageLayout = new LinearLayout(getContext());
            imageLayout.setOrientation(LinearLayout.VERTICAL);
            imageLayout.setPadding(8, 8, 8, 8);
            
            // 创建ImageView
            ImageView imageView = new ImageView(getContext());
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            imageView.setLayoutParams(imageParams);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setAdjustViewBounds(true);
            imageView.setMaxHeight(400); // 设置最大高度避免图片过大
            imageView.setBackgroundColor(0xFFF0F0F0);
            
            // 加载图片
            Glide.with(this)
                    .load(imageFile)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_dialog_alert)
                    .into(imageView);
            
            // 创建删除按钮
            Button deleteBtn = new Button(getContext());
            deleteBtn.setText("删除图片" + (i + 1));
            deleteBtn.setOnClickListener(v -> {
                selectedImageFiles.remove(index);
                displaySelectedImages();
            });
            
            imageLayout.addView(imageView);
            imageLayout.addView(deleteBtn);
            llImageContainer.addView(imageLayout);
        }
        
        // 更新按钮状态
        btnAnalyzeWardrobe.setEnabled(!selectedImageFiles.isEmpty());
        
        // 重置分析结果和推荐图片
        tvAnalysisResult.setText("");
        ivRecommendationImage.setVisibility(View.GONE);
        cardRecommendationImage.setVisibility(View.GONE);
        btnGenerateImage.setEnabled(false);
        btnGenerateRecommendation.setEnabled(false);
    }
    

    


    private void analyzeWardrobe() {
        String purpose = tvPurposeInput.getText().toString().trim();
        if (TextUtils.isEmpty(purpose)) {
            Toast.makeText(getContext(), "请输入穿衣目的", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedImageFiles.isEmpty()) {
            Toast.makeText(getContext(), "请选择至少一张衣柜照片", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 先上传图片，然后进行分析
        uploadPhotosAndAnalyze(purpose);
    }
    
    private void uploadPhotosAndAnalyze(String purpose) {
        // 显示上传提示
        Log.d("WardrobeFragment", "准备上传 " + selectedImageFiles.size() + " 张图片");
        Toast.makeText(getContext(), "正在上传 " + selectedImageFiles.size() + " 张图片...", Toast.LENGTH_SHORT).show();
        
        // 打印每张图片的信息
        for (int i = 0; i < selectedImageFiles.size(); i++) {
            File file = selectedImageFiles.get(i);
            Log.d("WardrobeFragment", "图片 " + (i+1) + ": " + file.getName() + ", 大小: " + file.length() + " bytes");
        }
        
        // 上传衣柜照片
        apiService.uploadWardrobePhotos(selectedImageFiles, new ApiService.ApiCallback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject response) {
                Log.d("WardrobeFragment", "图片上传成功: " + response.toString());
                // 上传成功后进行分析
                parseWardrobe(purpose);
            }
            
            @Override
            public void onError(String error) {
                Log.e("WardrobeFragment", "图片上传失败: " + error);
                Toast.makeText(getContext(), "图片上传失败: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void parseWardrobe(String purpose) {
        // 清空之前的分析结果
        analysisTextBuilder.setLength(0);
        tvAnalysisResult.setText("");
        
        // 显示等待提示
        Toast.makeText(getContext(), "衣柜分析中，时间可能较长，请耐心等待，不要离开...", Toast.LENGTH_LONG).show();
        
        // 显示分析结果卡片
        if (cardAnalysisResult != null) {
            cardAnalysisResult.setVisibility(View.VISIBLE);
        }
        
        // 显示TextView
        if (tvAnalysisResult != null) {
            tvAnalysisResult.setVisibility(View.VISIBLE);
        }
        
        // 使用原生Android流式处理
        parseWardrobeWithOriginalMethod(purpose);
    }
    

    
    private void parseWardrobeWithOriginalMethod(String purpose) {
        apiService.parseWardrobeAndRecommend(purpose, new ApiService.StreamCallback() {
            @Override
            public void onData(String data) {
                Log.d("WardrobeFragment", "收到流式数据: " + data);
                if (getActivity() != null && isAdded()) {
                    // 累积流式数据
                    analysisTextBuilder.append(data);
                    
                    // 将Markdown格式转换为富文本并显示
                    String markdownText = analysisTextBuilder.toString();
                    Log.d("WardrobeFragment", "当前累积文本长度: " + markdownText.length());
                    markwon.setMarkdown(tvAnalysisResult, markdownText);
                    
                    // 自动滚动到底部显示最新内容
                    scrollToBottom();
                } else {
                    Log.w("WardrobeFragment", "Fragment未添加或Activity为空，跳过数据更新");
                }
            }
            
            @Override
            public void onComplete() {
                Log.d("WardrobeFragment", "流式响应完成");
                if (getActivity() != null && isAdded()) {
                    showLoading(false);
                    wardrobeAnalysisResult = analysisTextBuilder.toString();
                    Log.d("WardrobeFragment", "最终分析结果长度: " + wardrobeAnalysisResult.length());
                    btnGenerateImage.setEnabled(true);
                    btnGenerateRecommendation.setEnabled(true);
                    
                    // 最终渲染完整的Markdown内容
                    markwon.setMarkdown(tvAnalysisResult, wardrobeAnalysisResult);
                } else {
                    Log.w("WardrobeFragment", "onComplete: Fragment未添加或Activity为空");
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e("WardrobeFragment", "流式响应错误: " + error);
                if (getActivity() != null && isAdded()) {
                    showLoading(false);
                    showError("分析失败：" + error);
                } else {
                    Log.w("WardrobeFragment", "onError: Fragment未添加或Activity为空");
                }
            }
         });
    }
    
    // 滚动到底部显示最新内容
    private void scrollToBottom() {
        if (scrollView != null) {
            scrollView.post(() -> {
                // 平滑滚动到ScrollView的底部
                scrollView.smoothScrollTo(0, scrollView.getChildAt(0).getHeight());
            });
        }
        
        // 保留TextView的滚动逻辑作为备用
        if (tvAnalysisResult != null) {
            tvAnalysisResult.post(() -> {
                // 检查Layout是否已经创建，避免空指针异常
                if (tvAnalysisResult.getLayout() != null) {
                    int scrollAmount = tvAnalysisResult.getLayout().getLineTop(tvAnalysisResult.getLineCount()) - tvAnalysisResult.getHeight();
                    if (scrollAmount > 0) {
                        tvAnalysisResult.scrollTo(0, scrollAmount);
                    } else {
                        tvAnalysisResult.scrollTo(0, 0);
                    }
                }
            });
        }
    }
    
    private void generateRecommendationImage() {
        if (TextUtils.isEmpty(wardrobeAnalysisResult)) {
            Toast.makeText(getContext(), "请先分析衣柜", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showLoading(true);
        
        // 显示等待提示
        Toast.makeText(getContext(), "推荐图片生成中，时间可能较长，请耐心等待，不要离开...", Toast.LENGTH_LONG).show();
        
        apiService.generateWardrobeImage(new ApiService.ApiCallback<JsonObject>() {
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
                                // 显示推荐图片卡片
                                cardRecommendationImage.setVisibility(View.VISIBLE);
                                
                                // 使用Glide加载图片到专用的推荐图片显示区域
                                Glide.with(WardrobeFragment.this)
                                        .load(imageUrl)
                                        .placeholder(android.R.drawable.ic_menu_gallery)
                                        .error(android.R.drawable.ic_dialog_alert)
                                        .listener(new RequestListener<Drawable>() {
                                            @Override
                                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                                return false;
                                            }
                                            
                                            @Override
                                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                                // 图片加载完成后自动滚动到底部
                                                scrollToBottom();
                                                return false;
                                            }
                                        })
                                        .into(ivRecommendationDisplay);
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
                showError("生成图片失败：" + error);
            }
        });
    }
    
    private void showError(String errorMessage) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                showLoading(false);
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtils.REQUEST_PERMISSIONS) {
            if (PermissionUtils.handlePermissionResult(requestCode, permissions, grantResults)) {
                Toast.makeText(getContext(), "权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "需要相机和存储权限才能使用此功能", Toast.LENGTH_SHORT).show();
            }
        }
    }
}