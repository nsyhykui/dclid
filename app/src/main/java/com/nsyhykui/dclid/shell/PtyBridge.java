package com.nsyhykui.dclid.shell;

import android.os.ParcelFileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class PtyBridge {
    static { System.loadLibrary("pty_bridge"); }
    public static native int createSubprocess(int rows, int cols, String cmdStr);
    public static native void setPtyWindowSize(int fd, int rows, int cols);

    public static class PtySession {
        public final int fd;
        public final FileInputStream inputStream;
        public final FileOutputStream outputStream;
        private final ParcelFileDescriptor pfd;

        public PtySession(int fd) {
            this.fd = fd;
            this.pfd = ParcelFileDescriptor.adoptFd(fd);
            this.inputStream = new FileInputStream(pfd.getFileDescriptor());
            this.outputStream = new FileOutputStream(pfd.getFileDescriptor());
        }
        public void close() {
            try { inputStream.close(); } catch (Exception ignored) {}
            try { outputStream.close(); } catch (Exception ignored) {}
            try { pfd.close(); } catch (Exception ignored) {}
        }
    }

    public static PtySession createSession(int rows, int cols, String cmdStr) {
        int fd = createSubprocess(rows, cols, cmdStr);
        return (fd < 0) ? null : new PtySession(fd);
    }
}
