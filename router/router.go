package router

import (
	"ai_clouthes_backed/handler/clouthes_recommend"
	"ai_clouthes_backed/handler/clouthes_weather"
	"ai_clouthes_backed/handler/try_clouthes"
	"ai_clouthes_backed/handler/users"
	"ai_clouthes_backed/utils"
	"github.com/gofiber/fiber/v3"
	"github.com/gofiber/fiber/v3/middleware/static"
	"path"
)

func Router(app *fiber.App) {
	app.Post("/login", users.UserLogin)
	app.Post("/register", users.UserRegister)
	app.Get("/try_files*", static.New(path.Join(".", "clouthesPic")))
	user := app.Group("/user", utils.Middle)
	user.Post("/chanpass", users.ChangePassword)
	user.Get("/logout", users.UserLogout)
	user.Post("/pic/upload", clouthes_recommend.UploadImages)
	user.Post("/pic/parse", clouthes_recommend.ClouthesParseImages)
	user.Get("/pic/makepic", clouthes_recommend.ClouthesMakeImages)
	user.Post("/weather/get", clouthes_weather.GetWeather)
	user.Post("/weather/make", clouthes_weather.MakeWeatherClouthes)
	user.Post("/weather/makepic", clouthes_weather.GetWeatherMakePic)
	user.Post("/try/double", try_clouthes.TryClouthesDouble)
	user.Post("/try/single", try_clouthes.TryClouthesSingle)
	user.Post("/try/nomodel1", try_clouthes.TryClouthesNoModel1)
	user.Post("/try/nomodel2", try_clouthes.TryClouthesNoModel2)
}
