package users

import (
	"ai_clouthes_backed/database"
	"ai_clouthes_backed/utils"
	"github.com/gofiber/fiber/v3"
	"golang.org/x/crypto/bcrypt"
)

func ChangePassword(ctx fiber.Ctx) error {
	//获取用户id
	userId := ctx.Locals("userId").(int)
	//获取新密码
	newPassword := ctx.FormValue("newpassword")
	user := new(database.User)
	//修改密码
	has, err := database.Engine.ID(userId).Get(user)
	if err != nil {
		utils.Logger.Error("修改密码失败: " + err.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 1031,
			"msg":  "修改密码失败，请稍后重试",
		})
	}
	if !has {
		utils.Logger.Error("修改密码时，用户不存在，但是正常应该存在: " + err.Error())
		return ctx.Status(fiber.StatusNotFound).JSON(fiber.Map{
			"code": 1032,
			"msg":  "修改密码失败，请稍后重试",
		})
	}
	//检查新密码是否与旧密码相同
	err = bcrypt.CompareHashAndPassword([]byte(user.Password), []byte(newPassword))
	if err == nil {
		return ctx.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"code": 1033,
			"msg":  "新密码与旧密码相同",
		})
	}
	//将新密码加密
	hashPassword, e := bcrypt.GenerateFromPassword([]byte(newPassword), bcrypt.DefaultCost)
	if e != nil {
		utils.Logger.Error("修改密码时，加密失败: " + err.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 1035,
			"msg":  "修改密码失败，请稍后重试",
		})
	}
	user.Password = string(hashPassword)
	_, err = database.Engine.Id(userId).Cols("password").Update(user)
	if err != nil {
		utils.Logger.Error("修改密码时，数据库更新失败: " + err.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 1034,
			"msg":  "修改密码失败，请稍后重试",
		})
	}
	return ctx.Status(fiber.StatusOK).JSON(fiber.Map{
		"code": 1000,
		"msg":  "修改密码成功",
	})
}
