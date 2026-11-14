package com.nsyhykui.dclid.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AuthManager {
    public static final int ALLOWED = 1;
    public static final int DENIED = 0;
    private static AuthManager instance;
    private final SharedPreferences prefs;
    private final ConcurrentHashMap<Integer, CountDownLatch> latches = new ConcurrentHashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private PromptListener promptListener;

    public interface PromptListener { void onPrompt(int uid); }

    private AuthManager(Context ctx) {
        prefs = ctx.getSharedPreferences("dclid_auth", Context.MODE_PRIVATE);
    }

    public static synchronized AuthManager get(Context ctx) {
        if (instance == null) instance = new AuthManager(ctx.getApplicationContext());
        return instance;
    }

    public void setPromptListener(PromptListener l) { this.promptListener = l; }

    // 获取所有已授权的 UID
    public Set<Integer> getAllowedUids() {
        Set<Integer> uids = new HashSet<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (entry.getKey().startsWith("token_")) {
                try {
                    uids.add(Integer.parseInt(entry.getKey().substring(6)));
                } catch (Exception ignored) {}
            }
        }
        return uids;
    }

    // 撤销某个 UID 的授权（删除 Token）
    public synchronized void revoke(int uid) {
        prefs.edit().remove("token_" + uid).apply();
    }

    public boolean awaitDecision(int uid, long timeoutMs) {
        CountDownLatch latch = new CountDownLatch(1);
        latches.put(uid, latch);
        mainHandler.post(() -> { if (promptListener != null) promptListener.onPrompt(uid); });
        try {
            return latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) { return false; }
    }

    public synchronized void decide(int uid, boolean allow) {
        CountDownLatch latch = latches.remove(uid);
        if (latch != null) latch.countDown();
    }
}
