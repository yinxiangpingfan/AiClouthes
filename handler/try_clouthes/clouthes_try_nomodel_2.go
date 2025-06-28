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

func TryClouthesNoModel2(c fiber.Ctx) error {
	userId := c.Locals("userId").(int)
	telephone, err := utils.IdToTelephone(userId)
	if err != nil {
		utils.Logger.Error("获取用户手机号失败: " + err.Error())
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2111,
			"msg":  "服务器出现错误，请稍后再试",
		})
	}
	var topFile, bottomFile *multipart.FileHeader
	topFile, err = c.FormFile("topGarment")
	if err != nil {
		utils.Logger.Error("上装文件获取失败: " + err.Error())
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2112,
			"msg":  "服务器出现错误，请稍后再试",
		})
	}
	bottomFile, err = c.FormFile("bottomGarment")
	if err != nil {
		utils.Logger.Error("下装文件获取失败: " + err.Error())
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2113,
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
	sex := c.FormValue("sex")
	var personURL string
	if sex == "male" {
		personURL = fmt.Sprintf("http://%s:%s/try_files/%s/%s", config.Configs.Server.Host, config.Configs.Server.Port, "model", url.PathEscape("male.png"))
	} else {
		personURL = fmt.Sprintf("http://%s:%s/try_files/%s/%s", config.Configs.Server.Host, config.Configs.Server.Port, "model", url.PathEscape("female.png"))
	}
	fmt.Println(topURL)
	fmt.Println(bottomURL)
	fmt.Println(personURL)
	if topURL == "" || bottomURL == "" || personURL == "" {
		utils.Logger.Error("文件上传失败")
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"code": 2115,
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
			"code": 2116,
			"msg":  "服务器出现错误，请稍后再试",
		})
	}
	//轮询任务状态
	var result string
	result, err = PollTaskStatus(taskID)
	if err != nil {
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2118,
			"msg":  "服务器出现错误，请稍后再试",
		})
	}
	//删除telephone_try下的所有文件
	utils.ClearFolder(path.Join("clouthesPic", telephone))
	if result != "" {
		result, err = utils.SavePic(result)
		if err != nil {
			return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
				"code": 2119,
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
			"code": 2117,
			"msg":  "服务器出现错误，请稍后再试"})
	}
}
