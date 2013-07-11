package com.masterofcode.android.coverflow_library.render_objects;

import android.app.Activity;
import android.graphics.Bitmap;
import android.opengl.GLUtils;
import com.masterofcode.android.coverflow_library.listeners.DataChangedListener;
import com.masterofcode.android.coverflow_library.utils.CoverflowQuery;

import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public abstract class AbstractImage<T extends AbstractImage<T>> {
    protected FloatBuffer vertexBuffer;	// buffer holding the vertices
    protected FloatBuffer textureBuffer;	// buffer holding the texture coordinates

    protected float texture[] = new float[]{
            // Mapping coordinates for the vertices
            0f, 1f,     // top left
            1f, 1f,     // top right
            0f, 0f,     // bottom left
            1f, 0f,     // bottom right
    };
    protected int[] textures = new int[1];

    protected String mUrl;

    protected Activity mActivity;
    protected GL10 mGL;

    protected int resId;

    protected CoverflowQuery mQuery;

    protected int imageSize;
    protected float desiredSize;

    protected int viewportWidth;
    protected int viewportHeight;

    protected boolean isTextureInit;

    protected boolean showBlackBars = true;
    protected boolean downloadingImage;

    protected DataChangedListener dataChangedListener;

    protected Bitmap loadedBitmap;

    public AbstractImage(Activity activity,  int resId){
        this.mActivity = activity;
        this.resId = resId;
    }

    public AbstractImage(Activity activity,  CoverflowQuery query){
        this.mActivity = activity;
        this.mQuery = query;
    }

    public AbstractImage(Activity activity, String url){
        this.mActivity = activity;
        this.mUrl = url;
    }

    public T setUrl(String url){
        this.mUrl = url;
        return (T)this;
    }

    public T setImageSize(int size){
        imageSize = size;
        return (T)this;
    }

    public T setGL(GL10 gl){
        this.mGL = gl;
        return (T)this;
    }

    public T setViewportData(int width, int height){
        this.viewportWidth = width;
        this.viewportHeight = height;
        return (T)this;
    }

    public T setShowBlackBars(boolean showBlackBars) {
        this.showBlackBars = showBlackBars;
        return (T)this;
    }

    public void initBuffers(){

        float sx = Math.abs((float)viewportWidth / imageSize);
        float sy = Math.abs((float)viewportHeight / imageSize);

        float scale = Math.min(sx, sy);

        desiredSize = (imageSize * scale);
        desiredSize -= desiredSize * 0.1f; //offset from the edge of the screen

        float x = 0f;//(viewportWidth - desiredSize) * 0.5f;
        float y = 0f;//(viewportHeight - desiredSize) * 0.5f;

        float vertices[] = {
                x, y,         //Bottom Left
                x + desiredSize, y, 	//Bottom Right
                x, y + desiredSize, 	//Top Left
                x + desiredSize, y + desiredSize,    //Top Right
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


        byteBuffer = ByteBuffer.allocateDirect(texture.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        textureBuffer = byteBuffer.asFloatBuffer();
        textureBuffer.put(texture);
        textureBuffer.position(0);

        isTextureInit = true;
    }

    /**
     * Load the texture for the square
     */
    protected int loadGLTexture(Bitmap bitmap) {

        if(bitmap == null || mGL == null){
            return 0;
        }

        removeTexture();

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
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

        // Clean up
        bitmap.recycle();
        if(loadedBitmap !=null){
            loadedBitmap = null;
        }

        initBuffers();

        return textures[0];
    }

    public void removeTexture(){
        if (textures[0] != 0) {
            mGL.glDeleteTextures(1, new int[] {textures[0]}, 0);
        }

        isTextureInit = false;
    }

    public float getDesiredSize(){
        return desiredSize;
    }

    /** The draw method for the square with the GL context */
    public void draw(GL10 gl, float translate, float scale) {

        if (textures[0] == 0) {
            return;
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
