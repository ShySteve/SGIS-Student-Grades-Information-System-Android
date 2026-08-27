package com.sgis.app;

import android.app.Application;

/**
 * Holds the single in-memory DataStore for the lifetime of the app process, standing in for
 * the "store" reference that was passed directly between JFrames in the Swing prototype.
 * Data is lost when the app process is killed - swapping this for real persistence (e.g. Room +
 * SQLite) is the natural next step, exactly as the original comment about JDBC/MySQL described.
 */
public class SgisApp extends Application {
    private DataStore store;

    @Override
    public void onCreate() {
        super.onCreate();
        store = DataStore.empty();
    }

    public DataStore getStore() {
        return store;
    }

    public static DataStore store(android.content.Context ctx) {
        return ((SgisApp) ctx.getApplicationContext()).getStore();
    }
}
