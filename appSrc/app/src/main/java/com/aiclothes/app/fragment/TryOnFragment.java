package com.aiclothes.app.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;

import com.google.gson.JsonObject;
import com.aiclothes.app.R;
import com.aiclothes.app.network.ApiService;
import com.aiclothes.app.utils.ImageUtils;
import com.aiclothes.app.utils.PermissionUtils;
import com.bumptech.glide.Glide;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TryOnFragment extends Fragment {
    private RadioGroup rgTryOnMode;
    private RadioButton rbWithModel, rbWithoutModel;
    private RadioGroup rgClothingType;
    private RadioButton rbSingleOutfit, rbSeparateClothes;
    private RadioGroup rgGender;
    private RadioButton rbMale, rbFemale;
    
    private ImageView ivModelImage, ivClothingImage, ivTopImage, ivBottomImage, ivResultImage;
    private Button btnSelectModel, btnSelectClothing, btnSelectTop, btnSelectBottom, btnStartTryOn;
    private TextView tvInstructions;
    private ProgressBar progressBar;
    private CardView cardModelSelection;
    private CardView cardGenderSelection;
    private CardView cardResult;
    
    private ApiService apiService;
    private File modelImageFile;
    private File clothingImageFile;
    private File topImageFile;
    private File bottomImageFile;
    private File currentPhotoFile; // 当前拍照的临时文件
    
    private int currentImageSelection = 0; // 0: model, 1: clothing/top, 2: bottom
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_try_on, container, false);
        initViews(view);
        initData();
        initListeners();
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 确保视图完全创建后再更新UI
        view.post(() -> {
            updateUI();
            updateImageVisibility();
            updateTryOnButton();
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 确保图片在Fragment重新激活时正确显示
        updateImageVisibility();
        updateTryOnButton();
    }
    
    private LinearLayout layoutSingleOutfit, layoutSeparateOutfit;
    
    private void initViews(View view) {
        rgTryOnMode = view.findViewById(R.id.rg_try_on_mode);
        rbWithModel = view.findViewById(R.id.rb_with_model);
        rbWithoutModel = view.findViewById(R.id.rb_without_model);
        
        rgClothingType = view.findViewById(R.id.rg_clothing_type);
        rbSingleOutfit = view.findViewById(R.id.rb_single_outfit);
        rbSeparateClothes = view.findViewById(R.id.rb_separate_clothes);
        
        rgGender = view.findViewById(R.id.rg_gender);
        rbMale = view.findViewById(R.id.rb_male);
        rbFemale = view.findViewById(R.id.rb_female);
        
        ivModelImage = view.findViewById(R.id.iv_model_image);
        ivClothingImage = view.findViewById(R.id.iv_clothing_image);
        ivTopImage = view.findViewById(R.id.iv_top_image);
        ivBottomImage = view.findViewById(R.id.iv_bottom_image);
        ivResultImage = view.findViewById(R.id.iv_result_image);
        
        btnSelectModel = view.findViewById(R.id.btn_select_model);
        btnSelectClothing = view.findViewById(R.id.btn_select_clothing);
        btnSelectTop = view.findViewById(R.id.btn_select_top);
        btnSelectBottom = view.findViewById(R.id.btn_select_bottom);
        btnStartTryOn = view.findViewById(R.id.btn_start_try_on);
        
        tvInstructions = view.findViewById(R.id.tv_instructions);
        progressBar = view.findViewById(R.id.progress_bar);
        
        // 获取模特照片选择卡片和性别选择卡片
        cardModelSelection = view.findViewById(R.id.card_model_selection);
        cardGenderSelection = view.findViewById(R.id.card_gender_selection);
        cardResult = view.findViewById(R.id.card_result);
        
        // 获取服装选择布局容器
        layoutSingleOutfit = view.findViewById(R.id.layout_single_outfit);
        layoutSeparateOutfit = view.findViewById(R.id.layout_separate_outfit);
    }
    
    private void initData() {
        apiService = ApiService.getInstance(getContext());
    }
    
    private void initListeners() {
        rgTryOnMode.setOnCheckedChangeListener((group, checkedId) -> updateUI());
        rgClothingType.setOnCheckedChangeListener((group, checkedId) -> updateUI());
        
        btnSelectModel.setOnClickListener(v -> {
            currentImageSelection = 0;
            selectImage();
        });
        
        btnSelectClothing.setOnClickListener(v -> {
            currentImageSelection = 1;
            selectImage();
        });
        
        btnSelectTop.setOnClickListener(v -> {
            currentImageSelection = 1;
            selectImage();
        });
        
        btnSelectBottom.setOnClickListener(v -> {
            currentImageSelection = 2;
            selectImage();
        });
        
        btnStartTryOn.setOnClickListener(v -> startTryOn());
    }
    
    private void updateUI() {
        boolean withModel = rbWithModel.isChecked();
        boolean singleOutfit = rbSingleOutfit.isChecked();
        
        // 有模特试衣：显示模特照片上传卡片，隐藏性别选择卡片
        // 无模特试衣：隐藏模特照片上传卡片，显示性别选择卡片
        cardModelSelection.setVisibility(withModel ? View.VISIBLE : View.GONE);
        cardGenderSelection.setVisibility(withModel ? View.GONE : View.VISIBLE);
        
        // 更新服装选择UI - 控制布局容器的可见性
        if (singleOutfit) {
            layoutSingleOutfit.setVisibility(View.VISIBLE);
            layoutSeparateOutfit.setVisibility(View.GONE);
        } else {
            layoutSingleOutfit.setVisibility(View.GONE);
            layoutSeparateOutfit.setVisibility(View.VISIBLE);
        }
        
        // 保持已选择图片的可见性
        updateImageVisibility();
        
        // 更新说明文字
        updateInstructions();
        
        // 检查是否可以开始试衣
        updateTryOnButton();
    }
    
    private void updateInstructions() {
        boolean withModel = rbWithModel.isChecked();
        boolean singleOutfit = rbSingleOutfit.isChecked();
        
        String instructions;
        if (withModel) {
            if (singleOutfit) {
                instructions = getString(R.string.instruction_with_model_single);
            } else {
                instructions = getString(R.string.instruction_with_model_double);
            }
        } else {
            if (singleOutfit) {
                instructions = getString(R.string.instruction_without_model_single);
            } else {
                instructions = getString(R.string.instruction_without_model_double);
            }
        }
        
        tvInstructions.setText(instructions);
    }
    
    private void updateImageVisibility() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                try {
                    // 根据已选择的图片文件更新ImageView的可见性和图片内容
                    if (modelImageFile != null && getContext() != null && ivModelImage != null) {
                        ivModelImage.setVisibility(View.VISIBLE);
                        Glide.with(this)
                                .load(modelImageFile)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .error(android.R.drawable.ic_dialog_alert)
                                .into(ivModelImage);
                    } else if (ivModelImage != null) {
                        ivModelImage.setVisibility(View.GONE);
                    }
                    
                    if (clothingImageFile != null && getContext() != null && ivClothingImage != null) {
                        ivClothingImage.setVisibility(View.VISIBLE);
                        Glide.with(this)
                                .load(clothingImageFile)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .error(android.R.drawable.ic_dialog_alert)
                                .into(ivClothingImage);
                    } else if (ivClothingImage != null) {
                        ivClothingImage.setVisibility(View.GONE);
                    }
                    
                    if (topImageFile != null && getContext() != null && ivTopImage != null) {
                        ivTopImage.setVisibility(View.VISIBLE);
                        Glide.with(this)
                                .load(topImageFile)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .error(android.R.drawable.ic_dialog_alert)
                                .into(ivTopImage);
                    } else if (ivTopImage != null) {
                        ivTopImage.setVisibility(View.GONE);
                    }
                    
                    if (bottomImageFile != null && getContext() != null && ivBottomImage != null) {
                        ivBottomImage.setVisibility(View.VISIBLE);
                        Glide.with(this)
                                .load(bottomImageFile)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .error(android.R.drawable.ic_dialog_alert)
                                .into(ivBottomImage);
                    } else if (ivBottomImage != null) {
                        ivBottomImage.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
    
    private void updateTryOnButton() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                try {
                    boolean withModel = rbWithModel.isChecked();
                    boolean singleOutfit = rbSingleOutfit.isChecked();
                    
                    boolean canTryOn = false;
                    
                    if (withModel) {
                        if (singleOutfit) {
                            canTryOn = modelImageFile != null && clothingImageFile != null;
                        } else {
                            canTryOn = modelImageFile != null && topImageFile != null && bottomImageFile != null;
                        }
                    } else {
                        if (singleOutfit) {
                            canTryOn = clothingImageFile != null;
                        } else {
                            canTryOn = topImageFile != null && bottomImageFile != null;
                        }
                    }
                    
                    // 确保按钮存在且可以更新
                    if (btnStartTryOn != null) {
                        btnStartTryOn.setEnabled(canTryOn);
                        
                        // 更新按钮样式以提供视觉反馈
                        if (canTryOn) {
                            btnStartTryOn.setAlpha(1.0f);
                        } else {
                            btnStartTryOn.setAlpha(0.5f);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
    
    private void selectImage() {
        if (!PermissionUtils.hasStoragePermission(getActivity()) || !PermissionUtils.hasCameraPermission(getActivity())) {
            PermissionUtils.requestPermissions(getActivity());
            return;
        }
        
        showImageSourceDialog();
    }
    
    private void showImageSourceDialog() {
        // 显示选择对话框：相册或拍照
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("选择图片来源");
        builder.setItems(new String[]{"从相册选择", "拍照"}, (dialog, which) -> {
            switch (which) {
                case 0:
                    selectImageFromGallery();
                    break;
                case 1:
                    takePhoto();
                    break;
            }
        });
        builder.show();
    }
    
    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
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
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        android.util.Log.d("TryOnFragment", "onActivityResult - requestCode: " + requestCode + ", resultCode: " + resultCode);
        
        if (resultCode != Activity.RESULT_OK) {
            android.util.Log.w("TryOnFragment", "Activity result not OK, resultCode: " + resultCode);
            return;
        }
        
        File imageFile = null;
        
        if (requestCode == ImageUtils.REQUEST_IMAGE_PICK) {
            android.util.Log.d("TryOnFragment", "处理相册选择结果");
            if (data != null && data.getData() != null) {
                Uri imageUri = data.getData();
                android.util.Log.d("TryOnFragment", "选择的图片URI: " + imageUri.toString());
                imageFile = ImageUtils.getFileFromUri(getContext(), imageUri);
                if (imageFile != null) {
                    android.util.Log.d("TryOnFragment", "从URI获取的文件路径: " + imageFile.getAbsolutePath());
                } else {
                    android.util.Log.e("TryOnFragment", "从URI获取文件失败");
                }
            } else {
                android.util.Log.e("TryOnFragment", "相册选择数据为空");
            }
        } else if (requestCode == ImageUtils.REQUEST_IMAGE_CAPTURE) {
            android.util.Log.d("TryOnFragment", "处理拍照结果");
            imageFile = currentPhotoFile; // 使用我们创建的临时文件
            if (imageFile != null && imageFile.exists()) {
                android.util.Log.d("TryOnFragment", "拍照文件路径: " + imageFile.getAbsolutePath());
            } else {
                android.util.Log.e("TryOnFragment", "获取拍照文件失败");
                imageFile = null;
            }
        }
        
        if (imageFile != null && imageFile.exists()) {
            android.util.Log.d("TryOnFragment", "图片文件存在，开始处理: " + imageFile.getAbsolutePath());
            android.util.Log.d("TryOnFragment", "图片文件大小: " + imageFile.length() + " bytes");
            handleSelectedImage(imageFile);
        } else {
            if (imageFile == null) {
                android.util.Log.e("TryOnFragment", "图片文件为null");
            } else {
                android.util.Log.e("TryOnFragment", "图片文件不存在: " + imageFile.getAbsolutePath());
            }
            Toast.makeText(getContext(), "获取图片失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }
     
     private void addSelectedImage(File imageFile) {
         android.util.Log.d("TryOnFragment", "开始处理选中的图片: " + imageFile.getAbsolutePath());
         
         // 检查文件是否存在
         if (!imageFile.exists()) {
             android.util.Log.e("TryOnFragment", "图片文件不存在: " + imageFile.getAbsolutePath());
             Toast.makeText(getContext(), "图片文件不存在", Toast.LENGTH_SHORT).show();
             return;
         }
         
         // 检查文件格式
         String fileName = imageFile.getName().toLowerCase();
         if (!fileName.endsWith(".jpg") && !fileName.endsWith(".png") && !fileName.endsWith(".jpeg")) {
             android.util.Log.e("TryOnFragment", "不支持的图片格式: " + fileName);
             Toast.makeText(getContext(), "仅支持 JPG、PNG、JPEG 格式的图片", Toast.LENGTH_SHORT).show();
             return;
         }
         
         // 检查文件大小 (5KB - 5MB)
         long fileSize = imageFile.length();
         android.util.Log.d("TryOnFragment", "图片文件大小: " + fileSize + " bytes");
         
         if (fileSize < 5 * 1024) { // 小于5KB
             android.util.Log.e("TryOnFragment", "图片文件太小: " + fileSize + " bytes");
             Toast.makeText(getContext(), "图片文件太小，请选择大于5KB的图片", Toast.LENGTH_SHORT).show();
             return;
         }
         
         if (fileSize > 5 * 1024 * 1024) { // 大于5MB
            android.util.Log.w("TryOnFragment", "图片文件较大，开始压缩: " + fileSize + " bytes");
            try {
                imageFile = ImageUtils.compressImage(imageFile, 1024, 1024, 80);
                android.util.Log.d("TryOnFragment", "图片压缩完成，新大小: " + imageFile.length() + " bytes");
            } catch (Exception e) {
                android.util.Log.e("TryOnFragment", "图片压缩失败", e);
                Toast.makeText(getContext(), "图片处理失败", Toast.LENGTH_SHORT).show();
                return;
            }
        }
         
         // 根据当前选择分配图片文件
         switch (currentImageSelection) {
             case 0: // 模特图片
                 modelImageFile = imageFile;
                 android.util.Log.d("TryOnFragment", "设置模特图片: " + imageFile.getAbsolutePath());
                 loadImageToView(imageFile, ivModelImage, "模特图片已选择");
                 break;
             case 1: // 衣物图片（单件模式）或上衣（多件模式）
                 if (rbSeparateClothes.isChecked()) {
                     topImageFile = imageFile;
                     android.util.Log.d("TryOnFragment", "设置上衣图片: " + imageFile.getAbsolutePath());
                     loadImageToView(imageFile, ivTopImage, "上衣图片已选择");
                 } else {
                     clothingImageFile = imageFile;
                     android.util.Log.d("TryOnFragment", "设置衣物图片: " + imageFile.getAbsolutePath());
                     loadImageToView(imageFile, ivClothingImage, "衣物图片已选择");
                 }
                 break;
             case 2: // 下衣图片（仅多件模式）
                 bottomImageFile = imageFile;
                 android.util.Log.d("TryOnFragment", "设置下衣图片: " + imageFile.getAbsolutePath());
                 loadImageToView(imageFile, ivBottomImage, "下衣图片已选择");
                 break;
         }
         
         // 更新UI状态
         updateImageVisibility();
         updateTryOnButton();
         
         android.util.Log.d("TryOnFragment", "图片处理完成");
     }
      
      private void handleSelectedImage(File imageFile) {
        addSelectedImage(imageFile);
    }
    
    private void loadImageToView(File imageFile, ImageView imageView, String successMessage) {
        if (getContext() != null && imageView != null && imageFile != null && imageFile.exists()) {
            android.util.Log.d("TryOnFragment", "加载图片到ImageView: " + imageFile.getAbsolutePath());
            
            // 先设置可见性
            imageView.setVisibility(View.VISIBLE);
            
            // 使用Glide加载图片，添加监听器来检查加载状态
            Glide.with(this)
                    .load(imageFile)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_dialog_alert)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            android.util.Log.e("TryOnFragment", "Glide加载图片失败: " + (e != null ? e.getMessage() : "未知错误"));
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "图片加载失败", Toast.LENGTH_SHORT).show();
                            }
                            return false;
                        }
                        
                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            android.util.Log.d("TryOnFragment", "Glide成功加载图片");
                            return false;
                        }
                    })
                    .into(imageView);
            
            // 显示成功消息
            Toast.makeText(getContext(), successMessage, Toast.LENGTH_SHORT).show();
        } else {
            android.util.Log.e("TryOnFragment", "loadImageToView失败 - Context: " + (getContext() != null) + 
                    ", ImageView: " + (imageView != null) + 
                    ", File: " + (imageFile != null) + 
                    ", File exists: " + (imageFile != null && imageFile.exists()));
        }
    }
    
    private boolean isProcessing = false; // 防重复点击标志
    
    private void startTryOn() {
        // 防重复点击检查
        if (isProcessing) {
            android.util.Log.d("TryOnFragment", "试穿正在进行中，忽略重复点击");
            return;
        }
        
        android.util.Log.d("TryOnFragment", "=== 开始试穿流程 ===");
        
        boolean withModel = rbWithModel.isChecked();
        boolean singleOutfit = rbSingleOutfit.isChecked();
        
        android.util.Log.d("TryOnFragment", "试穿模式: " + (withModel ? "带模特" : "不带模特"));
        android.util.Log.d("TryOnFragment", "衣物类型: " + (singleOutfit ? "单件" : "分开"));
        
        // 验证必要的图片是否已选择
        if (withModel) {
            android.util.Log.d("TryOnFragment", "验证模特图片...");
            if (modelImageFile == null) {
                android.util.Log.e("TryOnFragment", "模特图片文件为null");
                showError(getString(R.string.error_no_model_image));
                return;
            }
            android.util.Log.d("TryOnFragment", "模特图片路径: " + modelImageFile.getAbsolutePath());
            android.util.Log.d("TryOnFragment", "模特图片大小: " + modelImageFile.length() + " bytes");
        }
        
        if (singleOutfit) {
            android.util.Log.d("TryOnFragment", "验证服装图片...");
            if (clothingImageFile == null) {
                android.util.Log.e("TryOnFragment", "服装图片文件为null");
                showError(getString(R.string.error_no_outfit_image));
                return;
            }
            android.util.Log.d("TryOnFragment", "服装图片路径: " + clothingImageFile.getAbsolutePath());
            android.util.Log.d("TryOnFragment", "服装图片大小: " + clothingImageFile.length() + " bytes");
        } else {
            android.util.Log.d("TryOnFragment", "验证上衣和下装图片...");
            if (topImageFile == null || bottomImageFile == null) {
                android.util.Log.e("TryOnFragment", "上衣或下装图片文件为null");
                showError(getString(R.string.error_no_garment_images));
                return;
            }
            android.util.Log.d("TryOnFragment", "上衣图片路径: " + topImageFile.getAbsolutePath());
            android.util.Log.d("TryOnFragment", "上衣图片大小: " + topImageFile.length() + " bytes");
            android.util.Log.d("TryOnFragment", "下装图片路径: " + bottomImageFile.getAbsolutePath());
            android.util.Log.d("TryOnFragment", "下装图片大小: " + bottomImageFile.length() + " bytes");
        }
        
        // 无模特试衣需要选择性别
        if (!withModel && rgGender.getCheckedRadioButtonId() == -1) {
            android.util.Log.e("TryOnFragment", "未选择性别");
            showError(getString(R.string.error_no_gender_selected));
            return;
        }
        
        // 验证图片文件大小和格式
        if (withModel && !validateImageFile(modelImageFile, "模特照片")) {
            return;
        }
        if (singleOutfit) {
            if (!validateImageFile(clothingImageFile, "套装照片")) {
                return;
            }
        } else {
            if (!validateImageFile(topImageFile, "上衣照片")) {
                return;
            }
            if (!validateImageFile(bottomImageFile, "下装照片")) {
                return;
            }
        }
        
        android.util.Log.d("TryOnFragment", "所有图片验证通过，开始显示加载状态");
        isProcessing = true; // 设置处理中状态
        btnStartTryOn.setEnabled(false); // 禁用按钮
        btnStartTryOn.setText("试穿中..."); // 更改按钮文本
        btnStartTryOn.setAlpha(0.6f); // 降低透明度
        showLoading(true);
        
        // 显示等待提示
        Toast.makeText(getContext(), "AI试衣生成中，时间可能较长，请耐心等待，不要离开...", Toast.LENGTH_LONG).show();
        
        String sex = null;
        if (!withModel) {
            sex = rbMale.isChecked() ? "male" : "female";
            android.util.Log.d("TryOnFragment", "性别参数: " + sex);
        }
        
        if (withModel) {
            if (singleOutfit) {
                android.util.Log.d("TryOnFragment", "调用带模特单件试穿API");
                // 有模特试套装 - 调用 /user/try/single
                apiService.tryOnWithModelSingle(modelImageFile, clothingImageFile, new ApiService.ApiCallback<JsonObject>() {
                    @Override
                    public void onSuccess(JsonObject response) {
                        android.util.Log.d("TryOnFragment", "带模特单件试穿API调用成功");
                        // 切换到主线程执行UI操作
                        new Handler(Looper.getMainLooper()).post(() -> {
                            isProcessing = false; // 重置处理状态
                            btnStartTryOn.setEnabled(true); // 重新启用按钮
                            btnStartTryOn.setText(getString(R.string.start_try_on)); // 恢复按钮文本
                            btnStartTryOn.setAlpha(1.0f); // 恢复透明度
                            showLoading(false);
                            handleTryOnResult(response);
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        android.util.Log.e("TryOnFragment", "带模特单件试穿API调用失败: " + error);
                        // 切换到主线程执行UI操作
                        new Handler(Looper.getMainLooper()).post(() -> {
                            isProcessing = false; // 重置处理状态
                            btnStartTryOn.setEnabled(true); // 重新启用按钮
                            btnStartTryOn.setText(getString(R.string.start_try_on)); // 恢复按钮文本
                            btnStartTryOn.setAlpha(1.0f); // 恢复透明度
                            showLoading(false);
                            showError(getString(R.string.error_try_on_failed) + error);
                        });
                    }
                });
            } else {
                android.util.Log.d("TryOnFragment", "调用带模特分开试穿API");
                // 有模特试上衣和裤子 - 调用 /user/try/double
                apiService.tryOnWithModelDouble(modelImageFile, topImageFile, bottomImageFile, new ApiService.ApiCallback<JsonObject>() {
                    @Override
                    public void onSuccess(JsonObject response) {
                        android.util.Log.d("TryOnFragment", "带模特分开试穿API调用成功");
                        // 切换到主线程执行UI操作
                        new Handler(Looper.getMainLooper()).post(() -> {
                            isProcessing = false; // 重置处理状态
                            btnStartTryOn.setEnabled(true); // 重新启用按钮
                            btnStartTryOn.setText(getString(R.string.start_try_on)); // 恢复按钮文本
                            btnStartTryOn.setAlpha(1.0f); // 恢复透明度
                            showLoading(false);
                            handleTryOnResult(response);
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        android.util.Log.e("TryOnFragment", "带模特分开试穿API调用失败: " + error);
                        // 切换到主线程执行UI操作
                        new Handler(Looper.getMainLooper()).post(() -> {
                            isProcessing = false; // 重置处理状态
                            btnStartTryOn.setEnabled(true); // 重新启用按钮
                            btnStartTryOn.setText(getString(R.string.start_try_on)); // 恢复按钮文本
                            btnStartTryOn.setAlpha(1.0f); // 恢复透明度
                            showLoading(false);
                            showError(getString(R.string.error_try_on_failed) + error);
                        });
                    }
                });
            }
        } else {
            if (singleOutfit) {
                android.util.Log.d("TryOnFragment", "调用不带模特单件试穿API");
                // 无模特试套装 - 调用 /user/try/nomodel1
                apiService.tryOnWithoutModelSingle(clothingImageFile, sex, new ApiService.ApiCallback<JsonObject>() {
                    @Override
                    public void onSuccess(JsonObject response) {
                        android.util.Log.d("TryOnFragment", "不带模特单件试穿API调用成功");
                        // 切换到主线程执行UI操作
                        new Handler(Looper.getMainLooper()).post(() -> {
                            isProcessing = false; // 重置处理状态
                            btnStartTryOn.setEnabled(true); // 重新启用按钮
                            btnStartTryOn.setText(getString(R.string.start_try_on)); // 恢复按钮文本
                            btnStartTryOn.setAlpha(1.0f); // 恢复透明度
                            showLoading(false);
                            handleTryOnResult(response);
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        android.util.Log.e("TryOnFragment", "不带模特单件试穿API调用失败: " + error);
                        // 切换到主线程执行UI操作
                        new Handler(Looper.getMainLooper()).post(() -> {
                            isProcessing = false; // 重置处理状态
                            btnStartTryOn.setEnabled(true); // 重新启用按钮
                            btnStartTryOn.setText(getString(R.string.start_try_on)); // 恢复按钮文本
                            btnStartTryOn.setAlpha(1.0f); // 恢复透明度
                            showLoading(false);
                            showError(getString(R.string.error_try_on_failed) + error);
                        });
                    }
                });
            } else {
                android.util.Log.d("TryOnFragment", "调用不带模特分开试穿API");
                // 无模特试上衣和裤子 - 调用 /user/try/nomodel2
                apiService.tryOnWithoutModelDouble(topImageFile, bottomImageFile, sex, new ApiService.ApiCallback<JsonObject>() {
                    @Override
                    public void onSuccess(JsonObject response) {
                        android.util.Log.d("TryOnFragment", "不带模特分开试穿API调用成功");
                        // 切换到主线程执行UI操作
                        new Handler(Looper.getMainLooper()).post(() -> {
                            isProcessing = false; // 重置处理状态
                            btnStartTryOn.setEnabled(true); // 重新启用按钮
                            btnStartTryOn.setText(getString(R.string.start_try_on)); // 恢复按钮文本
                            btnStartTryOn.setAlpha(1.0f); // 恢复透明度
                            showLoading(false);
                            handleTryOnResult(response);
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        android.util.Log.e("TryOnFragment", "不带模特分开试穿API调用失败: " + error);
                        // 切换到主线程执行UI操作
                        new Handler(Looper.getMainLooper()).post(() -> {
                            isProcessing = false; // 重置处理状态
                            btnStartTryOn.setEnabled(true); // 重新启用按钮
                            btnStartTryOn.setText(getString(R.string.start_try_on)); // 恢复按钮文本
                            btnStartTryOn.setAlpha(1.0f); // 恢复透明度
                            showLoading(false);
                            showError(getString(R.string.error_try_on_failed) + error);
                        });
                    }
                });
            }
        }
    }
    
    private boolean validateImageFile(File imageFile, String imageType) {
        if (imageFile == null) {
            return false;
        }
        
        // 检查文件大小（5KB - 5MB）
        long fileSize = imageFile.length();
        if (fileSize < 5 * 1024 || fileSize > 5 * 1024 * 1024) {
            showError(imageType + "文件大小必须在5KB到5MB之间");
            return false;
        }
        
        // 检查文件格式
        String fileName = imageFile.getName().toLowerCase();
        if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg") && 
            !fileName.endsWith(".png") && !fileName.endsWith(".bmp") && 
            !fileName.endsWith(".heic")) {
            showError(imageType + "格式不支持，请选择JPG、PNG、BMP或HEIC格式的图片");
            return false;
        }
        
        return true;
    }
    
    private void handleTryOnResult(JsonObject response) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                try {
                    android.util.Log.d("TryOnFragment", "处理试衣结果: " + response.toString());
                    
                    // 根据接口文档，成功时返回格式：{"code": 1000, "msg": "处理成功", "data": "http..."}
                    int code = response.has("code") ? response.get("code").getAsInt() : -1;
                    String msg = response.has("msg") ? response.get("msg").getAsString() : "";
                    
                    android.util.Log.d("TryOnFragment", "响应码: " + code + ", 消息: " + msg);
                    
                    if (code == 1000) {
                        String imageUrl = response.has("data") ? response.get("data").getAsString() : "";
                        android.util.Log.d("TryOnFragment", "获取到图片URL: " + imageUrl);
                        
                        if (!TextUtils.isEmpty(imageUrl)) {
                            // 确保结果卡片和图片视图存在
                            if (cardResult != null && ivResultImage != null) {
                                // 先显示结果卡片和图片视图
                                cardResult.setVisibility(View.VISIBLE);
                                ivResultImage.setVisibility(View.VISIBLE);
                                
                                // 使用Glide加载结果图片，添加详细的监听器
                                Glide.with(TryOnFragment.this)
                                        .load(imageUrl)
                                        .placeholder(android.R.drawable.ic_menu_gallery)
                                        .error(android.R.drawable.ic_dialog_alert)
                                        .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                                            @Override
                                            public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                                android.util.Log.e("TryOnFragment", "结果图片加载失败: " + (e != null ? e.getMessage() : "未知错误"));
                                                if (getContext() != null) {
                                                    Toast.makeText(getContext(), "结果图片加载失败", Toast.LENGTH_SHORT).show();
                                                }
                                                return false;
                                            }
                                            
                                            @Override
                                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                                android.util.Log.d("TryOnFragment", "结果图片加载成功");
                                                return false;
                                            }
                                        })
                                        .into(ivResultImage);
                                
                                // 隐藏进度条
                                if (progressBar != null) {
                                    progressBar.setVisibility(View.GONE);
                                }
                                
                                Toast.makeText(getContext(), "试衣完成", Toast.LENGTH_SHORT).show();
                            } else {
                                android.util.Log.e("TryOnFragment", "结果视图为空 - cardResult: " + (cardResult != null) + ", ivResultImage: " + (ivResultImage != null));
                                Toast.makeText(getContext(), "界面组件未初始化", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            android.util.Log.e("TryOnFragment", "未获取到有效的图片URL");
                            // 隐藏进度条
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                            Toast.makeText(getContext(), "试衣失败：未获取到结果图片", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        android.util.Log.e("TryOnFragment", "试衣请求失败，错误码: " + code);
                        // 处理错误码
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        String errorMsg = msg;
                        
                        // 根据接口文档中的错误码显示相应的错误信息
                        switch (code) {
                            case 1001:
                                errorMsg = "参数错误";
                                break;
                            case 1002:
                                errorMsg = "文件过大";
                                break;
                            case 1003:
                                errorMsg = "文件格式不支持";
                                break;
                            case 1004:
                                errorMsg = "处理失败";
                                break;
                            case 1005:
                                errorMsg = "服务器繁忙";
                                break;
                            default:
                                errorMsg = "试衣失败: " + msg;
                                break;
                        }
                        
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    android.util.Log.e("TryOnFragment", "试衣结果解析异常", e);
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    Toast.makeText(getContext(), "试衣结果解析失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
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
                // 权限授予后，自动显示图片选择对话框
                showImageSourceDialog();
            } else {
                Toast.makeText(getContext(), "需要相机和存储权限才能使用此功能", Toast.LENGTH_SHORT).show();
            }
        }
    }
}