#!/bin/bash

# 设置目标平台
export GOOS=linux
export GOARCH=amd64  # 根据目标架构调整

# 使用 musl 工具链
export CC=x86_64-linux-musl-gcc
export CXX=x86_64-linux-musl-g++
export CGO_ENABLED=1

# 添加必要的链接标志
export LDFLAGS="-linkmode external -extldflags -static"

# 编译项目（替换 your/main/package）
go build -ldflags "$LDFLAGS" -o your-app-linux