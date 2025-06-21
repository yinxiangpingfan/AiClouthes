package clouthes_weather

import (
	"ai_clouthes_backed/config"
	"ai_clouthes_backed/database"
	"ai_clouthes_backed/utils"
	"fmt"
	"time"

	"github.com/dingdinglz/vivo"
	"github.com/gofiber/fiber/v3"
)

func GetWeatherMakePic(ctx fiber.Ctx) error {
	information := ctx.FormValue("information")
	userId := ctx.Locals("userId").(int)
	//查找userID对应的性别
	var user database.User
	_, e8 := database.Engine.Where("id = ?", userId).Get(&user)
	if e8 != nil {
		utils.Logger.Error("Get sex error" + e8.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2064,
			"msg":  "根据天气推荐衣服失败，请稍后再试",
		})
	}
	sex := user.Sex
	if sex == "male" {
		sex = "男"
	} else {
		sex = "女"
	}
	information = fmt.Sprintf("一位%s性，%s", sex, information)
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
