package handler

import (
	"ai_clouthes_backed/database"
	"ai_clouthes_backed/utils"
	"github.com/gofiber/fiber/v3"
	"golang.org/x/crypto/bcrypt"
)

func UserLogin(ctx fiber.Ctx) error {
	//用户登陆功能
	telephone := ctx.FormValue("telephone")
	password := ctx.FormValue("password")
	//检查密码是否正确
	user := new(database.User)
	has, err := database.Engine.Where("telephone = ?", telephone).Get(user)
	if err != nil {
		utils.Logger.Error("登陆账号时，获取手机号对应的用户结构体存在错误" + err.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 1021,
			"msg":  "登陆账号失败，请稍后再试",
		})
	}
	if !has {
		return ctx.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"code": 1022,
			"msg":  "该手机号不存在",
		})
	}
	//检查密码是否正确
	err = bcrypt.CompareHashAndPassword([]byte(user.Password), []byte(password))
	if err != nil {
		return ctx.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"code": 1023,
			"msg":  "密码错误",
		})
	}
	//生成token
	token, e := utils.Gentoken(int(user.Id))
	if e != nil {
		utils.Logger.Error("登陆账号时，生成token失败" + e.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 1024,
			"msg":  "登陆账号失败，请稍后再试",
		})
	}
	//将token存入cookie
	ctx.Cookie(&fiber.Cookie{
		Name:     "token",
		Value:    token,
		MaxAge:   86400, //cookie的有效时间，时间单位秒。如果不设置过期时间，默认情况下关闭浏览器后cookie被删除
		Path:     "/",   //cookie存放目录
		Secure:   false, //是否只能通过https访问
		HTTPOnly: false, //是否允许别人通过js获取自己的cookie，设为false防止XSS攻击
	})
	return ctx.Status(fiber.StatusOK).JSON(fiber.Map{
		"code":  1020,
		"msg":   "登陆成功",
		"token": token,
	})
}
