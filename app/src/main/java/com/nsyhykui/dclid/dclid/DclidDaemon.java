package com.nsyhykui.dclid.dclid;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.preference.PreferenceManager;
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
    private volatile boolean isRunning = false;
    private volatile boolean isTcpRunning = false;
    private ServerSocket tcpServer;
    private LocalServerSocket udsServer;
    private String binPath;

    public DclidDaemon(Context context) {
        this.context = context.getApplicationContext();
        this.rpcHandler = new CommandHandler(context, new DhizukuDpm(context));
        this.binPath = context.getCacheDir().getAbsolutePath() + "/bin";
        AuthManager.get(context).setPromptListener(uid -> AuthOverlay.show(context, uid, "unknown"));
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;
        setupBinDir();
        new Thread(this::startUdsServer).start();
        startTcpServerThread();
        startWatchdog();
    }

    // 准备 bin 目录，将 so 伪装成 pm 和 device
    private void setupBinDir() {
        File binDir = new File(binPath);
        if (!binDir.exists()) binDir.mkdirs();
        
        String soPath = context.getApplicationInfo().nativeLibraryDir + "/libdcli_bin.so";
        File pmFile = new File(binDir, "pm");
        File deviceFile = new File(binDir, "device");
        
        try {
            // 使用软链接 (ln -s) 或者直接拷贝
            Runtime.getRuntime().exec(new String[]{"ln", "-sf", soPath, pmFile.getAbsolutePath()}).waitFor();
            Runtime.getRuntime().exec(new String[]{"ln", "-sf", soPath, deviceFile.getAbsolutePath()}).waitFor();
            // 确保权限
            Runtime.getRuntime().exec(new String[]{"chmod", "755", pmFile.getAbsolutePath()}).waitFor();
            Runtime.getRuntime().exec(new String[]{"chmod", "755", deviceFile.getAbsolutePath()}).waitFor();
        } catch (Exception ignored) {}
    }

    private void startWatchdog() {
        new Thread(() -> {
            while (isRunning) {
                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                if (!isRunning) break;
                if (!isTcpRunning) startTcpServerThread();
            }
        }).start();
    }

    private void startTcpServerThread() {
        new Thread(this::startTcpServer).start();
    }

    private void startUdsServer() {
        try {
            udsServer = new LocalServerSocket("dclid_rpc");
            while (isRunning) {
                LocalSocket socket = udsServer.accept();
                new Thread(() -> {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                        String cmd = reader.readLine();
                        if (cmd != null) { writer.write(rpcHandler.process(cmd)); writer.flush(); }
                        socket.close();
                    } catch (Exception ignored) {}
                }).start();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void startTcpServer() {
        if (isTcpRunning) return;
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            int port = Integer.parseInt(sp.getString("bind_port", "12345"));
            String ip = sp.getString("bind_ip", "0.0.0.0");
            if (!isPortAvailable(port)) return;
            tcpServer = new ServerSocket();
            tcpServer.setReuseAddress(true);
            tcpServer.bind(new InetSocketAddress(ip, port));
            isTcpRunning = true;

            while (isRunning && isTcpRunning) {
                Socket client = tcpServer.accept();
                new Thread(() -> handleTcp(client)).start();
            }
        } catch (Exception e) { isTcpRunning = false; }
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket ss = new ServerSocket()) {
            ss.setReuseAddress(true); ss.bind(new InetSocketAddress(port));
            return true;
        } catch (IOException e) { return false; }
    }

    private void handleTcp(Socket client) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            OutputStream out = client.getOutputStream();

            String authLine = reader.readLine();
            if (authLine == null || !authLine.startsWith("AUTH ")) { client.close(); return; }
            String[] parts = authLine.split(" ");
            int uid = Integer.parseInt(parts[1]);
            String token = parts[2].trim(); 
            SharedPreferences sp = context.getSharedPreferences("dclid_auth", Context.MODE_PRIVATE);

            if (!token.equals(sp.getString("token_" + uid, ""))) {
                out.write("NEED_AUTH\n".getBytes(StandardCharsets.UTF_8)); out.flush();
                if (!AuthManager.get(context).awaitDecision(uid, 60000)) {
                    out.write("DENY\n".getBytes(StandardCharsets.UTF_8)); out.flush();
                    client.close(); return;
                }
                sp.edit().putString("token_" + uid, token).apply();
            }
            out.write("OK\n".getBytes(StandardCharsets.UTF_8)); out.flush();

            String nextLine = reader.readLine();
            int rows = 24, cols = 80;
            if (nextLine != null && nextLine.startsWith("SIZE ")) {
                String[] p = nextLine.split(" ");
                rows = Integer.parseInt(p[1]); cols = Integer.parseInt(p[2]);
            }

            PtyBridge.PtySession pty = PtyBridge.createSession(rows, cols, "/system/bin/sh -i");
            if (pty == null) { client.close(); return; }

            // ★ 核心修复：静默注入 PATH 环境变量，彻底废弃 alias
            try {
                String inject = "export PATH='" + binPath + "':$PATH\n" +
                                "export PS1='$ '\n";
                Thread.sleep(100);
                pty.outputStream.write(inject.getBytes(StandardCharsets.UTF_8));
                pty.outputStream.flush();
            } catch (Exception ignored) {}

            if (nextLine != null && nextLine.startsWith("EXEC ")) {
                String cmd = nextLine.substring(5);
                try {
                    Thread.sleep(50); 
                    pty.outputStream.write((cmd + "\nexit\n").getBytes(StandardCharsets.UTF_8));
                    pty.outputStream.flush();
                } catch (Exception ignored) {}
            }

            InputStream clientIn = client.getInputStream();
            OutputStream clientOut = client.getOutputStream();
            
            new Thread(() -> transfer(pty.inputStream, clientOut, client)).start();
            
            if (nextLine == null || !nextLine.startsWith("EXEC ")) {
                new Thread(() -> transfer(clientIn, pty.outputStream, client)).start();
            }

        } catch (Exception e) { 
            try { client.close(); } catch (Exception ignored) {} 
        }
    }

    private void transfer(InputStream in, OutputStream out, Socket socket) {
        byte[] buf = new byte[4096]; int len;
        try {
            while ((len = in.read(buf)) != -1) { 
                out.write(buf, 0, len); 
                out.flush(); 
            }
        } catch (Exception ignored) {
        } finally { 
            try { socket.close(); } catch (Exception ignored) {} 
        }
    }

    public void stop() {
        isRunning = false;
        isTcpRunning = false;
        try { if (tcpServer != null) tcpServer.close(); } catch (Exception ignored) {}
        try { if (udsServer != null) udsServer.close(); } catch (Exception ignored) {}
    }
}
