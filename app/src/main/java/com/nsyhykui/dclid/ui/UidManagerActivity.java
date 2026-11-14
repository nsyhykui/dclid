package com.nsyhykui.dclid.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.nsyhykui.dclid.R;
import com.nsyhykui.dclid.core.AuthManager;
import java.util.Set;

public class UidManagerActivity extends Activity {
    private LinearLayout listLayout;
    private AuthManager auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = AuthManager.get(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(50, 50, 50, 50);

        listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        root.addView(listLayout);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        listLayout.removeAllViews();
        Set<Integer> allowed = auth.getAllowedUids();
        
        if (allowed.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText(R.string.uid_empty);
            listLayout.addView(tv);
            return;
        }
        
        for (final Integer uid : allowed) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 20, 0, 20);

            TextView tv = new TextView(this);
            tv.setText("UID: " + uid);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            Button btn = new Button(this);
            btn.setText(R.string.uid_revoke);
            btn.setOnClickListener(v -> {
                auth.revoke(uid);
                refresh();
            });

            row.addView(tv); 
            row.addView(btn);
            listLayout.addView(row);
        }
    }
}
