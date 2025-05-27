package router

import (
	"ai_clouthes_backed/handler"
	"ai_clouthes_backed/utils"
	"github.com/gofiber/fiber/v3"
)

func Router(app *fiber.App) {
	app.Post("/login", handler.UserLogin)
	app.Post("/register", handler.UserRegister)
	user := app.Group("/user", utils.Middle)
	user.Post("/chanpass", handler.ChangePassword)
	user.Get("/logout", handler.UserLogout)

}
