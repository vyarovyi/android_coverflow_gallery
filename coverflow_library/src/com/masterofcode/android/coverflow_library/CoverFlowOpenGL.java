/*
 * Copyright 2013 - Android Coverflow Gallery. (Vladyslav Yarovyi)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.masterofcode.android.coverflow_library;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.animation.AnimationUtils;
import com.masterofcode.android.coverflow_library.listeners.CoverFlowListener;
import com.masterofcode.android.coverflow_library.listeners.DataChangedListener;
import com.masterofcode.android.coverflow_library.render_objects.Background;
import com.masterofcode.android.coverflow_library.render_objects.CoverImage;
import com.masterofcode.android.coverflow_library.render_objects.EmptyImage;
import com.masterofcode.android.coverflow_library.utils.CoverflowQuery;
import com.masterofcode.android.coverflow_library.utils.DataCache;
import com.masterofcode.android.coverflow_library.utils.EQuality;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom Cover Flow Gallery View.
 * This core class is responsible for drawing all images.
 *
 * @author skynet67
 */
public class CoverFlowOpenGL extends GLSurfaceView implements GLSurfaceView.Renderer{

	private static final int TOUCH_MINIMUM_MOVE = 5;
	private static final float FRICTION = 10.0f;
    private static final float MAX_SPEED = 6.0f;

    private int maxTiles = 21; // the maximum tiles in the cache
    private int visibleTiles = 5; // the visble tiles left and right

    private int imageSize = 512; // the bitmap size we use for the texture

    private float mOffset;
    private int mLastOffset;
    private RectF mTouchRect;
    
    private int mWidth;
    private int mHeight;

    private boolean mTouchMoved;
    private float mTouchStartPos;
    private float mTouchStartX;
    private float mTouchStartY;
    
    private float mStartOffset;
    private long mStartTime;
    
    private float mStartSpeed;
    private float mDuration;
    private Runnable mAnimationRunnable;
    private VelocityTracker mVelocity;
    
    private CoverFlowListener mListener;
    private DataCache<Integer, CoverImage> mCache;

    private CoverflowQuery aQuery;

    private List<String> imagesList;

    private List<CoverImage> images;

    private Activity mActivity;

    private EmptyImage emptyImage;
    private Background mBackground;

    private boolean showBlackBars;

    public CoverFlowOpenGL(Context context) {
        super(context);

        init();
	}

    public CoverFlowOpenGL(Context context, AttributeSet attrs){
        super(context, attrs);

        init();
    }

    public void setActivity(Activity activity){
        this.mActivity = activity;
        aQuery = new CoverflowQuery(mActivity);
    }

    public void init(){

        setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);

        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setZOrderMediaOverlay(true);
        setZOrderOnTop(true);

//        int cacheForVisibleTiles = (visibleTiles * 2 + 1) + 10; // visible_left + center + visible_right + 10 additional
        mCache = new DataCache<Integer, CoverImage>(maxTiles);//Math.min(maxTiles, cacheForVisibleTiles ));
        mLastOffset = 0;
        mOffset = 0;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glEnable(GL10.GL_TEXTURE_2D);			//Enable Texture Mapping ( NEW )
        gl.glShadeModel(GL10.GL_SMOOTH); 			//Enable Smooth Shading
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f); 	//Black Background
        gl.glDisable(GL10.GL_DEPTH_TEST); 			//Enables Depth Testing

        //Really Nice Perspective Calculations
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        mCache.clear();

        mWidth = w;
        mHeight = h;

        if(mBackground != null){
            mBackground.setGL(gl);
            mBackground.initBuffers(w, h);
            mBackground.loadGLTexture();
        }

        if(emptyImage != null){
            emptyImage.setGL(gl);
            emptyImage.setViewportData(mWidth, mHeight);
            emptyImage.setImageSize(imageSize);
            emptyImage.loadGLTexture();
        }

        if(images != null && images.size() > 0){
            for(CoverImage cImg : images){
                if(cImg != null){
                    cImg.setGL(gl);
                    cImg.setViewportData(mWidth, mHeight);
                    cImg.removeTexture();
                }
            }
        }

        float imagew = w * 0.45f / 2.0f;
        float imageh = h * 0.45f / 2.0f;
        mTouchRect = new RectF(w / 2 - imagew, h / 2 - imageh, w / 2 + imagew, h / 2 + imageh);

        gl.glViewport(0, 0, w, h); 	//Reset The Current Viewport
        gl.glMatrixMode(GL10.GL_PROJECTION); 	//Select The Projection Matrix
        gl.glLoadIdentity(); 					//Reset The Projection Matrix

        GLU.gluOrtho2D(gl, 0, w, 0, h);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

//        updateCache();
    }
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		switch(action) {
		case MotionEvent.ACTION_DOWN:
			touchBegan(event);
			return true;
		case MotionEvent.ACTION_MOVE:
			touchMoved(event);
			return true;
		case MotionEvent.ACTION_UP:
			touchEnded(event);
			return true;
		}
		return false;
	}

    private float checkValid(float off) {
        int max = imagesList.size() - 1;
        if (off < 0)
            return 0;
        else if (off > max)
            return max;

        return off;
    }
	
	private void touchBegan(MotionEvent event) {
		endAnimation();
		
		float x = event.getX();
		mTouchStartX = x;
		mTouchStartY = event.getY();
		mStartTime = System.currentTimeMillis();
		mStartOffset = mOffset;
		
		mTouchMoved = false;
		
		mTouchStartPos = (x / mWidth) * 10 - 5;
		mTouchStartPos /= 2;
		
		mVelocity = VelocityTracker.obtain();
		mVelocity.addMovement(event);
	}
	
	private void touchMoved(MotionEvent event) {
		float pos = (event.getX() / mWidth) * 10 - 5;
		pos /= 2;
		
		if (!mTouchMoved) {
			float dx = Math.abs(event.getX() - mTouchStartX);
			float dy = Math.abs(event.getY() - mTouchStartY);
			
			if (dx < TOUCH_MINIMUM_MOVE && dy < TOUCH_MINIMUM_MOVE)
				return ;
			
			mTouchMoved = true;
		}
		
		mOffset = checkValid(mStartOffset + mTouchStartPos - pos);
		
		requestRender();
		mVelocity.addMovement(event);
	}
	
	private void touchEnded(MotionEvent event) {
		float pos = (event.getX() / mWidth) * 10 - 5;
		pos /= 2;
		
		if (mTouchMoved) {
			mStartOffset += mTouchStartPos - pos;
			mStartOffset = checkValid(mStartOffset);
			mOffset = mStartOffset;
			
			mVelocity.addMovement(event);
			
			mVelocity.computeCurrentVelocity(1000);
			double speed = mVelocity.getXVelocity();
			speed = (speed / mWidth) * 10;
			if (speed > MAX_SPEED)
				speed = MAX_SPEED;
			else if (speed < -MAX_SPEED)
				speed = -MAX_SPEED;
			
			startAnimation(-speed);
		} else {
			if (mTouchRect.contains(event.getX(), event.getY())) {
				mListener.topTileClicked(this, (int) (mOffset + 0.01));
			}
		}
	}
	
	private void startAnimation(double speed) {
		if (mAnimationRunnable != null)
			return ;
		
		double delta = speed * speed / (FRICTION * 2);
		if (speed < 0)
			delta = -delta;
		
		double nearest = mStartOffset + delta;
		nearest = Math.floor(nearest + 0.5);
		nearest = checkValid((float) nearest);
		
		mStartSpeed = (float) Math.sqrt(Math.abs(nearest - mStartOffset) * FRICTION * 2);
		if (nearest < mStartOffset)
			mStartSpeed = -mStartSpeed;
		
		mDuration = Math.abs(mStartSpeed / FRICTION);
		mStartTime = AnimationUtils.currentAnimationTimeMillis();

		mAnimationRunnable = new Runnable() {
			@Override
			public void run() {
				driveAnimation();
			}
		};
		post(mAnimationRunnable);
	}
	
	private void driveAnimation() {
		float elapsed = (AnimationUtils.currentAnimationTimeMillis() - mStartTime) / 1000.0f;
		if (elapsed >= mDuration)
			endAnimation();
		else {
			updateAnimationAtElapsed(elapsed);
			post(mAnimationRunnable);
		}
	}
	
	private void endAnimation() {
		if (mAnimationRunnable != null) {
			mOffset = (float) Math.floor(mOffset + 0.5);
			mOffset = checkValid(mOffset);

			requestRender();
			
			removeCallbacks(mAnimationRunnable);
			mAnimationRunnable = null;

//            updateCache();
		}
	}
	
	private void updateAnimationAtElapsed(float elapsed) {
		if (elapsed > mDuration)
			elapsed = mDuration;
		
		float delta = Math.abs(mStartSpeed) * elapsed - FRICTION * elapsed * elapsed / 2;
		if (mStartSpeed < 0)
			delta = -delta;
		
		mOffset = checkValid(mStartOffset + delta);
		requestRender();
	}

//    private void updateCache(){
//        int diff = VISIBLE_TILES * 2 + 5;
//        int diffLeft = Math.max(0,(int)mOffset - diff);
//        int diffRight = Math.min(images.size(),(int)mOffset + diff);
//
//        for(int i = 0; i < images.size(); i++){
//            if(mCache.containsKey(i) && (i < diffLeft || i > diffRight)){
//                mCache.removeObjectForKey(i);
//            } else
//            if(!mCache.containsKey(i) && i >= diffLeft && i <= diffRight){
//                CoverImage img = images.get(i);
//                img.tryLoadTexture(dataChangedListener, i);
//                mCache.putObjectForKey(i, img);
//            }
//        }
//    }


    public int getMaxTiles() {
        return maxTiles;
    }

    public void setMaxTiles(int maxTiles) {
        this.maxTiles = maxTiles;
        mCache = new DataCache<Integer, CoverImage>(maxTiles);
    }

    public int getVisibleTiles() {
        return visibleTiles;
    }

    public void setVisibleTiles(int visibleTiles) {
        this.visibleTiles = visibleTiles;
    }

    public void setImageQuality(EQuality size){
        imageSize = size.getValue();
    }

    public void setImageShowBlackBars(boolean value){
        showBlackBars = value;
    }

    public void setImagesList(List<String> imagesList){
        this.imagesList = imagesList;

        if(imagesList != null && imagesList.size() > 0){
            images = new ArrayList<CoverImage>(imagesList.size());

            for(String imageUrl : imagesList){
                CoverImage ci = new CoverImage(mActivity, aQuery)
                        .setUrl(imageUrl)
                        .setImageSize(imageSize)
                        .setShowBlackBars(showBlackBars);
                images.add(ci);
            }
        }
    }

    public void setCoverFlowListener(CoverFlowListener listener) {
        mListener = listener;
    }

    public void setSelection(int position) {
        endAnimation();

        if(images != null && images.size() > 0){
            position = Math.min(position, images.size() - 1);
        }
        mOffset = position;

        requestRender();
    }

	public void setBackgroundRes(int res) {
        mBackground = new Background(mActivity, res);
	}

    public void setEmptyRes(int res) {
        emptyImage = new EmptyImage(mActivity, res);
    }

//    public void setBackgroundUrl(String url){
//        mBackground = new Background(mActivity, url);
//    }

//    public void setEmtpyUrl(String url){
//        emptyImage = new EmptyImage(mActivity, url);
//    }

	@Override
	public void onDrawFrame(GL10 gl) {
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        // clear Screen and Depth Buffer
        gl.glDisable(GL10.GL_DEPTH_TEST);
        gl.glClearColor(0, 0, 0, 0);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

        // Drawing
        gl.glTranslatef(0.0f, 0.0f, 0.0f);		// move 5 units INTO the screen

        if(mBackground != null){
            mBackground.draw(gl);
        }

        final float offset = mOffset;
        int i;

        int max = imagesList != null ? imagesList.size() - 1 : 0;
        int mid = (int) Math.floor(offset + 0.5);
        int iStartPos = mid - visibleTiles;

        if (iStartPos < 0)
            iStartPos = 0;
        // draw the left tiles
        for (i = iStartPos; i < mid; ++i) {
            drawTile(i, i - offset, gl);
        }

        // draw the right tiles
        int iEndPos = mid + visibleTiles;
        if (iEndPos > max)
            iEndPos = max;
        for (i = iEndPos; i >= mid; --i) {
            drawTile(i, i - offset, gl);
        }

        //draw the center tile
        if (mLastOffset != (int) offset) {
            mListener.tileOnTop(this, (int) offset);
            mLastOffset = (int) offset;
        }
	}

    private void drawTile(int position, float off, GL10 gl) {
        CoverImage cacheImg = mCache.objectForKey(position);

        boolean canDraw = false;

        if(cacheImg == null){
            cacheImg = images.get(position);
            cacheImg.tryLoadTexture(dataChangedListener, position);
            mCache.putObjectForKey(position, cacheImg);

            if (cacheImg.getTexture() != 0){
                canDraw = true;
            }
        } else if(cacheImg.getTexture() != 0){
            canDraw = true;
        }

        float desiredSize = canDraw ? cacheImg.getDesiredSize() : emptyImage.getDesiredSize();
        float spread = (mWidth - desiredSize) * 0.5f / visibleTiles;
        float trans = off * spread;
        float sc = 1.0f - (Math.abs(off) * 1 / (visibleTiles + 1));

        if(canDraw){
            cacheImg.draw(gl, trans, sc);
        } else {
            emptyImage.draw(gl, trans, sc);
        }
    }

    private DataChangedListener dataChangedListener = new DataChangedListener() {
        @Override
        public void imageUpdated(int position) {
            synchronized(this) {
                if(mOffset - visibleTiles < position || position < mOffset + visibleTiles  ){
                    requestRender();
                }
            }
        }
    };
}
