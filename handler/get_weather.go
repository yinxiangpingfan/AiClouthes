package handler

import (
	"ai_clouthes_backed/config"
	"ai_clouthes_backed/utils"
	"encoding/json"
	"fmt"
	"github.com/gofiber/fiber/v3"
	"io"
	"net/http"
)

type JSONData struct {
	Status    string      `json:"status"`
	Count     string      `json:"count"`
	Info      string      `json:"info"`
	Infocode  string      `json:"infocode"`
	Forecasts []Forecasts `json:"forecasts"`
}
type Casts struct {
	Date           string `json:"date"`
	Week           string `json:"week"`
	Dayweather     string `json:"dayweather"`
	Nightweather   string `json:"nightweather"`
	Daytemp        string `json:"daytemp"`
	Nighttemp      string `json:"nighttemp"`
	Daywind        string `json:"daywind"`
	Nightwind      string `json:"nightwind"`
	Daypower       string `json:"daypower"`
	Nightpower     string `json:"nightpower"`
	DaytempFloat   string `json:"daytemp_float"`
	NighttempFloat string `json:"nighttemp_float"`
}
type Forecasts struct {
	City       string  `json:"city"`
	Adcode     string  `json:"adcode"`
	Province   string  `json:"province"`
	Reporttime string  `json:"reporttime"`
	Casts      []Casts `json:"casts"`
}

func GetWeather(ctx fiber.Ctx) error {
	code := ctx.FormValue("code")
	randomUrl := fmt.Sprintf("https://restapi.amap.com/v3/weather/weatherInfo?city=%s&extensions=all&key=%s", code, config.Configs.Vivo.Gaode)
	req, err := http.NewRequest("GET", randomUrl, nil)
	if err != nil {
		utils.Logger.Error("获取天气失败" + err.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2041,
			"msg":  "获取天气失败，请稍后再试",
		})
	}

	resp, e := http.DefaultClient.Do(req)
	if e != nil {
		utils.Logger.Error("获取天气失败" + e.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2042,
			"msg":  "获取天气失败，请稍后再试",
		})
	}
	defer resp.Body.Close()

	body, errs := io.ReadAll(resp.Body)
	if errs != nil {
		utils.Logger.Error("获取天气失败" + errs.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2043,
			"msg":  "获取天气失败，请稍后再试",
		})
	}
	var data JSONData
	err = json.Unmarshal(body, &data)
	if err != nil {
		utils.Logger.Error("获取天气失败,转义json失败" + err.Error())
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2044,
			"msg":  "获取天气失败，请稍后再试",
		})
	}
	if data.Status != "1" {
		utils.Logger.Error("获取天气失败,获取天气失败" + data.Info)
		return ctx.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
			"code": 2045,
			"msg":  "获取天气失败，请稍后再试",
		})
	}
	return ctx.Status(fiber.StatusOK).JSON(fiber.Map{
		"code": 1000,
		"msg":  "获取天气成功",
		"data": data,
	})
}
