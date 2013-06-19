package com.masterofcode.android.coverflow_library.listeners;

import android.graphics.Bitmap;
import com.masterofcode.android.coverflow_library.CoverFlowOpenGL;


public interface CoverFlowListener {
    public int getCount(CoverFlowOpenGL view);				// Number of images to display
    public Bitmap getImage(CoverFlowOpenGL anotherCoverFlow, int position);	// Image at position
    public void tileOnTop(CoverFlowOpenGL view, int position); // Notify what tile is on top after scroll or start
    public void topTileClicked(CoverFlowOpenGL view, int position);
}
