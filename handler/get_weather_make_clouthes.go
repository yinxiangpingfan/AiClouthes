package handler

import (
	"ai_clouthes_backed/config"
	"fmt"
	"github.com/dingdinglz/vivo"
	"github.com/gofiber/fiber/v3"
)

func MakeWeatherClouthes(ctx fiber.Ctx) error {
	weather := ctx.FormValue("weather")
	sex := ctx.FormValue("sex")
	app := vivo.NewVivoAIGC(vivo.Config{
		AppID:  config.Configs.Vivo.AppID,
		AppKey: config.Configs.Vivo.AppKey,
	})

	say := fmt.Sprintf("我是一名%s性，根据%s的天气,为我推荐合适的日常穿着。严格按以下JSON格式返回：{\"ClothingMatch\": {\"UpperGarment\": \"具体上装推荐\",\"Bottoms\": \"具体下装推荐\"}}要求：1. 仅返回JSON对象，不要包含任何解释性文字2. 推荐需符合天气和性别3. 可自由修改推荐内容，但保持字段结构不变4. 衣物描述需具体明确（如材质/厚度/款式）", sex, weather)
	res, err := app.Chat(vivo.GenerateRequestID(), vivo.GenerateSessionID(), []vivo.ChatMessage{
		{
			Role:    vivo.CHAT_ROLE_USER,
			Content: say,
		},
	}, nil)
	if err != nil {
		ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2051,
			"msg":  "根据天气推荐衣服失败，请稍后再试",
		})
	}
	return ctx.JSON(fiber.Map{
		"code": 1000,
		"data": res,
		"msg":  "success",
	})
}
