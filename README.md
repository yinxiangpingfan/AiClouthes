# 项目概况

## 用户类

分为：1 用户注册 2 用户登录 3 用户修改密码（只支持登陆成功后修改，不支持找回密码） 4 用户登出账号

## 衣服类

<mark>打算去除保存记录功能，以及保存衣橱功能，等过了复赛可以再加上。</mark>

#### 根据天气

1 获取地区的天气 2 根据天气推荐衣物 3 根据推荐的衣物生成穿着图片

#### 根据衣柜

1 上传衣柜照片 2 输入目的，识别衣柜的衣物，并根据目的推荐衣物 3 根据推荐的衣物生成试穿照片

#### 试穿

1 无模特试套装衣物 2 无模特试上衣+裤子 3 有模特试套装衣物 4 有模特试上衣+裤子

# 后端

启动后端需要在main.go同一级下创建server.log（用户记录错误时的日志） 以及use.log（记录使用的日志） 以及 config.yaml（配置文件）

config.yaml为以下格式

```
server:
 port: 端口号
 host: 服务器ip
cookie:
 secret: 生成token的secret
vivo:
 appid: vivo_id
 appkey: vivo_key
 aliguijikey: 轨迹流动的key
 aliyun: 阿里云百炼的key
 gaode: 高德的key
```

# 前端

分为APP和网页面
