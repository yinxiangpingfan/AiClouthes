package utils

import (
	"fmt"
	"os"
	"path/filepath"
)

// 清空文件夹
func ClearFolder(folderPath string) error {
	// 捕获 Walk 函数的返回值作为最终错误
	err := filepath.Walk(folderPath, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return fmt.Errorf("访问路径 %q 失败: %w", path, err)
		}
		if path == folderPath {
			return nil
		}

		var removeErr error
		switch {
		case info.Mode().IsRegular():
			removeErr = os.Remove(path)
		case info.Mode().IsDir():
			removeErr = os.RemoveAll(path)
		}

		if removeErr != nil {
			return fmt.Errorf("删除 %q 失败: %w", path, removeErr)
		}
		return nil
	})

	if err != nil {
		return fmt.Errorf("清空文件夹失败: %w", err)
	}
	return nil
}
