package clouthes_weather

import (
	"ai_clouthes_backed/config"
	"ai_clouthes_backed/database"
	"ai_clouthes_backed/utils"
	"encoding/json"
	"fmt"
	"os"

	"github.com/dingdinglz/vivo"
	"github.com/gofiber/fiber/v3"
)

func MakeWeatherClouthes(ctx fiber.Ctx) error {
	weather := ctx.FormValue("weather")
	userId := ctx.Locals("userId").(int)
	//查找userID对应的性别
	var user database.User
	_, e8 := database.Engine.Where("id = ?", userId).Get(&user)
	if e8 != nil {
		utils.Logger.Error("Get sex error" + e8.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2055,
			"msg":  "根据天气推荐衣服失败，请稍后再试",
		})
	}
	sex := user.Sex
	if sex == "male" {
		sex = "男"
	} else {
		sex = "女"
	}
	app := vivo.NewVivoAIGC(vivo.Config{
		AppID:  config.Configs.Vivo.AppID,
		AppKey: config.Configs.Vivo.AppKey,
	})
	content, e := os.ReadFile("content.txt")
	if e != nil {
		utils.Logger.Error("ReadFile error" + e.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2052,
			"msg":  "根据天气推荐衣服失败，请稍后再试",
		})
	}
	say := fmt.Sprintf("我是一名%s性，根据%s的天气,为我推荐合适的日常穿着。严格按以下JSON格式返回：%s", sex, weather, string(content))
	res, err := app.Chat(vivo.GenerateRequestID(), vivo.GenerateSessionID(), []vivo.ChatMessage{
		{
			Role:    vivo.CHAT_ROLE_USER,
			Content: say,
		},
	}, nil)
	if err != nil {
		utils.Logger.Error("Chat error" + err.Error())
		ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2051,
			"msg":  "根据天气推荐衣服失败，请稍后再试",
		})
	}
	type ClothingMatch struct {
		UpperGarment string `json:"UpperGarment"`
		Bottoms      string `json:"Bottoms"`
	}

	type Response struct {
		ClothingMatch ClothingMatch `json:"ClothingMatch"`
	}

	var data Response
	if err := json.Unmarshal([]byte(res.Content), &data); err != nil {
		utils.Logger.Error("Unmarshal json error" + err.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2054,
			"msg":  "根据天气推荐衣服失败，请稍后再试",
		})
	}
	return ctx.JSON(fiber.Map{
		"code": 1000,
		"data": data,
		"msg":  "success",
	})
}
