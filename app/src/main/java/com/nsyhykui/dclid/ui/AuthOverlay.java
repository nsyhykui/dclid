package com.nsyhykui.dclid.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.nsyhykui.dclid.R;
import com.nsyhykui.dclid.core.AuthManager;

public class AuthOverlay {
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static View currentView = null;

    public static void show(final Context context, final int uid, final String pkgName) {
        handler.post(() -> {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) { AuthManager.get(context).decide(uid, false); return; }
            if (currentView != null) { try { wm.removeView(currentView); } catch (Exception ignored) {} }

            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(0xFF202124);
            root.setPadding(60, 60, 60, 60);

            TextView title = new TextView(context);
            title.setText(R.string.auth_dialog_title);
            title.setTextColor(0xFFFFFFFF);
            title.setTextSize(18f);
            title.setPadding(0, 0, 0, 20);

            TextView msg = new TextView(context);
            msg.setText(context.getString(R.string.auth_dialog_msg, uid));
            msg.setTextColor(0xFFCCCCCC);
            msg.setTextSize(14f);
            msg.setPadding(0, 0, 0, 40);

            Button btnAllow = new Button(context);
            btnAllow.setText(R.string.auth_allow);
            btnAllow.setOnClickListener(v -> { AuthManager.get(context).decide(uid, true); dismiss(wm); });

            Button btnDeny = new Button(context);
            btnDeny.setText(R.string.auth_deny);
            btnDeny.setOnClickListener(v -> { AuthManager.get(context).decide(uid, false); dismiss(wm); });

            root.addView(title); root.addView(msg); root.addView(btnAllow); root.addView(btnDeny);

            int type = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
                    type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.CENTER;
            try { wm.addView(root, lp); currentView = root; } catch (Exception e) { AuthManager.get(context).decide(uid, false); }
        });
    }

    public static void dismiss(WindowManager wm) {
        if (currentView != null) { try { wm.removeView(currentView); } catch (Exception ignored) {} currentView = null; }
    }
}
