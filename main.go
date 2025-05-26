package main

import "ai_clouthes_backed/config"

func main() {
	// 初始化配置文件
	config.GetConfig(&config.Configs)
}
