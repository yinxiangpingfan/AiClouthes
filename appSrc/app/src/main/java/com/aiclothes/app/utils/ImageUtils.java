package com.aiclothes.app.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageUtils {
    public static final int REQUEST_IMAGE_CAPTURE = 1001;
    public static final int REQUEST_IMAGE_PICK = 1002;
    
    private static File currentPhotoFile;
    
    // 从相册选择图片
    public static void pickImageFromGallery(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        activity.startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }
    
    // 拍照
    public static void captureImage(Activity activity) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            File photoFile = createImageFile(activity);
            if (photoFile != null) {
                currentPhotoFile = photoFile;
                Uri photoURI = FileProvider.getUriForFile(activity,
                        activity.getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                activity.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else {
            Toast.makeText(activity, "没有找到相机应用", Toast.LENGTH_SHORT).show();
        }
    }
    
    // 创建图片文件
    private static File createImageFile(Context context) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // 获取当前拍照的文件
    public static File getCurrentPhotoFile() {
        return currentPhotoFile;
    }
    
    // 从Uri获取文件
    public static File getFileFromUri(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            
            File tempFile = createImageFile(context);
            if (tempFile == null) return null;
            
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            
            outputStream.close();
            inputStream.close();
            
            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // 压缩图片
    public static File compressImage(File originalFile, int maxWidth, int maxHeight, int quality) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(originalFile.getAbsolutePath(), options);
            
            int imageWidth = options.outWidth;
            int imageHeight = options.outHeight;
            
            int scaleFactor = Math.min(imageWidth / maxWidth, imageHeight / maxHeight);
            if (scaleFactor < 1) scaleFactor = 1;
            
            options.inJustDecodeBounds = false;
            options.inSampleSize = scaleFactor;
            
            Bitmap bitmap = BitmapFactory.decodeFile(originalFile.getAbsolutePath(), options);
            if (bitmap == null) return originalFile;
            
            File compressedFile = new File(originalFile.getParent(), "compressed_" + originalFile.getName());
            FileOutputStream outputStream = new FileOutputStream(compressedFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.close();
            bitmap.recycle();
            
            return compressedFile;
        } catch (Exception e) {
            e.printStackTrace();
            return originalFile;
        }
    }
    
    // 检查文件大小
    public static boolean isFileSizeValid(File file) {
        long fileSizeInBytes = file.length();
        long fileSizeInKB = fileSizeInBytes / 1024;
        long fileSizeInMB = fileSizeInKB / 1024;
        
        return fileSizeInKB >= 5 && fileSizeInMB <= 5;
    }
    
    // 检查图片格式
    public static boolean isImageFormatValid(String fileName) {
        String extension = fileName.toLowerCase();
        return extension.endsWith(".jpg") || 
               extension.endsWith(".jpeg") || 
               extension.endsWith(".png") || 
               extension.endsWith(".bmp") || 
               extension.endsWith(".heic");
    }
    
    // 获取文件大小描述
    public static String getFileSizeDescription(File file) {
        long fileSizeInBytes = file.length();
        if (fileSizeInBytes < 1024) {
            return fileSizeInBytes + " B";
        } else if (fileSizeInBytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", fileSizeInBytes / 1024.0);
        } else {
            return String.format(Locale.getDefault(), "%.1f MB", fileSizeInBytes / (1024.0 * 1024.0));
        }
    }
}