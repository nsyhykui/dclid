#include <jni.h>
#include <pty.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <termios.h>
#include <fcntl.h>
#include <stdlib.h>
#include <string.h>

JNIEXPORT jint JNICALL
Java_com_nsyhykui_dclid_shell_PtyBridge_createSubprocess(
        JNIEnv *env, jobject thiz, jint rows, jint cols, jstring cmdStr) {
    struct winsize sz;
    sz.ws_row = rows; sz.ws_col = cols; sz.ws_xpixel = 0; sz.ws_ypixel = 0;
    int master_fd;
    pid_t pid = forkpty(&master_fd, NULL, NULL, &sz);

    if (pid == 0) {
        // ★ 核心修复：在子进程 exec 之前，强行关闭终端回显
        struct termios t;
        tcgetattr(0, &t);
        t.c_lflag &= ~ECHO;
        tcsetattr(0, TCSANOW, &t);

        const char *cmd = (*env)->GetStringUTFChars(env, cmdStr, 0);
        execl("/system/bin/sh", "sh", "-c", cmd, NULL);
        _exit(1);
    } else if (pid > 0) {
        return master_fd;
    }
    return -1;
}

JNIEXPORT void JNICALL
Java_com_nsyhykui_dclid_shell_PtyBridge_setPtyWindowSize(
        JNIEnv *env, jobject thiz, jint fd, jint rows, jint cols) {
    struct winsize sz;
    sz.ws_row = rows; sz.ws_col = cols; sz.ws_xpixel = 0; sz.ws_ypixel = 0;
    ioctl(fd, TIOCSWINSZ, &sz);
}
