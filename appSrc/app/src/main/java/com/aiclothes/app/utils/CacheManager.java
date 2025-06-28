package com.aiclothes.app.utils;

import android.content.Context;
import android.util.Log;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 缓存管理工具类
 * 用于管理APP内的图片缓存，避免存储空间占用过大
 */
public class CacheManager {
    private static final String TAG = "CacheManager";
    
    // 缓存目录名称
    private static final String WARDROBE_CACHE_DIR = "wardrobe_cache";
    private static final String TRY_ON_CACHE_DIR = "try_on_cache";
    private static final String GLIDE_CACHE_DIR = "image_manager_disk_cache";
    
    /**
     * 清除衣橱管理功能的缓存图片
     * 在用户上传第一个衣服图片时调用
     */
    public static void clearWardrobeCache(Context context) {
        if (context == null) {
            Log.w(TAG, "Context为空，无法清除衣橱缓存");
            return;
        }
        
        Log.d(TAG, "开始清除衣橱缓存...");
        
        // 清除Glide缓存
        clearGlideCache(context);
        
        // 清除应用内部存储的衣橱相关图片
        clearDirectoryCache(context, WARDROBE_CACHE_DIR);
        
        // 清除外部存储的图片缓存
        clearExternalPicturesCache(context);
        
        Log.d(TAG, "衣橱缓存清除完成");
    }
    
    /**
     * 清除AI试衣功能的缓存图片（除了当前试穿结果）
     * 在获取试穿结果时调用
     */
    public static void clearTryOnCache(Context context, String currentResultUrl) {
        if (context == null) {
            Log.w(TAG, "Context为空，无法清除试衣缓存");
            return;
        }
        
        Log.d(TAG, "开始清除试衣缓存（保留当前结果）...");
        
        // 清除Glide缓存（但保留当前结果图片）
        clearGlideCacheExceptCurrent(context, currentResultUrl);
        
        // 清除应用内部存储的试衣相关图片
        clearDirectoryCache(context, TRY_ON_CACHE_DIR);
        
        // 清除临时图片文件
        clearTempImages(context);
        
        Log.d(TAG, "试衣缓存清除完成");
    }
    
    /**
     * 清除AI试衣功能的缓存图片
     * @param context 上下文
     * @param keepCurrentResult 是否保留当前结果
     */
    public static void clearTryOnCache(Context context, boolean keepCurrentResult) {
        if (context == null) {
            Log.w(TAG, "Context为空，无法清除试衣缓存");
            return;
        }
        
        Log.d(TAG, "开始清除试衣缓存...");
        
        if (keepCurrentResult) {
            // 保留当前结果，只清除其他缓存
            clearGlideCacheExceptCurrent(context, null);
        } else {
            // 清除所有Glide缓存
            clearGlideCache(context);
        }
        
        // 清除应用内部存储的试衣相关图片
        clearDirectoryCache(context, TRY_ON_CACHE_DIR);
        
        // 清除临时图片文件
        clearTempImages(context);
        
        Log.d(TAG, "试衣缓存清除完成");
    }
    
    /**
     * 清除所有图片缓存
     * 可在应用设置中提供给用户手动清理
     */
    public static void clearAllImageCache(Context context) {
        if (context == null) {
            Log.w(TAG, "Context为空，无法清除所有缓存");
            return;
        }
        
        Log.d(TAG, "开始清除所有图片缓存...");
        
        // 清除Glide缓存
        clearGlideCache(context);
        
        // 清除所有应用内部缓存目录
        clearDirectoryCache(context, WARDROBE_CACHE_DIR);
        clearDirectoryCache(context, TRY_ON_CACHE_DIR);
        
        // 清除外部存储的图片缓存
        clearExternalPicturesCache(context);
        
        // 清除临时图片文件
        clearTempImages(context);
        
        Log.d(TAG, "所有图片缓存清除完成");
    }
    
    /**
     * 获取缓存大小信息
     */
    public static String getCacheSizeInfo(Context context) {
        if (context == null) {
            return "无法获取缓存信息";
        }
        
        long totalSize = 0;
        
        // 计算Glide缓存大小
        File glideCache = new File(context.getCacheDir(), GLIDE_CACHE_DIR);
        totalSize += getDirectorySize(glideCache);
        
        // 计算应用内部缓存大小
        File wardrobeCache = new File(context.getCacheDir(), WARDROBE_CACHE_DIR);
        totalSize += getDirectorySize(wardrobeCache);
        
        File tryOnCache = new File(context.getCacheDir(), TRY_ON_CACHE_DIR);
        totalSize += getDirectorySize(tryOnCache);
        
        // 计算外部存储图片大小
        File externalPictures = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        if (externalPictures != null) {
            totalSize += getDirectorySize(externalPictures);
        }
        
        return formatFileSize(totalSize);
    }
    
    /**
     * 清除Glide缓存
     */
    private static void clearGlideCache(Context context) {
        try {
            // 清除内存缓存（主线程）
            Glide.get(context).clearMemory();
            
            // 清除磁盘缓存（后台线程）
            new Thread(() -> {
                try {
                    Glide.get(context).clearDiskCache();
                    Log.d(TAG, "Glide磁盘缓存清除完成");
                } catch (Exception e) {
                    Log.e(TAG, "清除Glide磁盘缓存失败", e);
                }
            }).start();
            
            Log.d(TAG, "Glide内存缓存清除完成");
        } catch (Exception e) {
            Log.e(TAG, "清除Glide缓存失败", e);
        }
    }
    
    /**
     * 清除Glide缓存（但保留指定URL的图片）
     */
    private static void clearGlideCacheExceptCurrent(Context context, String preserveUrl) {
        try {
            // 清除内存缓存
            Glide.get(context).clearMemory();
            
            // 对于磁盘缓存，由于Glide不提供选择性清除，我们只能全部清除
            // 当前结果图片会在下次加载时重新缓存
            new Thread(() -> {
                try {
                    Glide.get(context).clearDiskCache();
                    Log.d(TAG, "Glide磁盘缓存清除完成（保留当前结果将重新缓存）");
                } catch (Exception e) {
                    Log.e(TAG, "清除Glide磁盘缓存失败", e);
                }
            }).start();
            
            Log.d(TAG, "Glide缓存清除完成");
        } catch (Exception e) {
            Log.e(TAG, "清除Glide缓存失败", e);
        }
    }
    
    /**
     * 清除指定目录的缓存
     */
    private static void clearDirectoryCache(Context context, String dirName) {
        try {
            File cacheDir = new File(context.getCacheDir(), dirName);
            if (cacheDir.exists()) {
                deleteDirectory(cacheDir);
                Log.d(TAG, "目录缓存清除完成: " + dirName);
            }
        } catch (Exception e) {
            Log.e(TAG, "清除目录缓存失败: " + dirName, e);
        }
    }
    
    /**
     * 清除外部存储的图片缓存
     */
    private static void clearExternalPicturesCache(Context context) {
        try {
            File picturesDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
            if (picturesDir != null && picturesDir.exists()) {
                // 只删除临时文件和压缩文件，保留用户主动保存的图片
                File[] files = picturesDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            String fileName = file.getName().toLowerCase();
                            // 删除临时文件和压缩文件
                            if (fileName.startsWith("jpeg_") || 
                                fileName.startsWith("compressed_") ||
                                fileName.contains("temp") ||
                                fileName.contains("cache")) {
                                if (file.delete()) {
                                    Log.d(TAG, "删除临时图片文件: " + fileName);
                                }
                            }
                        }
                    }
                }
                Log.d(TAG, "外部存储图片缓存清除完成");
            }
        } catch (Exception e) {
            Log.e(TAG, "清除外部存储图片缓存失败", e);
        }
    }
    
    /**
     * 清除临时图片文件
     */
    private static void clearTempImages(Context context) {
        try {
            // 清除应用缓存目录下的临时图片
            File cacheDir = context.getCacheDir();
            if (cacheDir.exists()) {
                File[] files = cacheDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            String fileName = file.getName().toLowerCase();
                            if (fileName.endsWith(".jpg") || 
                                fileName.endsWith(".png") || 
                                fileName.endsWith(".jpeg") ||
                                fileName.contains("temp") ||
                                fileName.contains("compressed")) {
                                if (file.delete()) {
                                    Log.d(TAG, "删除临时缓存文件: " + fileName);
                                }
                            }
                        }
                    }
                }
            }
            Log.d(TAG, "临时图片文件清除完成");
        } catch (Exception e) {
            Log.e(TAG, "清除临时图片文件失败", e);
        }
    }
    
    /**
     * 递归删除目录及其内容
     */
    private static boolean deleteDirectory(File directory) {
        if (directory != null && directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            return directory.delete();
        }
        return false;
    }
    
    /**
     * 计算目录大小
     */
    private static long getDirectorySize(File directory) {
        long size = 0;
        if (directory != null && directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += getDirectorySize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        }
        return size;
    }
    
    /**
     * 格式化文件大小
     */
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(java.util.Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format(java.util.Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}