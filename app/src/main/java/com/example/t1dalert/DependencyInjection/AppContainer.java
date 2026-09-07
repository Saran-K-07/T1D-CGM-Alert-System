package com.example.t1dalert.DependencyInjection;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.t1dalert.CGM.Repository.CgmRepository;
import com.example.t1dalert.Preferences.EncryptedPreferencesRepository;
import com.example.t1dalert.Notification.NotificationHelper;
import com.example.t1dalert.Preferences.PreferencesRepository;

import lombok.Getter;

public final class AppContainer {
    private static AppContainer INSTANCE;

    @Getter
    private final PreferencesRepository prefs;
    @Getter
    private final RequestQueue requestQueue;
    @Getter
    private final Handler mainHandler;
    @Getter
    private final CgmRepository cgmRepository;
    @Getter
    private final NotificationHelper notificationHelper;

    private AppContainer(Context appContext) {
        Context ctx = appContext.getApplicationContext();
        this.prefs = new EncryptedPreferencesRepository(ctx);
        this.requestQueue = Volley.newRequestQueue(ctx);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.cgmRepository = new CgmRepository(ctx, requestQueue, prefs);
        this.notificationHelper = new NotificationHelper();
    }

    public static void init(Context context) {
        if (INSTANCE == null) {
            synchronized (AppContainer.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AppContainer(context.getApplicationContext());
                }
            }
        }
    }

    public static AppContainer get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("AppContainer not initialized. Call AppContainer.init(context) in Application or MainActivity.onCreate()");
        }
        return INSTANCE;
    }

    public static void resetForTesting() {
        INSTANCE = null;
    }
}
