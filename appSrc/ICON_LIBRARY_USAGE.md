# 图标库使用指南

## 已添加的图标库

### 1. Iconics Core
```gradle
implementation 'com.mikepenz:iconics-core:5.4.0'
```

### 2. Material Design Iconic Font
```gradle
implementation 'com.mikepenz:material-design-iconic-typeface:2.2.0.8-kotlin@aar'
```

### 3. Google Material Typeface
```gradle
implementation 'com.mikepenz:google-material-typeface:4.0.0.3-kotlin@aar'
```

## 如何使用图标库中的图标

### 在XML布局中使用

```xml
<!-- 使用Material Design图标 -->>
<com.mikepenz.iconics.view.IconicsImageView
    android:layout_width="24dp"
    android:layout_height="24dp"
    app:iiv_icon="gmd-weather-sunny"
    app:iiv_color="@color/primary_color" />

<!-- 使用Material Design Iconic图标 -->
<com.mikepenz.iconics.view.IconicsImageView
    android:layout_width="24dp"
    android:layout_height="24dp"
    app:iiv_icon="zmdi-weather-sunny"
    app:iiv_color="@color/primary_color" />
```

### 在Java代码中使用

```java
// 设置ImageView的图标
ImageView imageView = findViewById(R.id.my_image_view);
IconicsDrawable drawable = new IconicsDrawable(this)
    .icon(GoogleMaterial.Icon.gmd_weather_sunny)
    .color(ContextCompat.getColor(this, R.color.primary_color))
    .sizeDp(24);
imageView.setImageDrawable(drawable);

// 设置到Button
Button button = findViewById(R.id.my_button);
button.setCompoundDrawablesWithIntrinsicBounds(
    new IconicsDrawable(this)
        .icon(GoogleMaterial.Icon.gmd_weather_sunny)
        .color(Color.WHITE)
        .sizeDp(18),
    null, null, null
);
```

## 推荐的天气相关图标

### Google Material Icons
- `gmd-weather-sunny` - 晴天
- `gmd-weather-cloudy` - 多云
- `gmd-weather-rainy` - 雨天
- `gmd-weather-snowy` - 雪天
- `gmd-thermostat` - 温度计
- `gmd-air` - 风

### Material Design Iconic Icons
- `zmdi-weather-sunny` - 晴天
- `zmdi-weather-cloudy` - 多云
- `zmdi-weather-rainy` - 雨天
- `zmdi-weather-snowy` - 雪天

## 推荐的衣物相关图标

### Google Material Icons
- `gmd-checkroom` - 衣帽间
- `gmd-dry_cleaning` - 干洗
- `gmd-local_laundry_service` - 洗衣服务
- `gmd-shopping_bag` - 购物袋
- `gmd-style` - 样式/时尚

### Material Design Iconic Icons
- `zmdi-shopping-bag` - 购物袋
- `zmdi-shopping-cart` - 购物车
- `zmdi-store` - 商店

## 推荐的试衣相关图标

### Google Material Icons
- `gmd-person` - 人物
- `gmd-face` - 脸部
- `gmd-accessibility` - 可访问性
- `gmd-style` - 样式
- `gmd-palette` - 调色板

## 初始化图标库

在Application类中初始化图标库：

```java
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 注册图标字体
        Iconics.registerFont(new GoogleMaterial());
        Iconics.registerFont(new MaterialDesignIconic());
    }
}
```

记得在AndroidManifest.xml中声明Application类：

```xml
<application
    android:name=".MyApplication"
    ... >
```

## 性能优化建议

1. **按需加载**: 只注册实际使用的图标字体
2. **缓存图标**: 对于频繁使用的图标，考虑缓存IconicsDrawable实例
3. **合适的尺寸**: 根据实际显示需求设置合适的图标尺寸
4. **颜色主题**: 使用主题颜色保持一致性

## 自定义图标

如果需要自定义图标，可以：

1. 继续使用Vector Drawable（推荐）
2. 创建自定义图标字体
3. 使用SVG转换工具

这样可以保持图标的可扩展性和一致性。