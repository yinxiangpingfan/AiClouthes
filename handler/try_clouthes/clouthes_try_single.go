// 文件大小：5KB ~ 5MB之间
// 分辨率：图片边长在150px ~ 4096px之间
package try_clouthes

import (
	"ai_clouthes_backed/config"
	"ai_clouthes_backed/utils"
	"bytes"
	"encoding/json"
	"fmt"
	"github.com/gofiber/fiber/v3"
	"io"
	"mime/multipart"
	"net/http"
	"net/url"
	"path"
	"time"
)

const (
	MaxRetries    = 60               // 最大重试次数
	RetryInterval = 1 * time.Second  // 重试间隔
	ApiTimeout    = 60 * time.Second // API超时
)

// 异步任务请求响应结构
type AsyncResponse struct {
	Output struct {
		TaskID     string `json:"task_id"`
		TaskStatus string `json:"task_status"`
	} `json:"output"`
	RequestID string `json:"request_id"`
}

// 任务状态响应结构
type TaskStatus struct {
	RequestID string `json:"request_id"`
	Output    struct {
		TaskID     string `json:"task_id"`
		TaskStatus string `json:"task_status"`
		ImageURL   string `json:"image_url"`
		Code       string `json:"code"`
		Message    string `json:"message"`
	} `json:"output"`
	Usage struct {
		ImageCount int `json:"image_count"`
	} `json:"usage"`
}

// 定义请求数据结构体
type ImageSynthesisRequest struct {
	Model string `json:"model"` // 使用的模型名称
	Input struct {
		TopGarmentURL    string `json:"top_garment_url"`    // 上衣图片URL
		BottomGarmentURL string `json:"bottom_garment_url"` // 下装图片URL
		PersonImageURL   string `json:"person_image_url"`   // 人物基准图片URL
	} `json:"input"`
	Parameters struct {
		Resolution  int  `json:"resolution"`   // 输出分辨率（-1表示自动）
		RestoreFace bool `json:"restore_face"` // 是否进行面部修复
	} `json:"parameters"`
}

func SubmitTaskTryClouthesDouble(p1 string, p2 string, p3 string) (string, error) {
	// 设置API端点
	url := "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/image-synthesis/"

	// 初始化请求数据
	requestBody := ImageSynthesisRequest{
		Model: "aitryon",
		Input: struct {
			TopGarmentURL    string `json:"top_garment_url"`
			BottomGarmentURL string `json:"bottom_garment_url"`
			PersonImageURL   string `json:"person_image_url"`
		}{
			TopGarmentURL:    p1,
			BottomGarmentURL: p2,
			PersonImageURL:   p3,
		},
		Parameters: struct {
			Resolution  int  `json:"resolution"`
			RestoreFace bool `json:"restore_face"`
		}{
			Resolution:  -1,
			RestoreFace: true,
		},
	}

	// 将结构体转换为JSON
	jsonData, err := json.Marshal(requestBody)
	if err != nil {
		utils.Logger.Error("JSON编码失败: " + err.Error())
		return "", fmt.Errorf("JSON编码失败: " + err.Error())
	}
	// 创建HTTP请求
	req, e := http.NewRequest("POST", url, bytes.NewBuffer(jsonData))
	if e != nil {
		utils.Logger.Error("创建请求失败: " + e.Error())
		return "", fmt.Errorf("创建请求失败:" + e.Error())
	}

	// 设置请求头
	req.Header.Set("X-DashScope-Async", "enable") // 启用异步模式
	token := fmt.Sprintf("Bearer %s", config.Configs.Vivo.Aliyun)
	req.Header.Set("Authorization", token)
	req.Header.Set("Content-Type", "application/json")

	// 发送HTTP请求
	client := &http.Client{}
	resp, e1 := client.Do(req)
	if e1 != nil {
		utils.Logger.Error("发送请求失败: " + e1.Error())
		return "", fmt.Errorf("请求失败: " + e1.Error())
	}
	defer resp.Body.Close()
	body, _ := io.ReadAll(resp.Body)
	//fmt.Printf("API响应内容: %s\n", string(body))
	if resp.StatusCode != http.StatusOK {
		utils.Logger.Error("非预期状态码: " + fmt.Sprintf("%d", resp.StatusCode))
		return "", fmt.Errorf("非预期状态码: %d", resp.StatusCode)
	}
	var asyncResp AsyncResponse
	if err := json.NewDecoder(bytes.NewReader(body)).Decode(&asyncResp); err != nil { // 使用缓存body
		utils.Logger.Error("响应解析失败: " + err.Error())
		return "", fmt.Errorf("响应解析失败: " + err.Error())
	}
	return asyncResp.Output.TaskID, nil
}

// 获取单个任务状态
func GetTaskStatus(client *http.Client, apiKey, taskID string) (*TaskStatus, error) {
	url := fmt.Sprintf("https://dashscope.aliyuncs.com/api/v1/tasks/%s", taskID)
	token := fmt.Sprintf("Bearer %s", apiKey)
	req, _ := http.NewRequest("GET", url, nil)
	req.Header.Set("Authorization", token)
	req.Header.Set("Accept", "application/json")
	resp, err := client.Do(req)
	if err != nil {
		utils.Logger.Error("状态查询失败: " + err.Error())
		return nil, fmt.Errorf("状态查询失败: " + err.Error())
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		utils.Logger.Error("非预期状态码: " + fmt.Sprintf("%d", resp.StatusCode))
		return nil, fmt.Errorf("非预期状态码: %d", resp.StatusCode)
	}

	body, _ := io.ReadAll(resp.Body) // 先读取body内容

	var status TaskStatus
	if err := json.NewDecoder(bytes.NewReader(body)).Decode(&status); err != nil { // 使用缓存body
		utils.Logger.Error("状态解析失败: " + err.Error())
		return nil, fmt.Errorf("状态解析失败:" + err.Error())
	}

	return &status, nil
}

// 轮询任务状态
func PollTaskStatus(taskID string) (string, error) {
	apiKey := config.Configs.Vivo.Aliyun
	client := &http.Client{Timeout: ApiTimeout}

	for i := 0; i < MaxRetries; i++ {
		status, err := GetTaskStatus(client, apiKey, taskID)
		if err != nil {
			return "", fmt.Errorf("获取单个任务状态失败: " + err.Error())
		}
		switch status.Output.TaskStatus {
		case "SUCCEEDED":
			return status.Output.ImageURL, nil
		case "FAILED":
			utils.Logger.Error("任务失败: " + status.Output.Code + " - " + status.Output.Message)
			return "", fmt.Errorf("任务失败: %s - %s", status.Output.Code, status.Output.Message)
		case "PENDING", "PRE-PROCESSING", "RUNNING", "POST-PROCESSING":
			time.Sleep(RetryInterval)
		default:
			fmt.Printf("未知状态: %s\n", status.Output.TaskStatus)
			utils.Logger.Error("未知状态: " + status.Output.TaskStatus)
			return "", fmt.Errorf("未知状态: %s", status.Output.TaskStatus)
		}
	}
	utils.Logger.Error("任务: " + taskID + "超时")
	return "", fmt.Errorf("任务超时")
}

func TryClouthesSingle(c fiber.Ctx) error {
	userId := c.Locals("userId").(int)
	telephone, err := utils.IdToTelephone(userId)
	if err != nil {
		utils.Logger.Error("获取用户手机号失败: " + err.Error())
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2071,
			"msg":  "服务器出现错误，请稍后再试",
		})
	}
	// 处理三个文件上传
	var topFile, personFile *multipart.FileHeader
	topFile, err = c.FormFile("topGarment")
	if err != nil {
		utils.Logger.Error("上装文件获取失败: " + err.Error())
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2072,
			"msg":  "服务器出现错误，请稍后再试",
		})
	}

	personFile, err = c.FormFile("personImage")
	if err != nil {
		utils.Logger.Error("模特文件获取失败: " + err.Error())
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2074,
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
	personURL := saveAndGetURL(personFile, 3)
	fmt.Println(topURL)
	fmt.Println(personURL)
	if topURL == "" || personURL == "" {
		utils.Logger.Error("文件上传失败")
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"code": 2075,
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
			"code": 2076,
			"msg":  "服务器出现错误，请稍后再试",
		})
	}
	//轮询任务状态
	var result string
	result, err = PollTaskStatus(taskID)
	if err != nil {
		return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2078,
			"msg":  "服务器出现错误，请稍后再试",
		})
	}
	//删除telephone_try下的所有文件
	utils.ClearFolder(path.Join("clouthesPic", telephone))
	if result != "" {
		result, err = utils.SavePic(result)
		if err != nil {
			return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
				"code": 2079,
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
			"code": 2077,
			"msg":  "服务器出现错误，请稍后再试"})

	}
}
