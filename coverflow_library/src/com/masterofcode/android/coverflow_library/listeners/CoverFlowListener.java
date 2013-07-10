package com.masterofcode.android.coverflow_library.listeners;

import com.masterofcode.android.coverflow_library.CoverFlowOpenGL;


public interface CoverFlowListener {
    public void tileOnTop(CoverFlowOpenGL view, int position); // Notify what tile is on top after scroll or start
    public void topTileClicked(CoverFlowOpenGL view, int position);
}
