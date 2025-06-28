package try_clouthes

import (
	"ai_clouthes_backed/config"
	"ai_clouthes_backed/utils"
	"fmt"
	"mime/multipart"
	"net/url"
	"path"
	"time"

	"github.com/gofiber/fiber/v3"
)

func TryClouthesDouble(c fiber.Ctx) error {
	userId := c.Locals("userId").(int)
	telephone, err := utils.IdToTelephone(userId)
	if err != nil {
		utils.Logger.Error("获取用户手机号失败: " + err.Error())
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2081,
			"msg":  "服务器出现错误，请稍后再试",
		})
	}
	// 处理三个文件上传
	var topFile, bottomFile, personFile *multipart.FileHeader
	topFile, err = c.FormFile("topGarment")
	if err != nil {
		utils.Logger.Error("上装文件获取失败: " + err.Error())
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2082,
			"msg":  "服务器出现错误，请稍后再试",
		})
	}
	bottomFile, err = c.FormFile("bottomGarment")
	if err != nil {
		utils.Logger.Error("下装文件获取失败: " + err.Error())
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2083,
			"msg":  "服务器出现错误，请稍后再试",
		})
	}
	personFile, err = c.FormFile("personImage")
	if err != nil {
		utils.Logger.Error("模特文件获取失败: " + err.Error())
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2084,
			"msg":  "服务器出现错误，请稍后再试",
		})
	}

	// 保存文件并生成URL
	saveAndGetURL := func(file *multipart.FileHeader, a int) string {
		filename := fmt.Sprintf("%d-%d-%d%s", time.Now().Unix(), time.Now().Nanosecond(), a, path.Ext(file.Filename))
		dstPath := path.Join("clouthesPic", telephone, filename)
		if err := c.SaveFile(file, dstPath); err != nil {
			return ""
		}
		// 修改saveAndGetURL函数中的URL生成部分
		return fmt.Sprintf("http://%s:%s/try_files/%s/%s", config.Configs.Server.Host, config.Configs.Server.Port, telephone, url.PathEscape(filename))
	}

	topURL := saveAndGetURL(topFile, 1)
	bottomURL := saveAndGetURL(bottomFile, 2)
	personURL := saveAndGetURL(personFile, 3)
	fmt.Println(topURL)
	fmt.Println(bottomURL)
	fmt.Println(personURL)
	if topURL == "" || bottomURL == "" || personURL == "" {
		utils.Logger.Error("文件上传失败")
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"code": 2085,
			"msg":  "文件上传失败",
		})
	}
	//time.Sleep(1000 * time.Second)
	// 提交异步任务
	var taskID string
	taskID, err = SubmitTaskTryClouthesDouble(topURL, bottomURL, personURL)
	if taskID == "" {
		utils.Logger.Error("提交异步任务失败" + err.Error())
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"code": 2086,
			"msg":  "服务器出现错误，请稍后再试",
		})
	}
	//轮询任务状态
	var result string
	result, err = PollTaskStatus(taskID)
	if err != nil {
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2088,
			"msg":  "服务器出现错误，请稍后再试",
		})
	}
	//删除telephone_try下的所有文件
	utils.ClearFolder(path.Join("clouthesPic", telephone))
	if result != "" {
		result, err = utils.SavePic(result)
		if err != nil {
			return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
				"code": 2089,
				"msg":  "服务器出现错误，请稍后再试",
			})
		}
		return c.Status(200).JSON(fiber.Map{
			"code": 1000,
			"msg":  "处理成功",
			"data": result,
		})
	} else {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"code": 2087,
			"msg":  "服务器出现错误，请稍后再试"})

	}
}
