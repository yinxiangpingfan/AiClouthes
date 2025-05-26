package main

import (
	"ai_clouthes_backed/config"
	"ai_clouthes_backed/database"
)

func main() {
	// 初始化配置文件
	config.GetConfig(&config.Configs)
	//启动数据库
	database.OpenDatabase(database.Engine)
}
