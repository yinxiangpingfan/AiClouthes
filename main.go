package main

import (
	"ai_clouthes_backed/config"
	"ai_clouthes_backed/database"
	"ai_clouthes_backed/router"
	"ai_clouthes_backed/utils"
	"fmt"
	"os"
	"path"

	"github.com/gofiber/fiber/v3"
	"github.com/gofiber/fiber/v3/middleware/cors"
	"github.com/gofiber/fiber/v3/middleware/logger"
	"github.com/gofiber/fiber/v3/middleware/recover"
)

func main() {
	//设置日志文件
	utils.OptionLogger()
	// 初始化配置文件
	config.GetConfig(&config.Configs)
	//启动数据库
	database.OpenDatabase()
	app := fiber.New(fiber.Config{
		BodyLimit: 50 * 1024 * 1024, // 50MB
	})
	// 关键：CORS 中间件必须放在最前面
	app.Use(cors.New(cors.Config{
		// 允许的前端域名（生产+开发环境）
		AllowOrigins:     []string{"*"},
		AllowCredentials: false,
		AllowMethods: []string{
			fiber.MethodGet,
			fiber.MethodPost,
			fiber.MethodHead,
			fiber.MethodPut,
			fiber.MethodDelete,
			fiber.MethodPatch,
			fiber.MethodOptions,
		},
		AllowHeaders: []string{
			"Origin",
			"Content-Type",
			"Accept",
			"Authorization",
			"X-Requested-With",
			"Cookie",
		},
		ExposeHeaders: []string{"Set-Cookie"}, // 允许前端访问 Set-Cookie 头
	}))
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
	//判断是否存在文件夹,不存在则创建
	err = utils.HaveFloder(path.Join("parsePic"))
	if err != nil {
		panic("创建文件夹失败")
	}
	err = utils.HaveFloder(path.Join("dapeiPic"))
	if err != nil {
		panic("创建文件夹失败")
	}
	err = utils.HaveFloder(path.Join("clouthesPic", "temp"))
	if err != nil {
		panic("创建文件夹失败")
	}
	//设置路由
	router.Router(app)
	//启动服务
	sever := fmt.Sprintf("%s:%s", "0.0.0.0", config.Configs.Server.Port)
	err = app.Listen(sever)
	if err != nil {
		panic("启动服务失败" + err.Error())
	}
}
