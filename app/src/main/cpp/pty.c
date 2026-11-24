#include <jni.h>
#include <pty.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/ioctl.h>
#include <termios.h>
#include <android/log.h>

#define TAG "PTY"

JNIEXPORT jint JNICALL
Java_com_kishanmulkalapal_tempv_Pty_createSubprocess(
        JNIEnv *env, jclass clazz, jstring jcmd, jstring jcwd, jint rows, jint cols) {

    const char *cmd = (*env)->GetStringUTFChars(env, jcmd, NULL);
    const char *cwd = (*env)->GetStringUTFChars(env, jcwd, NULL);

    struct winsize ws = { .ws_row = (unsigned short) rows,
                          .ws_col = (unsigned short) cols };
    int master_fd = -1;
    pid_t pid = forkpty(&master_fd, NULL, NULL, &ws);

    if (pid == 0) {
        chdir(cwd);
        setenv("TERM", "xterm-256color", 1);
        setenv("HOME", cwd, 1);
        execl(cmd, cmd, "-", (char *) NULL);
        _exit(127);
    }

    __android_log_print(ANDROID_LOG_DEBUG, TAG, "forkpty pid=%d fd=%d", pid, master_fd);
    (*env)->ReleaseStringUTFChars(env, jcwd, cwd);
    (*env)->ReleaseStringUTFChars(env, jcmd, cmd);
    return master_fd;
}

JNIEXPORT void JNICALL
Java_com_kishanmulkalapal_tempv_Pty_resize(
        JNIEnv *env, jclass clazz, jint fd, jint rows, jint cols) {
    struct winsize ws = { .ws_row = (unsigned short) rows,
                          .ws_col = (unsigned short) cols };
    ioctl(fd, TIOCSWINSZ, &ws);
}
