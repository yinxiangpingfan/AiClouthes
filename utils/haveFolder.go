package utils

import "os"

func HaveFloder(path string) error {
	_, err := os.Stat(path)
	if err != nil {
		err = os.Mkdir(path, 0755)
		if err != nil {
			return err
		}
		return nil
	} else {
		return nil
	}
}
