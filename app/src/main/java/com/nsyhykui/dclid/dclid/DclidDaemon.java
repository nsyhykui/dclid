package com.nsyhykui.dclid.dclid;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.preference.PreferenceManager;
import android.widget.Toast;
import com.nsyhykui.dclid.R;
import com.nsyhykui.dclid.core.AuthManager;
import com.nsyhykui.dclid.core.CommandHandler;
import com.nsyhykui.dclid.core.DhizukuDpm;
import com.nsyhykui.dclid.shell.PtyBridge;
import com.nsyhykui.dclid.ui.AuthOverlay;
import java.io.*;
import java.net.InetSocketAddress;
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
        AuthManager.get(context).setPromptListener(uid -> AuthOverlay.show(context, uid, "unknown"));
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;
        new Thread(this::startUdsServer).start();
        new Thread(this::startTcpServer).start();
    }

    private void startUdsServer() {
        try {
            udsServer = new LocalServerSocket("dclid_rpc");
            while (isRunning) {
                LocalSocket socket = udsServer.accept();
                new Thread(() -> handleUds(socket)).start();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleUds(LocalSocket socket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            String cmd = reader.readLine();
            if (cmd != null) { writer.write(rpcHandler.process(cmd)); writer.flush(); }
            socket.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void startTcpServer() {
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            int port = Integer.parseInt(sp.getString("bind_port", "12345"));
            String ip = sp.getString("bind_ip", "0.0.0.0");

            if (!isPortAvailable(port)) {
                new android.os.Handler(context.getMainLooper()).post(() -> 
                    Toast.makeText(context, context.getString(R.string.toast_port_in_use, port), Toast.LENGTH_LONG).show()
                );
                return;
            }

            tcpServer = new ServerSocket();
            tcpServer.setReuseAddress(true);
            tcpServer.bind(new InetSocketAddress(ip, port));

            while (isRunning) {
                Socket client = tcpServer.accept();
                new Thread(() -> handleTcp(client)).start();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket ss = new ServerSocket()) {
            ss.setReuseAddress(true);
            ss.bind(new InetSocketAddress(port));
            return true;
        } catch (IOException e) { return false; }
    }

    private void handleTcp(Socket client) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            OutputStream out = client.getOutputStream();

            // 1. 握手
            String authLine = reader.readLine(); 
            if (authLine == null || !authLine.startsWith("AUTH ")) { client.close(); return; }
            int uid = Integer.parseInt(authLine.substring(5).trim());
            
            // 2. 阻塞等待授权
            if (AuthManager.get(context).awaitDecision(uid, 60000) != AuthManager.ALLOWED) {
                out.write("DENY\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
                client.close(); 
                return;
            }

            // 3. 授权通过，发送 OK 唤醒客户端
            out.write("OK\n".getBytes(StandardCharsets.UTF_8));
            out.flush();

            // 4. 读取 SIZE
            String sizeLine = reader.readLine();
            int rows = 24, cols = 80;
            if (sizeLine != null && sizeLine.startsWith("SIZE ")) {
                String[] parts = sizeLine.split(" ");
                rows = Integer.parseInt(parts[1]); cols = Integer.parseInt(parts[2]);
            }

            // 5. 启动 PTY
            PtyBridge.PtySession pty = PtyBridge.createSession(rows, cols);
            if (pty == null) { client.close(); return; }

            String dcliBinPath = context.getApplicationInfo().nativeLibraryDir + "/libdcli_bin.so";
            String aliasCmd = "alias pm='" + dcliBinPath + " pm'; alias device='" + dcliBinPath + " device'; exec /system/bin/sh -i\n";
            pty.outputStream.write(aliasCmd.getBytes(StandardCharsets.UTF_8));
            pty.outputStream.flush();

            // 6. 双向转发
            InputStream clientIn = client.getInputStream();
            OutputStream clientOut = client.getOutputStream();
            new Thread(() -> transferStream(pty.inputStream, clientOut)).start();
            new Thread(() -> transferStream(clientIn, pty.outputStream)).start();

        } catch (Exception e) { e.printStackTrace(); try { client.close(); } catch (Exception ignored) {} }
    }

    private void transferStream(InputStream in, OutputStream out) {
        byte[] buffer = new byte[4096];
        int len;
        try {
            while ((len = in.read(buffer)) != -1) { out.write(buffer, 0, len); out.flush(); }
        } catch (Exception ignored) {}
    }

    public void stop() {
        isRunning = false;
        try { if (tcpServer != null) tcpServer.close(); } catch (Exception ignored) {}
        try { if (udsServer != null) udsServer.close(); } catch (Exception ignored) {}
    }
}
