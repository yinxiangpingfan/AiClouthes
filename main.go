package main

import (
	"ai_clouthes_backed/config"
	"ai_clouthes_backed/database"
	"ai_clouthes_backed/router"
	"ai_clouthes_backed/utils"
	"fmt"
	"github.com/gofiber/fiber/v3"
	"github.com/gofiber/fiber/v3/middleware/logger"
	"github.com/gofiber/fiber/v3/middleware/recover"
	"os"
)

func main() {
	//设置日志文件
	utils.OptionLogger()
	// 初始化配置文件
	config.GetConfig(&config.Configs)
	//启动数据库
	database.OpenDatabase()
	app := fiber.New()
	logfile, err := os.OpenFile("use.log", os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0666)
	if err != nil {
		panic("日志中间件设置失败")
	}
	app.Use(logger.New(
		logger.Config{
			DisableColors: false, //是否禁用颜色
			TimeFormat:    "2006-01-02 15:04:05",
			Output:        logfile,
			TimeZone:      "Asia/Shanghai",
		},
	)) //中间件,日志
	app.Use(recover.New()) //中间件,恢复
	//设置路由
	router.Router(app)
	//启动服务
	sever := fmt.Sprintf("%s:%s", config.Configs.Server.Host, config.Configs.Server.Port)
	err = app.Listen(sever)
	if err != nil {
		panic("启动服务失败" + err.Error())
	}
}
