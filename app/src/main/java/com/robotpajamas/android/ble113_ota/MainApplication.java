package com.robotpajamas.android.ble113_ota;

import android.app.Application;
import android.support.v4.BuildConfig;

import timber.log.Timber;

import static timber.log.Timber.DebugTree;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new DebugTree());
        }
    }
}