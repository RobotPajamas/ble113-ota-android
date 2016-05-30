package com.robotpajamas.android.ble113_ota;

import android.app.Application;

import timber.log.Timber;

import static timber.log.Timber.DebugTree;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new DebugTree());
    }
}