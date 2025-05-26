package utils

import "github.com/gofiber/fiber/v3"

func Middle(ctx fiber.Ctx) error {
	token := ctx.Cookies("token")
	if token == "" {
		return ctx.Status(fiber.StatusUnauthorized).JSON(fiber.Map{
			"code": 1101,
			"msg":  "请重新登录",
		})
	}
	//解析token
	a, err := ParseToken(token)
	if err != nil {
		return ctx.Status(fiber.StatusUnauthorized).JSON(fiber.Map{
			"code": 1102,
			"msg":  "请重新登录",
		})
	}
	//将用户id存入ctx中
	ctx.Locals("userId", a)
	ctx.Next()
	return nil
}
