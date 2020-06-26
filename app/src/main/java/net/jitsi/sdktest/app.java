package net.jitsi.sdktest;

import android.content.Context;

import java.lang.ref.WeakReference;

public class app extends android.app.Application{

    private static WeakReference<Context> referenceCntx;

    @Override
    public void onCreate() {
        super.onCreate();

        referenceCntx = new WeakReference<>(getApplicationContext());
    }

    public static Context getApplicationCntx(){
        return referenceCntx.get();
    }
}
