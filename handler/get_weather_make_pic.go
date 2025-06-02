package handler

import (
	"ai_clouthes_backed/config"
	"ai_clouthes_backed/utils"
	"github.com/dingdinglz/vivo"
	"github.com/gofiber/fiber/v3"
	"time"
)

func GetWeatherMakePic(ctx fiber.Ctx) error {
	information := ctx.FormValue("information")
	app := vivo.NewVivoAIGC(vivo.Config{
		AppID:  config.Configs.Vivo.AppID,
		AppKey: config.Configs.Vivo.AppKey,
	})
	task_id, err := app.Draw(information, vivo.DRAW_THEME_GENERAL)
	if err != nil {
		utils.Logger.Error("生成图片失败,生成task_id: " + err.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2061,
			"msg":  "生成图片失败，请稍后再试",
		})
	}
	url, status, err1 := app.DrawGetResult(task_id)
	if err1 != nil {
		utils.Logger.Error("生成图片失败,获取url: " + err1.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2062,
			"msg":  "生成图片失败，请稍后再试",
		})
	}

	for status == vivo.DRAW_TASK_STATUS_QUEUE || status == vivo.DRAW_TASK_STATUS_RUNNING {
		time.Sleep(1 * time.Second)
		url, status, err = app.DrawGetResult(task_id)
		if err != nil {
			utils.Logger.Error("生成图片失败,轮询: " + err.Error())
			return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
				"code": 2063,
				"msg":  "生成图片失败，请稍后再试",
			})
		}
	}
	return ctx.Status(fiber.StatusOK).JSON(fiber.Map{
		"code": 1000,
		"msg":  "生成图片成功",
		"url":  url,
	})
}
