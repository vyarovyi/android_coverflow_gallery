package com.masterofcode.android.coverflow_library;

import javax.microedition.khronos.opengles.GL10;

public class CoverFlowRecord {

    private int mTexture;
    private GL10 gl;

    public CoverFlowRecord(int texture, GL10 gl) {
        mTexture = texture;
        this.gl = gl;
    }

    public GL10 getGl() {
        return gl;
    }

    public void setGl(GL10 gl) {
        this.gl = gl;
    }

    public int getTexture() {
        return mTexture;
    }

    public void setTexture(int mTexture) {
        this.mTexture = mTexture;
    }

    @Override
    protected void finalize() throws Throwable {
        if (mTexture != 0) {
            gl.glDeleteTextures(1, new int[] {mTexture}, 0);
        }

        super.finalize();
    }
}
