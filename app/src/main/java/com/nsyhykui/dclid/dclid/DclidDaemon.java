package com.nsyhykui.dclid.dclid;

import android.content.Context;
import com.nsyhykui.dclid.core.AuthManager;
import com.nsyhykui.dclid.core.CommandHandler;
import com.nsyhykui.dclid.core.DhizukuDpm;
import com.nsyhykui.dclid.ui.AuthOverlay;

public class DclidDaemon {
    private final Context context;
    private final CommandHandler rpcHandler;
    private volatile boolean isRunning = false;

    private UdsServer udsServer;
    private TcpServer tcpServer;

    public DclidDaemon(Context context) {
        this.context = context.getApplicationContext();
        this.rpcHandler = new CommandHandler(context, new DhizukuDpm(context));
        AuthManager.get(context).setPromptListener(uid -> AuthOverlay.show(context, uid, "unknown"));
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;

        udsServer = new UdsServer(context, rpcHandler);
        new Thread(udsServer, "uds-server").start();

        tcpServer = new TcpServer(context, rpcHandler);
        new Thread(tcpServer, "tcp-server").start();

        new Thread(this::watchdog, "watchdog").start();
    }

    private void watchdog() {
        while (isRunning) {
            try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            if (!isRunning) break;
            if (!tcpServer.isRunning()) {
                tcpServer = new TcpServer(context, rpcHandler);
                new Thread(tcpServer, "tcp-server").start();
            }
        }
    }

    public void stop() {
        isRunning = false;
        if (udsServer != null) udsServer.stop();
        if (tcpServer != null) tcpServer.stop();
    }
}
