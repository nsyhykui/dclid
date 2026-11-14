cmake_minimum_required(VERSION 3.18)
project(dclid C)

# 1. PTY 动态库
add_library(pty_bridge SHARED pty_bridge.c)
target_link_libraries(pty_bridge log)

# 2. 伪装成 .so 的可执行文件（使用 add_executable 保证正常初始化）
add_executable(dcli_bin dcli_main.c)

# 3. 编译完成后，自动把它复制到 jniLibs 目录并加上 lib 前缀
add_custom_command(TARGET dcli_bin POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy
    $<TARGET_FILE:dcli_bin>
    ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libdcli_bin.so
)
