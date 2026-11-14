package com.nsyhykui.dclid.core;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AuthManager {
    public static final int ALLOWED = 1;
    public static final int DENIED = 0;
    private static AuthManager instance;
    private final ConcurrentHashMap<Integer, CountDownLatch> latches = new ConcurrentHashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private PromptListener promptListener;

    public interface PromptListener { void onPrompt(int uid); }

    private AuthManager(Context ctx) {}
    public static synchronized AuthManager get(Context ctx) {
        if (instance == null) instance = new AuthManager(ctx.getApplicationContext());
        return instance;
    }

    public void setPromptListener(PromptListener l) { this.promptListener = l; }

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
