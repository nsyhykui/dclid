package com.nsyhykui.dclid;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import com.rosan.dhizuku.api.Dhizuku;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setTextSize(18);
        tv.setPadding(50, 50, 50, 50);

        StringBuilder sb = new StringBuilder();
        sb.append("dclid 0.0.0\n\n");

        boolean isInit = Dhizuku.init(this);
        if (isInit) {
            sb.append("✅ Dhizuku-API 初始化成功\n");
            if (Dhizuku.isPermissionGranted()) {
                sb.append("✅ 已获得 Dhizuku 权限\n");
            } else {
                sb.append("⚠️ 未获得 Dhizuku 权限\n");
            }
        } else {
            sb.append("❌ Dhizuku 初始化失败，请确保已安装并激活 Dhizuku\n");
        }

        tv.setText(sb.toString());
        setContentView(tv);
    }
}
