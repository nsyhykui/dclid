cmake_minimum_required(VERSION 3.18)
project(dclid C)

add_library(pty_bridge SHARED pty_bridge.c)
target_link_libraries(pty_bridge log)

# 核心修复：伪装成动态库，并强行把入口点指向 main 函数
add_library(dcli_bin SHARED dcli_main.c)
set_target_properties(dcli_bin PROPERTIES 
    SUFFIX ".so"
    OUTPUT_NAME "dcli_bin"
    LINK_FLAGS "-Wl,-e,main"
)
