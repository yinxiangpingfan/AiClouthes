package clouthes_recommend

import (
	"ai_clouthes_backed/config"
	"ai_clouthes_backed/database"
	"ai_clouthes_backed/utils"
	"encoding/json"
	"fmt"
	"github.com/dingdinglz/openai"
	"github.com/gofiber/fiber/v3"
	"os"
	"path"
	"path/filepath"
)

// 解析图片
func ClouthesParseImages(ctx fiber.Ctx) error {
	//解析图片
	userId := ctx.Locals("userId").(int)
	telephone, err := utils.IdToTelephone(userId)
	text := ctx.FormValue("ques")
	if err != nil {
		utils.Logger.Error("解析图片时，获取用户手机号失败" + err.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2021,
			"msg":  "服务器出现错误，请稍后再试",
		})
	}
	var imageContents []openai.VisionContent
	err = add(path.Join("parsePic", telephone), &imageContents)
	if err != nil {
		utils.Logger.Error("解析图片时，获取用户图片失败" + err.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2022,
			"msg":  "服务器出现错误，请稍后再试",
		})
	}
	sy := fmt.Sprintf("解析上方几张图片，分析其中包含的衣服并输出，并根据%s的目的从刚刚识别的衣服中选择一套衣服（也可以是几件）。请按照以下格式输出：\n例如：第一张图片中包含的衣服有：1.黑色西服外套（具体描述这个衣服）\n2.白色衬衫（具体描述这个衣服）\n第三张图片中包含的衣服有：1.红色衬衫（具体描述这个衣服）\n2.蓝色裤子（具体描述这个衣服）\n根据您的需求，我推荐您穿：1.黑色西服外套\n2.红色衬衫\n3.蓝色裤子\n原因：说明选择几件衣服的原因\n\n请按照这个格式输出，必须要按照这个格式不要输出其他内容。注意，括号里面的具体描述这个衣服，直接在括号里面输出描述，要输出描述的内容，而不是输出具体描述这个衣服这几个字。注意：一定要从实际出发，图片数量以及衣物要根据实际图片来输出。在推荐衣服的时候要注意衣服在穿的时候不能冲突，要符合常理。", text)
	imageContents = append(imageContents, openai.VisionContent{
		Type: openai.VISION_MESSAGE_TEXT,
		Text: sy,
	})
	answer := ""
	client := openai.NewClient(&openai.ClientConfig{
		BaseUrl: "https://api.siliconflow.cn/v1",
		ApiKey:  config.Configs.Vivo.Aliguijikey,
	})
	ctx.Set("Content-Type", "text/event-stream")
	ctx.Set("Cache-Control", "no-cache")
	ctx.Set("Connection", "keep-alive")
	ctx.Set("x-Accel-Buffering", "no")
	writer := ctx.Response().BodyWriter()
	err = client.ChatVisionStream("Pro/Qwen/Qwen2.5-VL-7B-Instruct", []openai.VisionMessage{
		{
			Role:    "user",
			Content: imageContents,
		},
	}, func(s string) {
		answer += s
		message := map[string]string{
			"data": s,
		}
		massgaeJson, _ := json.Marshal(message)
		// 发送 SSE 事件
		writer.Write([]byte("event: message\n"))
		writer.Write([]byte("data: " + string(massgaeJson) + "\n\n"))
		if f, ok := writer.(interface{ Flush() error }); ok {
			f.Flush()
		}
	})
	if err != nil {
		utils.Logger.Error("解析图片时，AI推荐失败" + err.Error())
		// 发送错误消息
		writer.Write([]byte("event: message\n"))
		writer.Write([]byte("data: {\"data\":\"错误了\"}\n\n"))
		writer.Write([]byte("event: message\n"))
		writer.Write([]byte("data: {\"data\":\"" + err.Error() + "\"}\n\n"))
		if f, ok := writer.(interface{ Flush() error }); ok {
			f.Flush()
		}
	}
	//将回答保存到数据库
	_, e11 := database.Engine.Where("id = ?", userId).Update(&database.User{
		Temp: answer,
	})
	if e11 != nil {
		utils.Logger.Error("解析图片时，保存回答到数据库失败" + e11.Error())
	}
	return nil
}

func add(folderPath string, imageContent *[]openai.VisionContent) error {
	// 捕获 Walk 函数的返回值作为最终错误
	err := filepath.Walk(folderPath, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return fmt.Errorf("访问路径 %q 失败: %w", path, err)
		}
		if path == folderPath {
			return nil
		}

		var removeErr error
		switch {
		case info.Mode().IsRegular():
			pic, err := os.ReadFile(path)
			if err != nil {
				return fmt.Errorf("读取图片失败: %w", err)
			}
			*imageContent = append(*imageContent, openai.VisionContent{
				Type: openai.VISION_MESSAGE_IMAGE_URL,
				ImageUrl: &openai.VisionContentImageUrl{
					Url: openai.GenerateImageUrlBase64(pic),
				},
			})
		case info.Mode().IsDir():
			return fmt.Errorf("文件夹 %q 不支持", path)
		}

		if removeErr != nil {
			return fmt.Errorf("AI荐衣获取图片失败: %w", removeErr)
		}
		return nil
	})

	if err != nil {
		return fmt.Errorf("AI荐衣获取图片失败: %w", err)
	}
	return nil
}
