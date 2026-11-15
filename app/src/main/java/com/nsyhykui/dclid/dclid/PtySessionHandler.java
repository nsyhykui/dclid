package com.nsyhykui.dclid.dclid;

import android.content.Context;
import com.nsyhykui.dclid.shell.PtyBridge;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class PtySessionHandler implements Runnable {
    private static final int ID_STDIN = 0;
    private static final int ID_STDOUT = 1;
    private static final int ID_WIN_SIZE = 5;

    private final Context context;
    private final InputStream in;
    private final OutputStream out;
    private final Socket client;
    private PtyBridge.PtySession pty;

    public PtySessionHandler(Context context, InputStream in, OutputStream out, Socket client) {
        this.context = context;
        this.in = in;
        this.out = out;
        this.client = client;
    }

    @Override
    public void run() {
        try {
            int[] size = readWindowSize();
            
            // ★ 纯净启动 sh -i，不需要通过 ENV 读文件
            pty = PtyBridge.createSession(size[0], size[1], "/system/bin/sh -i");
            if (pty == null) { client.close(); return; }

            // ★ 完美无痕注入
            try {
                String dcliBinPath = context.getApplicationInfo().nativeLibraryDir + "/libdcli_bin.so";
                String inject = "alias pm='" + dcliBinPath + " pm'\n" +
                                "alias device='" + dcliBinPath + " device'\n" +
                                "export PS1='$ '\n";

                // 1. 关闭回显
                PtyBridge.setEcho(pty.fd, false);
                Thread.sleep(50); // 等一下 sh 初始化

                // 2. 盲打注入，此时屏幕上绝对看不到任何字符
                pty.outputStream.write(inject.getBytes(StandardCharsets.UTF_8));
                pty.outputStream.flush();
                Thread.sleep(50);

                // 3. 恢复回显
                PtyBridge.setEcho(pty.fd, true);
            } catch (Exception ignored) {}

            Thread readThread = new Thread(this::ptyToClient, "pty-read");
            readThread.start();
            clientToPty();
            readThread.join();
        } catch (Exception e) {
            try { client.close(); } catch (Exception ignored) {}
        }
    }

    private int[] readWindowSize() throws IOException {
        int[] result = {24, 80};
        byte[] header = new byte[5];
        if (in.read(header) == 5) {
            ByteBuffer bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
            int id = bb.get() & 0xFF;
            int len = bb.getInt();
            if (id == ID_WIN_SIZE && len == 4) {
                byte[] data = new byte[4];
                in.read(data);
                ByteBuffer wb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
                result[0] = wb.getShort();
                result[1] = wb.getShort();
            }
        }
        return result;
    }

    private void ptyToClient() {
        try {
            byte[] buf = new byte[4096];
            int len;
            while ((len = pty.inputStream.read(buf)) != -1) {
                byte[] pkt = new byte[5 + len];
                pkt[0] = ID_STDOUT;
                pkt[1] = (byte)(len & 0xFF);
                pkt[2] = (byte)((len >> 8) & 0xFF);
                pkt[3] = (byte)((len >> 16) & 0xFF);
                pkt[4] = (byte)((len >> 24) & 0xFF);
                System.arraycopy(buf, 0, pkt, 5, len);
                out.write(pkt);
                out.flush();
            }
        } catch (Exception ignored) {}
    }

    private void clientToPty() {
        try {
            byte[] header = new byte[5];
            while (in.read(header) == 5) {
                ByteBuffer hb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
                int id = hb.get() & 0xFF;
                int len = hb.getInt();
                if (len < 0 || len > 1024 * 1024) return;
                byte[] data = new byte[len];
                if (len > 0 && in.read(data) != len) return;
                if (id == ID_STDIN) {
                    pty.outputStream.write(data);
                    pty.outputStream.flush();
                } else if (id == ID_WIN_SIZE && len == 4) {
                    ByteBuffer wb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
                    PtyBridge.setPtyWindowSize(pty.fd, wb.getShort(), wb.getShort());
                }
            }
        } catch (Exception ignored) {}
    }
}
