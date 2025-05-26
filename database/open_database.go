package database

import (
	"ai_clouthes_backed/config"
	"fmt"
	_ "github.com/go-sql-driver/mysql"
	"github.com/go-xorm/xorm"
)

// 用于连接数据库,并把根据结构体创建表结构
var Engine *xorm.Engine

func OpenDatabase(e *xorm.Engine) {
	var err error
	mys := fmt.Sprintf("%s:%s@tcp(%s:%s)/%s?charset=utf8", config.Configs.Database.Username, config.Configs.Database.Password, config.Configs.Database.Host, config.Configs.Database.Port, config.Configs.Database.Dbname)
	e, err = xorm.NewEngine("mysql", mys)
	if err != nil {
		panic("数据库连接失败: " + err.Error())
	}

}
