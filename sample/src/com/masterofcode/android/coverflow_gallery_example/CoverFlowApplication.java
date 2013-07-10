package com.masterofcode.android.coverflow_gallery_example;

import android.app.Application;
import android.os.Environment;
import com.androidquery.callback.BitmapAjaxCallback;
import com.androidquery.util.AQUtility;

import java.io.File;

public class CoverFlowApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        File ext = Environment.getExternalStorageDirectory();
        File cacheDir = new File(ext, "coverflow_example");
        AQUtility.setCacheDir(cacheDir);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        BitmapAjaxCallback.clearCache();
    }
}
