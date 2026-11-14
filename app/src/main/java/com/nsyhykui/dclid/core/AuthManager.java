package com.nsyhykui.dclid.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AuthManager {
    public static final int ALLOWED = 1;
    public static final int DENIED = 0;
    public static final int TIMEOUT = -2;
    private static AuthManager instance;
    private final SharedPreferences prefs;
    private final Set<Integer> allowedUids;
    private final ConcurrentHashMap<Integer, CountDownLatch> latches = new ConcurrentHashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private PromptListener promptListener;

    public interface PromptListener { void onPrompt(int uid); }

    private AuthManager(Context ctx) {
        prefs = ctx.getSharedPreferences("dclid_auth", Context.MODE_PRIVATE);
        allowedUids = new HashSet<>();
        for (String s : prefs.getStringSet("allowed_uids", new HashSet<>())) {
            try { allowedUids.add(Integer.parseInt(s)); } catch (Exception ignored) {}
        }
    }

    public static synchronized AuthManager get(Context ctx) {
        if (instance == null) instance = new AuthManager(ctx.getApplicationContext());
        return instance;
    }

    public Set<Integer> getAllowedUids() { return new HashSet<>(allowedUids); }
    public void setPromptListener(PromptListener l) { this.promptListener = l; }

    public int awaitDecision(int uid, long timeoutMs) {
        if (allowedUids.contains(uid)) return ALLOWED;
        CountDownLatch latch = new CountDownLatch(1);
        latches.put(uid, latch);
        mainHandler.post(() -> { if (promptListener != null) promptListener.onPrompt(uid); });
        try {
            if (latch.await(timeoutMs, TimeUnit.MILLISECONDS)) return allowedUids.contains(uid) ? ALLOWED : DENIED;
        } catch (InterruptedException ignored) {}
        return TIMEOUT;
    }

    public synchronized void decide(int uid, boolean allow) {
        if (allow) {
            allowedUids.add(uid);
            Set<String> strSet = new HashSet<>();
            for (int u : allowedUids) strSet.add(String.valueOf(u));
            prefs.edit().putStringSet("allowed_uids", strSet).apply();
        }
        CountDownLatch latch = latches.remove(uid);
        if (latch != null) latch.countDown();
    }
    
    public synchronized void revoke(int uid) {
        allowedUids.remove(uid);
        Set<String> strSet = new HashSet<>();
        for (int u : allowedUids) strSet.add(String.valueOf(u));
        prefs.edit().putStringSet("allowed_uids", strSet).apply();
    }
}
