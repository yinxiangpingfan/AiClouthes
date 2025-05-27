package utils

import (
	fiberlog "github.com/gofiber/fiber/v3/log"
	"os"
)

var Logger fiberlog.AllLogger

func OptionLogger() {
	Logger = fiberlog.DefaultLogger()
	logFile, err := os.OpenFile("server.log", os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0666)
	if err != nil {
		panic("打开日志文件失败: " + err.Error())
	}
	Logger.SetLevel(fiberlog.LevelError)
	Logger.SetOutput(logFile)
}
