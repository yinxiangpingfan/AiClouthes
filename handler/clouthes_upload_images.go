package handler

import (
	"ai_clouthes_backed/utils"
	"github.com/gofiber/fiber/v3"
	"math/rand"
	"path"
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
	//删除原来文件夹的图片
	err = utils.ClearFolder(path.Join("parsePic", tel))
	if err != nil {
		utils.Logger.Error("上传图片时，清空文件夹失败" + err.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2014,
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
		//由于系统之间的路径分隔符不同，所以需要使用filepath.Join来拼接路径
		err = ctx.SaveFile(file, filepath.Join("parsePic", tel, file.Filename))
		if err != nil {
			utils.Logger.Error("上传图片时，保存文件失败" + err.Error())
			return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
				"code": 2013,
				"msg":  "上传图片失败,请稍后再试",
			})
		}
	}
	return ctx.Status(fiber.StatusOK).JSON(fiber.Map{
		"code": 1000,
		"msg":  "上传图片成功",
	})
}
