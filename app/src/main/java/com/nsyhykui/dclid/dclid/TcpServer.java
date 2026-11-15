package com.nsyhykui.dclid.dclid;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.nsyhykui.dclid.core.AuthManager;
import com.nsyhykui.dclid.core.CommandHandler;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TcpServer implements Runnable {
    private final Context context;
    private final CommandHandler rpcHandler;
    private volatile boolean running = false;
    private ServerSocket server;

    public TcpServer(Context context, CommandHandler rpcHandler) {
        this.context = context;
        this.rpcHandler = rpcHandler;
    }

    public boolean isRunning() { return running; }

    @Override
    public void run() {
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            int port = Integer.parseInt(sp.getString("bind_port", "12345"));
            String ip = sp.getString("bind_ip", "0.0.0.0");

            if (!isPortAvailable(port)) return;

            server = new ServerSocket();
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress(ip, port));
            running = true;

            while (running) {
                Socket client = server.accept();
                new Thread(() -> handle(client), "tcp-client").start();
            }
        } catch (Exception e) {
            running = false;
        }
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket ss = new ServerSocket()) {
            ss.setReuseAddress(true);
            ss.bind(new InetSocketAddress(port));
            return true;
        } catch (IOException e) { return false; }
    }

    private void handle(Socket client) {
        try {
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

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

            String request = reader.readLine();
            if (request == null) { client.close(); return; }

            if (request.startsWith("shell:")) {
                String command = request.substring(6).trim();
                if (!command.isEmpty()) {
                    new RawExecHandler(context, command, out, client).run();
                } else {
                    new PtySessionHandler(context, in, out, client).run();
                }
            } else {
                client.close();
            }
        } catch (Exception e) {
            try { client.close(); } catch (Exception ignored) {}
        }
    }

    public void stop() {
        running = false;
        try { if (server != null) server.close(); } catch (Exception ignored) {}
    }
}
