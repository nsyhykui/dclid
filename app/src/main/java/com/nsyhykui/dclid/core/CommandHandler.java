package com.nsyhykui.dclid.core;

import android.content.Context;

public class CommandHandler {
    private final PmPackagesHandler pmPackagesHandler;
    private final DeviceHandler deviceHandler;

    public CommandHandler(Context context, DhizukuDpm dpmHelper) {
        this.pmPackagesHandler = new PmPackagesHandler(context);
        this.deviceHandler = new DeviceHandler(dpmHelper);
    }

    public String process(String cmd) {
        String[] parts = cmd.trim().split("\\s+");
        String mainCmd = parts[0];

        // 截取后面的参数
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        try {
            if (mainCmd.equals("pm")) {
                return handlePm(args);
            }
            if (mainCmd.equals("device")) {
                return deviceHandler.handle(args);
            }
            return "Failed: unknown command";
        } catch (Throwable t) {
            return "Failed: " + t.getMessage();
        }
    }

    private String handlePm(String[] args) {
        if (args.length >= 1 && args[0].equals("list")) {
            if (args.length >= 2 && args[1].equals("packages")) {
                String[] pkgArgs = new String[args.length - 2];
                System.arraycopy(args, 2, pkgArgs, 0, pkgArgs.length);
                return pmPackagesHandler.handle(pkgArgs);
            }
        }
        return "Failed: unsupported pm command";
    }
}
