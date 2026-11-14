package com.nsyhykui.dclid.core;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;

public class DeviceHandler {
    private final DhizukuDpm dpmHelper;

    public DeviceHandler(DhizukuDpm dpmHelper) {
        this.dpmHelper = dpmHelper;
    }

    public String handle(String[] args) {
        if (args.length == 0) return "Failed: missing device command";

        String cmd = args[0];
        try {
            DevicePolicyManager dpm = dpmHelper.get();
            ComponentName admin = dpmHelper.admin();

            if (cmd.equals("lock")) {
                dpm.lockNow();
                return "Success";
            }
            if (cmd.equals("camera_off")) {
                dpm.setCameraDisabled(admin, true);
                return "Success";
            }
            if (cmd.equals("camera_on")) {
                dpm.setCameraDisabled(admin, false);
                return "Success";
            }
            if (cmd.equals("screencap_off")) {
                dpm.setScreenCaptureDisabled(admin, true);
                return "Success";
            }
            if (cmd.equals("screencap_on")) {
                dpm.setScreenCaptureDisabled(admin, false);
                return "Success";
            }
            if (cmd.equals("info")) {
                boolean cam = dpm.getCameraDisabled(admin);
                boolean cap = dpm.getScreenCaptureDisabled(admin);
                long timeout = dpm.getMaximumTimeToLock(admin);
                return "Camera disabled: " + cam + "\n" +
                       "Screencap disabled: " + cap + "\n" +
                       "Max time to lock: " + timeout + "ms";
            }
            return "Failed: unknown device command";
        } catch (Throwable t) {
            return "Failed: " + t.getMessage();
        }
    }
}
