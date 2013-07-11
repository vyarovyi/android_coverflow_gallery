/**
 * 
 */
package com.masterofcode.android.coverflow_library.render_objects;

import android.app.Activity;
import android.graphics.Bitmap;
import com.masterofcode.android.coverflow_library.R;
import com.masterofcode.android.coverflow_library.listeners.DataChangedListener;
import com.masterofcode.android.coverflow_library.utils.CoverflowBitmapCallback;
import com.masterofcode.android.coverflow_library.utils.CoverflowQuery;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author impaler
 * @author skynet67
 */
public class CoverImage extends AbstractImage<CoverImage> {

    private int index;

    public CoverImage(Activity activity, CoverflowQuery query){
       super(activity, query);
    }

    public void tryLoadTexture(DataChangedListener dataChangedListener, int index){
        this.dataChangedListener = dataChangedListener;
        this.index = index;

        loadTexture();
    }

    private int loadTexture(){
        int result = 0;

        Bitmap bm = (loadedBitmap != null && !loadedBitmap.isRecycled())
                ? loadedBitmap : mQuery.setShowBlackBars(showBlackBars).getCachedImage(mUrl, imageSize);

        if (bm == null) {

            downloadingImage = true;

            CoverflowBitmapCallback callback = new CoverflowBitmapCallback(mActivity, showBlackBars,  new CoverflowBitmapCallback.ImageLoadCallback(){
                public void onLoad(final Bitmap bitmap){

                    downloadingImage = false;

                    if(dataChangedListener != null){
                        dataChangedListener.imageUpdated(index);
                    }

                    loadedBitmap = bitmap;
                }
            });

            mQuery.image(mUrl, true, true, imageSize, R.drawable.empty, callback);
        } else {
            result = loadGLTexture(bm);
        }

        return result;
    }

	public int getTexture(){
        if(textures[0] == 0 && !downloadingImage){
            loadTexture();
        }

        return textures[0];
    }

	/** The draw method for the square with the GL context */
	public void draw(GL10 gl, float translate, float scale) {

        if(textures[0] == 0){
            loadTexture();
        }

        if(!isTextureInit){
            return;
        }

        gl.glPushMatrix();

        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

		// bind the previously generated texture
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
		
		// Point to our buffers
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		
		// Set the face rotation
		gl.glFrontFace(GL10.GL_CW);
		
		// Point to our vertex buffer
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vertexBuffer);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);


        //TODO: optimize next calculations
        float shiftY = (desiredSize - (desiredSize * scale)) * 0.5f ;
        float shiftX = translate > 0 ? (desiredSize - (desiredSize * scale)) : 0;

        gl.glTranslatef(-desiredSize* 0.5f, -desiredSize * 0.5f, 0); // set image center into 0.0
        gl.glTranslatef(translate + shiftX, shiftY, 0); // move image
        gl.glTranslatef(viewportWidth * 0.5f, viewportHeight * 0.5f, 0); // translate the picture to the center

        gl.glScalef(scale, scale, 1); // scale the picture

        // Draw the vertices as triangle strip
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);

		//Disable the client state before leaving
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        gl.glPopMatrix();
	}
}
