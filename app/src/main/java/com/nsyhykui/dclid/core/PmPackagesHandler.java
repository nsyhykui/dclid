package com.nsyhykui.dclid.core;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import java.util.List;

public class PmPackagesHandler {
    private final Context context;

    public PmPackagesHandler(Context ctx) {
        this.context = ctx.getApplicationContext();
    }

    public String handle(String[] args) {
        boolean showSystem = false;
        boolean showThirdParty = false;
        String filter = null;

        // 解析参数
        for (String arg : args) {
            if (arg.equals("-s")) showSystem = true;
            else if (arg.equals("-3")) showThirdParty = true;
            else if (!arg.startsWith("-")) filter = arg;
        }

        PackageManager pm = context.getPackageManager();
        List<PackageInfo> list = pm.getInstalledPackages(0);

        StringBuilder sb = new StringBuilder();
        for (PackageInfo pi : list) {
            ApplicationInfo ai = pi.applicationInfo;
            if (ai == null) continue;

            boolean isSystem = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;

            // 过滤系统/第三方
            if (showSystem && !isSystem) continue;
            if (showThirdParty && isSystem) continue;

            // 过滤包名
            if (filter != null && !pi.packageName.contains(filter)) continue;

            sb.append("package:").append(pi.packageName).append("\n");
        }

        return sb.toString().trim();
    }
}
