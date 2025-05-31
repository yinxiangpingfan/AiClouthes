package handler

import (
	"ai_clouthes_backed/config"
	"ai_clouthes_backed/database"
	"ai_clouthes_backed/utils"
	"fmt"
	"github.com/dingdinglz/vivo"
	"github.com/gofiber/fiber/v3"
	"strings"
	"time"
)

// 生成图片
func ClouthesMakeImages(ctx fiber.Ctx) error {
	userId := ctx.Locals("userId").(int)
	var user database.User
	_, e := database.Engine.Id(userId).Get(&user)
	if e != nil {
		utils.Logger.Error("生成图片失败,获取用户信息: " + e.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2034,
			"msg":  "生成图片失败，请稍后再试",
		})
	}
	//截取say根据您的需求，我推荐您穿后面的文字
	say := user.Temp
	extracted := extractRecommendation(say)
	recommand := "生成一件白色衬衫，黑色西服裤子的试穿图片"
	recommand = fmt.Sprintf("根据一下穿搭，生成真人试穿结果，%s", extracted)
	app := vivo.NewVivoAIGC(vivo.Config{
		AppID:  config.Configs.Vivo.AppID,
		AppKey: config.Configs.Vivo.AppKey,
	})
	task_id, err := app.Draw(recommand, vivo.DRAW_THEME_GENERAL)
	if err != nil {
		utils.Logger.Error("生成图片失败,生成task_id: " + err.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2031,
			"msg":  "生成图片失败，请稍后再试",
		})
	}
	url, status, err1 := app.DrawGetResult(task_id)
	if err1 != nil {
		utils.Logger.Error("生成图片失败,获取url: " + err1.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2032,
			"msg":  "生成图片失败，请稍后再试",
		})
	}

	for status == vivo.DRAW_TASK_STATUS_QUEUE || status == vivo.DRAW_TASK_STATUS_RUNNING {
		time.Sleep(1 * time.Second)
		url, status, err = app.DrawGetResult(task_id)
		if err != nil {
			utils.Logger.Error("生成图片失败,轮询: " + err.Error())
			return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
				"code": 2033,
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

func extractRecommendation(input string) string {
	// 定义起始和结束标记
	startMarker := "根据您的需求，我推荐您穿"
	endMarker := "原因"

	// 查找起始位置
	startIdx := strings.Index(input, startMarker)
	if startIdx == -1 {
		return "" // 未找到起始标记
	}

	// 计算推荐内容的起始位置（跳过起始标记）
	contentStart := startIdx + len(startMarker)
	remaining := (input)[contentStart:]

	// 查找结束标记位置
	endIdx := strings.Index(remaining, endMarker)
	if endIdx == -1 {
		// 未找到结束标记则截取到字符串末尾
		return strings.TrimSpace(remaining)
	}

	// 截取推荐内容并移除尾部空白
	return strings.TrimSpace(remaining[:endIdx])
}
