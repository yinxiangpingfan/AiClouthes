package config

//运用viper读取配置文件

import (
	"fmt"
	"github.com/spf13/viper"
)

// 定义配置文件结构体
type Config struct {
	Database struct {
		Host     string `mapstructure:"host"`
		Port     string `mapstructure:"port"`
		Username string `mapstructure:"username"`
		Password string `mapstructure:"password"`
		Dbname   string `mapstructure:"dbname"`
	} `mapstructure:"database"`
	Server struct {
		Port string `mapstructure:"port"`
		Host string `mapstructure:"host"`
	} `mapstructure:"server"`
	Cookie struct {
		Secret string `mapstructure:"secret"`
	}
}

var Configs Config

func GetConfig(config *Config) {
	// 读取配置文件
	viper.SetConfigName("config") // 配置文件名（不带扩展名）
	viper.SetConfigType("yaml")   // 配置文件类型
	viper.AddConfigPath(".")      // 配置文件路径
	// 读取配置文件
	if err := viper.ReadInConfig(); err != nil {
		panic("读取配置文件失败: " + err.Error())
	}
	if err := viper.Unmarshal(&config); err != nil {
		panic("解析配置文件失败: " + err.Error())
	}
	fmt.Println("配置文件读取成功")
}
