package utils

import (
	"net/url"
	"strings"
)

func MakeUrl(params map[string]string) string {
	sb := strings.Builder{}
	sb.WriteString("?")
	for k, v := range params {
		sb.WriteString(url.QueryEscape(k))
		sb.WriteString("=")
		sb.WriteString(url.QueryEscape(v))
		sb.WriteString("&")
	}
	if len(sb.String()) > 0 {
		return sb.String()[:sb.Len()-1]
	} else {
		return ""
	}
}
