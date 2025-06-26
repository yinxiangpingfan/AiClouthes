package clouthes_recommend

import (
	"ai_clouthes_backed/config"
	"ai_clouthes_backed/database"
	"ai_clouthes_backed/utils"
	"bufio"
	"encoding/json"
	"fmt"
	"os"
	"path"
	"path/filepath"

	"github.com/dingdinglz/openai"
	"github.com/gofiber/fiber/v3"
)

// 解析图片
func ClouthesParseImages(ctx fiber.Ctx) error {
	//解析图片
	userId := ctx.Locals("userId").(int)
	telephone, err := utils.IdToTelephone(userId)
	var text string
	text = ctx.FormValue("ques")
	if err != nil {
		utils.Logger.Error("解析图片时，获取用户手机号失败" + err.Error())
		return nil
	}
	var imageContents []openai.VisionContent
	err = add(path.Join("parsePic", telephone), &imageContents)
	if err != nil {
		utils.Logger.Error("解析图片时，获取用户图片失败" + err.Error())
		return nil
	}
	sy := fmt.Sprintf("请严格解析用户提供的图片（共 [X] 张），按图片顺序逐一列出每张图片中的所有可清晰辨认衣物。格式为：\n第一张图片中包含的衣服有：\n1.[衣物名称]（颜色、款式、材质、关键特征如领型/袖长/图案等）\n2.[衣物名称]（同上具体描述）\n...\n第二张图片中包含的衣服有：\n1.[衣物名称]（具体描述）\n...（按实际图片数量继续）。要求衣物名称准确具体，描述仅基于图片可见信息。\n\n然后，根据用户本次目的：%s，从上述分析出的所有衣物中选择一套可组合穿着的搭配进行推荐。格式为：\n根据您的需求，我推荐您穿：\n1.[推荐衣物1名称]\n2.[推荐衣物2名称]\n...\n原因：[说明选择原因，必须包含：1. 场合匹配性分析 2. 单品间搭配兼容性 3. 放弃其他选项的关键理由]。\n\n全局要求：\n1. 输出必须且仅包含以上两部分指定格式内容（衣物分析列表 + 推荐列表 + 原因段落），绝对不要添加任何其他文字（如问候语、总结、表情符号）。\n2. 衣物分析必须忠实于图片内容，[X]需替换为实际图片数量。\n3. 推荐搭配必须符合常理：不能同时推荐两件需外穿的单品（如两件外套），确保有上衣和下装组合，季节/场合需合理（如不推荐羽绒服参加夏季婚礼）。\n4. 衣物分析中括号()内必须直接填写具体描述内容，不要写“具体描述”字样。\n5. 推荐衣物必须且只能来自第一步分析列出的衣物列表。违者将导致功能失效。", text)
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
	ctx.Set("Cache-Control", "no-cache, no-transform")
	ctx.Set("Connection", "keep-alive")
	ctx.Set("X-Accel-Buffering", "no")
	ctx.Set("Transfer-Encoding", "chunked")
	ctx.Set("Access-Control-Allow-Origin", "*")
	ctx.Set("Access-Control-Allow-Headers", "Cache-Control")
	ctx.Set("Access-Control-Allow-Credentials", "true")
	ctx.Response().SetBodyStreamWriter(func(w *bufio.Writer) {
		defer func() {
			w.Flush()
		}()
		if err != nil {
			utils.Logger.Error("解析图片时，AI推荐失败" + err.Error())
			// 发送错误消息
			w.Write([]byte("event: message\n"))
			w.Write([]byte("data: {\"data\":\"错误了\"}\n\n"))
			w.Write([]byte("event: message\n"))
			w.Write([]byte("data: {\"data\":\"" + err.Error() + "\"}\n\n"))
			w.Flush()
		}
		err = client.ChatVisionStream("Qwen/Qwen2.5-VL-32B-Instruct", []openai.VisionMessage{
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
			w.Write([]byte("event: message\n"))
			w.Write([]byte("data: " + string(massgaeJson) + "\n\n"))
			w.Flush()
		})

		// 流式输出完成后发送结束标记
		if err == nil {
			w.Write([]byte("event: message\n"))
			w.Write([]byte("data: [DONE]\n\n"))
			w.Flush()
		}
		if err != nil {
			utils.Logger.Error("解析图片时，AI推荐失败" + err.Error())
			// 发送错误消息
			w.Write([]byte("event: message\n"))
			w.Write([]byte("data: {\"data\":\"错误了\"}\n\n"))
			w.Write([]byte("event: message\n"))
			w.Write([]byte("data: {\"data\":\"" + err.Error() + "\"}\n\n"))
			w.Flush()
		}
		//将回答保存到数据库
		_, e11 := database.Engine.Where("id = ?", userId).Update(&database.User{
			Temp: answer,
		})
		if e11 != nil {
			utils.Logger.Error("解析图片时，保存回答到数据库失败" + e11.Error())
		}
	})
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
