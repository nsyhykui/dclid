cmake_minimum_required(VERSION 3.18)
project(dclid C)

# 动态库
add_library(pty_bridge SHARED pty_bridge.c)

# 伪装的可执行文件
add_executable(dcli_bin dcli_main.c)
set_target_properties(dcli_bin PROPERTIES SUFFIX ".so")
