package database

import (
	"fmt"
	"time"

	"github.com/go-xorm/xorm"
	_ "github.com/mattn/go-sqlite3"
)

// 用于连接数据库,并把根据结构体创建表结构
var Engine *xorm.Engine

type User struct {
	Id        int64  `xorm:"pk autoincr"`
	Telephone string `xorm:"VARCHAR(11)"`
	Password  string `xorm:"VARCHAR(255)"`
	Sex       string
	Temp      string    `xorm:"TEXT"`
	CreatedAt time.Time `xorm:"created"`
	UpdatedAt time.Time `xorm:"updated"`
}

func OpenDatabase() {
	var err error
	Engine, err = xorm.NewEngine("sqlite3", "./test.db")
	if err != nil {
		panic("数据库连接失败: " + err.Error())
	}
	err = Engine.Sync(new(User))
	if err != nil {
		panic("表结构体创建失败: " + err.Error())
	}
	fmt.Println("数据库连接成功")
}
