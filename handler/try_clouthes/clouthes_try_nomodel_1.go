package try_clouthes

import (
	"ai_clouthes_backed/config"
	"ai_clouthes_backed/utils"
	"fmt"
	"github.com/gofiber/fiber/v3"
	"mime/multipart"
	"net/url"
	"path"
	"time"
)

//不需要用户提供模特图片

func TryClouthesNoModel1(c fiber.Ctx) error {
	userId := c.Locals("userId").(int)
	telephone, err := utils.IdToTelephone(userId)
	if err != nil {
		utils.Logger.Error("获取用户手机号失败: " + err.Error())
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2091,
			"msg":  "服务器出现错误，请稍后再试",
		})
	}
	// 处理三个文件上传
	var topFile *multipart.FileHeader
	topFile, err = c.FormFile("topGarment")
	if err != nil {
		utils.Logger.Error("上装文件获取失败: " + err.Error())
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2092,
			"msg":  "服务器出现错误，请稍后再试",
		})
	}
	sex := c.FormValue("sex")
	saveAndGetURL := func(file *multipart.FileHeader, a int) string {
		filename := fmt.Sprintf("%d-%d-%d%s", time.Now().Unix(), time.Now().Nanosecond(), a, path.Ext(file.Filename))
		dstPath := path.Join("clouthesPic", telephone, filename)
		if err := c.SaveFile(file, dstPath); err != nil {
			return ""
		}
		// 修改saveAndGetURL函数中的URL生成部分
		return fmt.Sprintf("http://%s:%s/try_files/%s/%s", config.Configs.Server.Host, config.Configs.Server.Port, telephone, url.PathEscape(filename))
	}
	var personURL string
	if sex == "male" {
		personURL = fmt.Sprintf("http://%s:%s/try_files/%s/%s", config.Configs.Server.Host, config.Configs.Server.Port, "model", url.PathEscape("male.png"))
	} else {
		personURL = fmt.Sprintf("http://%s:%s/try_files/%s/%s", config.Configs.Server.Host, config.Configs.Server.Port, "model", url.PathEscape("female.png"))
	}
	topURL := saveAndGetURL(topFile, 1)
	fmt.Println(topURL)
	fmt.Println(personURL)
	if topURL == "" || personURL == "" {
		utils.Logger.Error("文件上传失败")
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"code": 2095,
			"msg":  "文件上传失败",
		})
	}
	//time.Sleep(1000 * time.Second)
	// 提交异步任务
	var taskID string
	taskID, err = SubmitTaskTryClouthesDouble(topURL, "", personURL)
	if taskID == "" {
		utils.Logger.Error("提交异步任务失败" + err.Error())
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"code": 2096,
			"msg":  "服务器出现错误，请稍后再试",
		})
	}
	//轮询任务状态
	var result string
	result, err = PollTaskStatus(taskID)
	if err != nil {
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2098,
			"msg":  "服务器出现错误，请稍后再试",
		})
	}
	//删除telephone_try下的所有文件
	utils.ClearFolder(path.Join("clouthesPic", telephone))
	if result != "" {
		result, err = utils.SavePic(result)
		if err != nil {
			return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
				"code": 2099,
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
			"code": 2097,
			"msg":  "服务器出现错误，请稍后再试"})

	}
}
