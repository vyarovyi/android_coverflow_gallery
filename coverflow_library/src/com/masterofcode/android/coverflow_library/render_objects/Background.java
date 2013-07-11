package com.masterofcode.android.coverflow_library.render_objects;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.opengl.GLUtils;
import com.masterofcode.android.coverflow_library.R;

import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class Background extends AbstractImage<Background>{

    private int[] textures = new int[1];

    public Background(Activity activity, int resId) {
        super(activity, resId);
    }

    public Background(Activity activity, String url){
        super(activity, url);
    }

    public void initBuffers(int width, int height) {

        float vertices[] = {
                0f, 0f,         //Bottom Left
                width, 0.0f,    //Bottom Right
                0f, height,    //Top Left
                width, height,    //Top Right
        };

        // a float has 4 bytes so we allocate for each coordinate 4 bytes
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());

        // allocates the memory from the byte buffer
        vertexBuffer = byteBuffer.asFloatBuffer();

        // fill the vertexBuffer with the vertices
        vertexBuffer.put(vertices);

        // set the cursor position to the beginning of the buffer
        vertexBuffer.position(0);

    }

    /**
     * Load the texture for the square
     *
     */
    public void loadGLTexture() {

        removeTexture();

        if(resId != 0){
            initResTexuture();
        } else {
            resId = R.drawable.empty;
            initResTexuture();
        }

    }

    private void initResTexuture(){
        // loading texture

        Bitmap bitmap = BitmapFactory.decodeResource(mActivity.getResources(), resId);

        int tmp = 1;
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        while (w > tmp || h > tmp) {
            tmp <<= 1;
        }

        int width = tmp;
        int height = tmp;

        Bitmap bm = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas cv = new Canvas(bm);

        float left = (width - w) / 2;
        float top = (height - h) / 2;
        cv.drawBitmap(bitmap, left, top, new Paint());


        float oneW = ((float) 1 / width);
        float oneH = ((float) 1 / height);

        float pointLeft = oneW * left;
        float pointRight = 1 - pointLeft;
        float pointBottom = oneH * top;
        float pointTop = 1 - pointBottom;

        this.texture = new float[]{
                // Mapping coordinates for the vertices
                pointLeft, pointTop,            // top left
                pointRight, pointTop,        // top right
                pointLeft, pointBottom,        // bottom left
                pointRight, pointBottom,    // bottom right
        };



        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(texture.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        textureBuffer = byteBuffer.asFloatBuffer();
        textureBuffer.put(texture);
        textureBuffer.position(0);

        // generate one texture pointer
        mGL.glGenTextures(1, textures, 0);
        // ...and bind it to our array
        mGL.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

        // create nearest filtered texture
        mGL.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        mGL.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);

        //Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
        mGL.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
        mGL.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);

        // Use Android GLUtils to specify a two-dimensional texture image from our bitmap
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bm, 0);

        // Clean up
        bm.recycle();
        bitmap.recycle();
    }

    /**
     * The draw method for the square with the GL context
     */
    public void draw(GL10 gl) {

        if (textures[0] == 0) {
            return;
        }

        gl.glPushMatrix();

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

        float scaleX = 1.0f;//GlRenderer.width / texWidth;
        float scaleY = 1.0f;//GlRenderer.height / texHeight;


        gl.glScalef(scaleX, scaleY, 1.0f); // scale the picture

        // Draw the vertices as triangle strip
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);

        //Disable the client state before leaving
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        gl.glPopMatrix();
    }
}
