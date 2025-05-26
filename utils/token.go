package utils

import (
	"ai_clouthes_backed/config"
	"fmt"
	"github.com/golang-jwt/jwt/v5"
	"time"
)

type JwtCustomClaims struct {
	ID int `json:"id"`
	jwt.RegisteredClaims
}

// 生成token
func Gentoken(id int) (string, error) {
	claims := JwtCustomClaims{
		ID: id,
		RegisteredClaims: jwt.RegisteredClaims{
			Issuer:    "ai_clothes",
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(24 * time.Hour)),
		},
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	tokenString, err := token.SignedString([]byte(config.Configs.Cookie.Secret))
	if err != nil {
		return "", err
	}
	return tokenString, nil
}

// 解析token
func ParseToken(tokenString string) (int, error) {
	token, err := jwt.ParseWithClaims(tokenString, &JwtCustomClaims{}, func(token *jwt.Token) (interface{}, error) {
		return []byte(config.Configs.Cookie.Secret), nil
	})
	if err != nil {
		//刷新cookie
		Logger.Error("解析token失败" + err.Error())
		return -1, err
	}
	if claims, ok := token.Claims.(*JwtCustomClaims); ok && token.Valid {
		return claims.ID, nil
	}
	return -1, fmt.Errorf("token失效")
}
