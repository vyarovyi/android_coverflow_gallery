package com.masterofcode.android.coverflow_library.utils;

import com.androidquery.callback.AjaxStatus;

import java.io.File;

public class CoverflowAjaxStatus extends AjaxStatus {

    private int source = NETWORK;
    private File file;

    public AjaxStatus source(int source){
        this.source = source;
        return this;
    }

    protected File getFile() {
        return file;
    }

}
