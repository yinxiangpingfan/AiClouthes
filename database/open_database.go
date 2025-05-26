package database

import (
	"ai_clouthes_backed/config"
	"fmt"
	_ "github.com/go-sql-driver/mysql"
	"github.com/go-xorm/xorm"
)

// 用于连接数据库,并把根据结构体创建表结构
var Engine *xorm.Engine

type User struct {
	Id        int64  `xorm:"pk autoincr"`
	Username  string `xorm:"VARCHAR(20)"`
	Telephone string `xorm:"VARCHAR(11)"`
	Password  string `xorm:"VARCHAR(255)"`
}

func OpenDatabase(e *xorm.Engine) {
	var err error
	mys := fmt.Sprintf("%s:%s@tcp(%s:%s)/%s?charset=utf8", config.Configs.Database.Username, config.Configs.Database.Password, config.Configs.Database.Host, config.Configs.Database.Port, config.Configs.Database.Dbname)
	e, err = xorm.NewEngine("mysql", mys)
	if err != nil {
		panic("数据库连接失败: " + err.Error())
	}
	err = e.Sync(new(User))
	if err != nil {
		panic("表结构体创建失败: " + err.Error())
	}
	fmt.Println("数据库连接成功")
}
