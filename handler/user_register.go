package handler

import (
	"ai_clouthes_backed/database"
	"ai_clouthes_backed/utils"
	"github.com/gofiber/fiber/v3"
	"golang.org/x/crypto/bcrypt"
	"os"
)

// 注册账号
func UserRegister(ctx fiber.Ctx) error {
	telephone := ctx.FormValue("telephone")
	easyPassword := ctx.FormValue("password") //加密前的密码
	//判断手机号是否存在
	has, err := database.Engine.Exist(&database.User{
		Telephone: telephone,
	})
	//后端发生错误
	if err != nil {
		utils.Logger.Error("注册账号时，判断手机号是否存在错误" + err.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 1011,
			"msg":  "注册账号失败，请稍后再试",
		})
	}
	if has {
		//手机号已存在
		return ctx.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"code": 1012,
			"msg":  "该手机号已被注册",
		})
	} else {
		//手机号不存在，可以注册
		//先对密码进行加密
		hashPassword, err := bcrypt.GenerateFromPassword([]byte(easyPassword), bcrypt.DefaultCost)
		if err != nil {
			utils.Logger.Error("注册账号时，密码加密失败" + err.Error())
			return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
				"code": 1013,
				"msg":  "注册账号失败，请稍后再试",
			})
		}
		//创建文件夹
		err = os.Mkdir("./"+telephone+"parseimage", 0755)
		//把加密后的密码存入数据库
		user := database.User{
			Telephone: telephone,
			Password:  string(hashPassword),
		}
		_, err = database.Engine.Insert(&user)
		if err != nil {
			utils.Logger.Error("注册账号时，插入数据库失败" + err.Error())
			return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
				"code": 1014,
				"msg":  "注册账号失败，请稍后再试",
			})
		}
		//注册成功
		return ctx.Status(fiber.StatusOK).JSON(fiber.Map{
			"code": 1000,
			"msg":  "注册账号成功",
		})
	}
}
