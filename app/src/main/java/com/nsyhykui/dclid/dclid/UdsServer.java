package com.nsyhykui.dclid.dclid;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import com.nsyhykui.dclid.core.CommandHandler;
import java.io.*;

public class UdsServer implements Runnable {
    private final CommandHandler rpcHandler;
    private volatile boolean running = true;
    private LocalServerSocket server;

    public UdsServer(Context context, CommandHandler rpcHandler) {
        this.rpcHandler = rpcHandler;
    }

    @Override
    public void run() {
        try {
            server = new LocalServerSocket("dclid_rpc");
            while (running) {
                LocalSocket socket = server.accept();
                new Thread(() -> handle(socket), "uds-client").start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handle(LocalSocket socket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            String cmd = reader.readLine();
            if (cmd != null) {
                writer.write(rpcHandler.process(cmd));
                writer.flush();
            }
            socket.close();
        } catch (Exception ignored) {}
    }

    public void stop() {
        running = false;
        try { if (server != null) server.close(); } catch (Exception ignored) {}
    }
}
