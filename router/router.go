package router

import (
	"ai_clouthes_backed/handler/clouthes_recommend"
	"ai_clouthes_backed/handler/clouthes_weather"
	"ai_clouthes_backed/handler/try_clouthes"
	"ai_clouthes_backed/handler/users"
	"ai_clouthes_backed/utils"
	"path"

	"github.com/gofiber/fiber/v3"
	"github.com/gofiber/fiber/v3/middleware/static"
)

func Router(app *fiber.App) {
	// 静态文件服务
	app.Get("/frontend/*", static.New(path.Join(".", "frontend")))
	app.Get("/try_files*", static.New(path.Join(".", "clouthesPic")))

	// 前端页面路由
	app.Get("/", func(c fiber.Ctx) error {
		return c.SendFile("./frontend/index.html")
	})
	app.Get("/login", func(c fiber.Ctx) error {
		return c.SendFile("./frontend/login.html")
	})
	app.Get("/weather", func(c fiber.Ctx) error {
		return c.SendFile("./frontend/weather.html")
	})
	app.Get("/wardrobe", func(c fiber.Ctx) error {
		return c.SendFile("./frontend/wardrobe.html")
	})
	app.Get("/try-on", func(c fiber.Ctx) error {
		return c.SendFile("./frontend/try-on.html")
	})
	app.Get("/profile", func(c fiber.Ctx) error {
		return c.SendFile("./frontend/profile.html")
	})

	// API路由
	app.Post("/login", users.UserLogin)
	app.Post("/register", users.UserRegister)

	// 需要认证的用户路由
	user := app.Group("/user", utils.Middle)
	user.Post("/chanpass", users.ChangePassword)
	user.Get("/logout", users.UserLogout)
	user.Post("/pic/upload", clouthes_recommend.UploadImages)
	user.Post("/pic/parse", clouthes_recommend.ClouthesParseImages)
	user.Get("/pic/makepic", clouthes_recommend.ClouthesMakeImages)
	user.Post("/weather/get", clouthes_weather.GetWeather)
	user.Post("/weather/make", clouthes_weather.MakeWeatherClouthes)
	user.Get("/weather/makepic", clouthes_weather.GetWeatherMakePic)
	user.Post("/try/double", try_clouthes.TryClouthesDouble)
	user.Post("/try/single", try_clouthes.TryClouthesSingle)
	user.Post("/try/nomodel1", try_clouthes.TryClouthesNoModel1)
	user.Post("/try/nomodel2", try_clouthes.TryClouthesNoModel2)
}
