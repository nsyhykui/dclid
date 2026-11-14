package com.nsyhykui.dclid.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import com.nsyhykui.dclid.R;
import com.nsyhykui.dclid.dclid.DclidService;
import com.rosan.dhizuku.api.Dhizuku;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);

        Preference dhizukuStatus = findPreference("dhizuku_status");
        if (dhizukuStatus != null) {
            boolean isInit = Dhizuku.init(requireContext());
            if (isInit && Dhizuku.isPermissionGranted()) {
                dhizukuStatus.setSummary("✅ 权限已获取");
            } else if (isInit) {
                dhizukuStatus.setSummary("⚠️ 初始化成功，但未授权");
            } else {
                dhizukuStatus.setSummary("❌ 初始化失败，请检查 Dhizuku");
            }
        }

        SwitchPreferenceCompat serviceSwitch = findPreference("service_switch");
        if (serviceSwitch != null) {
            serviceSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isChecked = (Boolean) newValue;
                Intent intent = new Intent(requireContext(), DclidService.class);
                if (isChecked) {
                    requireContext().startForegroundService(intent);
                    Toast.makeText(getContext(), "正在启动守护进程...", Toast.LENGTH_SHORT).show();
                } else {
                    requireContext().stopService(intent);
                    Toast.makeText(getContext(), "守护进程已停止", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }
    }
}
