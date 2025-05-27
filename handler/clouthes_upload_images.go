package handler

import (
	"ai_clouthes_backed/utils"
	"github.com/gofiber/fiber/v3"
	"math/rand"
	"path/filepath"
	"strconv"
	"time"
)

//此路由用于上传图片

func UploadImages(ctx fiber.Ctx) error {
	//上传图片
	userId := ctx.Locals("userId").(int)
	form, err := ctx.MultipartForm()
	if err != nil {
		utils.Logger.Error("上传图片时，获取表单数据失败" + err.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2011,
			"msg":  "上传图片失败,请稍后再试",
		})
	}
	files := form.File["pictures"]
	tel, e := utils.IdToTelephone(userId)
	if e != nil {
		utils.Logger.Error("上传图片时，获取手机号失败" + e.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2012,
			"msg":  "上传图片失败,请稍后再试",
		})
	}
	//遍历文件
	for i, file := range files {
		//保存文件
		//更改文件名字为时间+随机数+序号+原来文件的后缀名
		randomNum := rand.Intn(100000)
		randomNumStr := strconv.Itoa(randomNum)
		now := time.Now()
		nowStr := now.Format("20060102150405")
		file.Filename = nowStr + strconv.Itoa(i) + randomNumStr + filepath.Ext(file.Filename)
		err = ctx.SaveFile(file, "./"+tel+file.Filename)
	}
	return ctx.Status(fiber.StatusOK).JSON(fiber.Map{
		"code": 1000,
		"msg":  "上传图片成功",
	})
}
