package com.robotpajamas.android.ble113_ota;

import android.app.Application;
import android.support.v4.BuildConfig;

import com.robotpajamas.android.ble113_ota.Blueteeth.BlueteethManager;

import timber.log.Timber;

import static timber.log.Timber.DebugTree;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        BlueteethManager.getInstance().initialize(this);


        if (BuildConfig.DEBUG) {
            Timber.plant(new DebugTree());
        } else {
            Timber.plant(new CrashReportingTree());
        }
    }

    /**
     * A tree which logs important information for crash reporting.
     */
    private static class CrashReportingTree extends Timber.HollowTree {
        @Override
        public void i(String message, Object... args) {
        }

        @Override
        public void i(Throwable t, String message, Object... args) {
            i(message, args); // Just add to the log.
        }

        @Override
        public void e(String message, Object... args) {
            i("ERROR: " + message, args); // Just add to the log.
        }

        @Override
        public void e(Throwable t, String message, Object... args) {
            e(message, args);
        }
    }
}