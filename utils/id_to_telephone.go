package utils

import (
	"ai_clouthes_backed/database"
	"fmt"
)

// 通过id获取手机号
func IdToTelephone(id int) (string, error) {
	//查询数据库，获取手机号
	user := new(database.User)
	has, err := database.Engine.Where("id =?", id).Get(user)
	if !has {
		return "", fmt.Errorf("服务器出现错误，请稍后再试")
	} else {
		if err != nil {
			return "", err
		} else {
			return user.Telephone, nil
		}
	}
}
