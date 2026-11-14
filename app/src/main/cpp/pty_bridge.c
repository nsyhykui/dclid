#include <jni.h>
#include <pty.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <termios.h>
#include <fcntl.h>

JNIEXPORT jint JNICALL
Java_com_nsyhykui_dclid_shell_PtyBridge_createSubprocess(
        JNIEnv *env, jobject thiz, jint rows, jint cols) {
    
    struct winsize sz;
    sz.ws_row = rows; sz.ws_col = cols;
    sz.ws_xpixel = 0; sz.ws_ypixel = 0;

    int master_fd;
    pid_t pid = forkpty(&master_fd, NULL, NULL, &sz);

    if (pid == 0) {
        execl("/system/bin/sh", "sh", "-i", NULL);
        _exit(1);
    } else if (pid > 0) {
        int flags = fcntl(master_fd, F_GETFL, 0);
        fcntl(master_fd, F_SETFL, flags | O_NONBLOCK);
        return master_fd;
    }
    return -1;
}

JNIEXPORT void JNICALL
Java_com_nsyhykui_dclid_shell_PtyBridge_setPtyWindowSize(
        JNIEnv *env, jobject thiz, jint fd, jint rows, jint cols) {
    struct winsize sz;
    sz.ws_row = rows; sz.ws_col = cols;
    sz.ws_xpixel = 0; sz.ws_ypixel = 0;
    ioctl(fd, TIOCSWINSZ, &sz);
}
