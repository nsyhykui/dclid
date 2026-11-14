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
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);

        Preference dhizukuBtn = findPreference("dhizuku_request");
        if (dhizukuBtn != null) {
            dhizukuBtn.setOnPreferenceClickListener(p -> {
                if (Dhizuku.init(requireContext())) {
                    if (Dhizuku.isPermissionGranted()) {
                        Toast.makeText(getContext(), R.string.toast_perm_granted, Toast.LENGTH_SHORT).show();
                    } else {
                        // 改用标准的匿名内部类，解决 Lambda 报错
                        Dhizuku.requestPermission(new DhizukuRequestPermissionListener() {
                            @Override
                            public void onRequestPermission(int grantResult) {
                                requireActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        int msgRes = (grantResult == 0) ? R.string.toast_perm_granted : R.string.toast_perm_denied;
                                        Toast.makeText(getContext(), msgRes, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        });
                    }
                } else {
                    Toast.makeText(getContext(), R.string.toast_init_failed, Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }

        Preference authManage = findPreference("auth_manage");
        if (authManage != null) {
            authManage.setOnPreferenceClickListener(p -> {
                startActivity(new Intent(requireContext(), UidManagerActivity.class));
                return true;
            });
        }

        SwitchPreferenceCompat serviceSwitch = findPreference("service_switch");
        if (serviceSwitch != null) {
            serviceSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isChecked = (Boolean) newValue;
                Intent intent = new Intent(requireContext(), DclidService.class);
                if (isChecked) {
                    requireContext().startForegroundService(intent);
                    Toast.makeText(getContext(), R.string.toast_service_starting, Toast.LENGTH_SHORT).show();
                } else {
                    requireContext().stopService(intent);
                    Toast.makeText(getContext(), R.string.toast_service_stopped, Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }
    }
}
