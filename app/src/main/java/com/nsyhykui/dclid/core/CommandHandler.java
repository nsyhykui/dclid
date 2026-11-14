package com.nsyhykui.dclid.core;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;

public class CommandHandler {
    private final DhizukuDpm dpmHelper;

    public CommandHandler(DhizukuDpm dpmHelper) {
        this.dpmHelper = dpmHelper;
    }

    public String process(String cmd) {
        String[] parts = cmd.trim().split("\\s+");
        String mainCmd = parts[0];

        if (mainCmd.equals("pm") || mainCmd.equals("device")) {
            return handleDoCommand(mainCmd, parts);
        }
        
        return "Failed: Unknown command for dclid RPC: " + cmd;
    }

    private String handleDoCommand(String mainCmd, String[] args) {
        try {
            DevicePolicyManager dpm = dpmHelper.get();
            ComponentName admin = dpmHelper.admin();
            
            if (mainCmd.equals("pm") && args.length >= 3 && args[1].equals("hide")) {
                dpm.setApplicationHidden(admin, args[2], true);
                return "Success";
            }
            if (mainCmd.equals("device") && args.length >= 2 && args[1].equals("lock")) {
                dpm.lockNow();
                return "Success";
            }
            // 接下来把所有 pm / device 命令都写在这里
            return "Failed: Command not implemented yet.";
        } catch (Throwable t) {
            return "Failed: " + t.getMessage();
        }
    }
}
