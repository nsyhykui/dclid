#include <jni.h>
JNIEXPORT jstring JNICALL Java_com_nsyhykui_dclid_MainActivity_getTestString(JNIEnv *env, jobject thiz) {
    return (*env)->NewStringUTF(env, "PTY Ready");
}
