package users

import (
	"ai_clouthes_backed/utils"
	"github.com/gofiber/fiber/v3"
)

// 退出登录
func UserLogout(ctx fiber.Ctx) error {
	//检查token是否存在
	token := ctx.Cookies("token")
	if token == "" {
		utils.Logger.Error("退出登录时，token不存在")
		return ctx.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"code": 1041,
			"msg":  "未登录",
		})
	}
	//删除cookie
	ctx.Cookie(&fiber.Cookie{
		Name:     "token",
		Value:    "",
		MaxAge:   -1,    //cookie的有效时间，时间单位秒。如果不设置过期时间，默认情况下关闭浏览器后cookie被删除
		Path:     "/",   //cookie存放目录
		Secure:   false, //是否只能通过https访问
		HTTPOnly: false, //是否允许别人通过js获取自己的cookie，设为false防止XSS攻击
	})
	return ctx.Status(fiber.StatusOK).JSON(fiber.Map{
		"code": 1000,
		"msg":  "退出登录成功",
	})
}
