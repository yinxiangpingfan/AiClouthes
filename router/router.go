package router

import (
	"ai_clouthes_backed/handler"
	"ai_clouthes_backed/utils"
	"github.com/gofiber/fiber/v3"
	"github.com/gofiber/fiber/v3/middleware/static"
	"path"
)

func Router(app *fiber.App) {
	app.Post("/login", handler.UserLogin)
	app.Post("/register", handler.UserRegister)
	app.Get("/try_files*", static.New(path.Join(".", "clouthesPic")))
	user := app.Group("/user", utils.Middle)
	user.Post("/chanpass", handler.ChangePassword)
	user.Get("/logout", handler.UserLogout)
	user.Post("/pic/upload", handler.UploadImages)
	user.Post("/pic/parse", handler.ClouthesParseImages)
	user.Get("/pic/makepic", handler.ClouthesMakeImages)
	user.Post("/weather/get", handler.GetWeather)
	user.Post("/weather/make", handler.MakeWeatherClouthes)
	user.Post("/weather/makepic", handler.GetWeatherMakePic)
	user.Post("/try/double", handler.TryClouthesDouble)
	user.Post("/try/single", handler.TryClouthesSingle)
	user.Post("/try/nomodel1", handler.TryClouthesNoModel1)
	user.Post("/try/nomodel2", handler.TryClouthesNoModel2)
}
