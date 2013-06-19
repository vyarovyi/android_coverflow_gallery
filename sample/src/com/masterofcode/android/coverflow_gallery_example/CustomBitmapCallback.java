package com.masterofcode.android.coverflow_gallery_example;

import android.graphics.Bitmap;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.callback.BitmapAjaxCallback;
import com.masterofcode.android.coverflow_library.listeners.DataChangedListener;

public class CustomBitmapCallback extends BitmapAjaxCallback {
    private int index;
    private DataChangedListener dataChangedListener;

    public CustomBitmapCallback(int index, DataChangedListener dataChangedListener){
        super();
        this.index = index;
        this.dataChangedListener = dataChangedListener;
        this.weakHandler(this,"refreshData");
    }

    //Don't remove this function. It will be called from AjaxCallback
    public void refreshData(String url, Bitmap bmp, AjaxStatus status){
        dataChangedListener.imageUpdated(index);
    }
}