package com.nsyhykui.dclid.core;

import android.app.admin.DevicePolicyManager;
import android.app.admin.IDevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.os.IInterface;
import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuBinderWrapper;
import org.lsposed.hiddenapibypass.HiddenApiBypass;
import java.lang.reflect.Field;

public class DhizukuDpm {
    private final Context context;
    private DevicePolicyManager cached = null;

    public DhizukuDpm(Context context) {
        this.context = context.getApplicationContext();
    }

    public DevicePolicyManager get() throws Exception {
        if (cached != null) return cached;

        HiddenApiBypass.addHiddenApiExemptions("Landroid/app/admin/DevicePolicyManager;");
        HiddenApiBypass.addHiddenApiExemptions("Landroid/app/admin/IDevicePolicyManager;");

        if (!Dhizuku.init(context)) {
            throw new IllegalStateException("Dhizuku init failed");
        }

        Context ownerCtx = context.createPackageContext(
                Dhizuku.getOwnerComponent().getPackageName(),
                Context.CONTEXT_IGNORE_SECURITY);
        DevicePolicyManager manager = ownerCtx.getSystemService(DevicePolicyManager.class);
        if (manager == null) throw new IllegalStateException("dpm null");

        Field field = manager.getClass().getDeclaredField("mService");
        field.setAccessible(true);
        Object old = field.get(manager);

        if (!(old instanceof DhizukuBinderWrapper)) {
            IBinder oldBinder = ((IInterface) old).asBinder();
            IBinder newBinder = Dhizuku.binderWrapper(oldBinder);
            IDevicePolicyManager newIf = IDevicePolicyManager.Stub.asInterface(newBinder);
            field.set(manager, newIf);
        }

        cached = manager;
        return cached;
    }

    public ComponentName admin() {
        return Dhizuku.getOwnerComponent();
    }
}
