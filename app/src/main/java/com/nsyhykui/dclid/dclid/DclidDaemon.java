package com.nsyhykui.dclid.dclid;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import com.nsyhykui.dclid.core.CommandHandler;
import com.nsyhykui.dclid.core.DhizukuDpm;
import com.nsyhykui.dclid.shell.PtyBridge;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class DclidDaemon {
    private final Context context;
    private final CommandHandler rpcHandler;
    private boolean isRunning = false;
    
    private ServerSocket tcpServer;
    private LocalServerSocket udsServer;

    public DclidDaemon(Context context) {
        this.context = context.getApplicationContext();
        this.rpcHandler = new CommandHandler(new DhizukuDpm(context));
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;

        // 1. 启动本地 UDS (给 libdcli_bin.so 用)
        new Thread(this::startUdsServer).start();
        
        // 2. 启动 TCP (给客户端 C/Python 用)
        new Thread(this::startTcpServer).start();
    }

    private void startUdsServer() {
        try {
            udsServer = new LocalServerSocket("dclid_rpc");
            while (isRunning) {
                LocalSocket socket = udsServer.accept();
                new Thread(() -> handleUds(socket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleUds(LocalSocket socket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            
            String cmd = reader.readLine();
            if (cmd != null) {
                String result = rpcHandler.process(cmd);
                writer.write(result);
                writer.flush();
            }
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startTcpServer() {
        try {
            tcpServer = new ServerSocket(12345);
            while (isRunning) {
                Socket client = tcpServer.accept();
                new Thread(() -> handleTcp(client)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleTcp(Socket client) {
        try {
            // 1. 启动 PTY
            PtyBridge.PtySession pty = PtyBridge.createSession(24, 80);
            if (pty == null) {
                client.close();
                return;
            }

            // 2. 注入 alias 启动 sh (以后完善)
            String nativeLibDir = context.getApplicationInfo().nativeLibraryDir;
            String dcliBinPath = nativeLibDir + "/libdcli_bin.so";
            String aliasCmd = "alias pm='" + dcliBinPath + " pm'; exec /system/bin/sh -i";
            
            // 为了简单，直接起一个 sh -c 注入环境
            pty.outputStream.write((aliasCmd + "\n").getBytes(StandardCharsets.UTF_8));
            pty.outputStream.flush();

            // 3. 双向流转发
            new Thread(() -> transferStream(pty.inputStream, client.getOutputStream())).start();
            new Thread(() -> transferStream(client.getInputStream(), pty.outputStream)).start();
        } catch (Exception e) {
            e.printStackTrace();
            try { client.close(); } catch (Exception ignored) {}
        }
    }

    private void transferStream(InputStream in, OutputStream out) {
        byte[] buffer = new byte[4096];
        int len;
        try {
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                out.flush();
            }
        } catch (Exception ignored) {}
    }

    public void stop() {
        isRunning = false;
        try { if (tcpServer != null) tcpServer.close(); } catch (Exception ignored) {}
        try { if (udsServer != null) udsServer.close(); } catch (Exception ignored) {}
    }
}
