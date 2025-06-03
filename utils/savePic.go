package utils

import (
	"ai_clouthes_backed/config"
	"fmt"
	"io"
	"math/rand"
	"net/http"
	"net/url"
	"os"
	"path"
	"time"
)

// 由于阿里云返回的图片url是直接下载的，不方便前端使用，所以需要将图片保存到本地
func SavePic(urls string) (string, error) {
	// 下载图片
	resp, err := http.Get(urls)
	if err != nil {
		Logger.Error("获取到图片，但是下载失败" + err.Error())
		return "", fmt.Errorf("获取到图片，但是下载失败" + err.Error())
	} else {
		defer resp.Body.Close()
		//命名文件，当前时间+随机数
		name := fmt.Sprintf("%s%d.%s", time.Now().Format("20060102150405"), rand.Intn(10000), ".jpg")
		path := path.Join("clouthesPic", "temp", name)
		f, err := os.Create(path)
		if err != nil {
			Logger.Error("保存阿里云图片文件时，创建文件失败" + err.Error())
			return "", fmt.Errorf("保存阿里云图片文件时，创建文件失败" + err.Error())
		} else {
			defer f.Close()
			_, err = io.Copy(f, resp.Body)
			if err != nil {
				Logger.Error("保存阿里云图片文件时，写入文件失败" + err.Error())
				return "", fmt.Errorf("保存阿里云图片文件时，写入文件失败" + err.Error())
			} else {
				return fmt.Sprintf("http://%s:%s/try_files/%s/%s", config.Configs.Server.Host, config.Configs.Server.Port, "temp", url.PathEscape(name)), nil
			}
		}
	}
}
