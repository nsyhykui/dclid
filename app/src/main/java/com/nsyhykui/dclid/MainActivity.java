package com.nsyhykui.dclid;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("dclid 0.0.0\nRunning on Gradle 9.1.0");
        tv.setTextSize(20);
        tv.setPadding(50, 50, 50, 50);
        setContentView(tv);
    }
}
