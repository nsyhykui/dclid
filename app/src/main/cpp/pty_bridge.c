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

// ★ 核心新增：通过 master_fd 控制终端的回显开关
JNIEXPORT void JNICALL
Java_com_nsyhykui_dclid_shell_PtyBridge_setEcho(
        JNIEnv *env, jobject thiz, jint fd, jboolean enable) {
    struct termios term;
    if (tcgetattr(fd, &term) == 0) {
        if (enable) {
            term.c_lflag |= ECHO;
        } else {
            term.c_lflag &= ~ECHO;
        }
        tcsetattr(fd, TCSANOW, &term);
    }
}
