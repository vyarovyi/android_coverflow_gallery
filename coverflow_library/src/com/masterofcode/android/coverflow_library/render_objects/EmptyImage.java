package com.masterofcode.android.coverflow_library.render_objects;

import android.app.Activity;
import android.graphics.*;
import com.masterofcode.android.coverflow_library.R;

public class EmptyImage extends AbstractImage<EmptyImage> {

    public EmptyImage(Activity activity,  int resId){
        super(activity, resId);
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

        Bitmap bm = BitmapFactory.decodeResource(mActivity.getResources(), resId);

        int w = bm.getWidth();
        int h = bm.getHeight();

        float sx = Math.abs((float)imageSize / w);
        float sy = Math.abs((float)imageSize / h);

        float desiredScale = Math.min(sx, sy);

        Bitmap bitmap = Bitmap.createBitmap(imageSize, imageSize, showBlackBars ? Bitmap.Config.RGB_565 : Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bitmap);

        float left = (imageSize - w * desiredScale) / 2;
        float top = (imageSize - h * desiredScale) / 2;

        Matrix matrix = new Matrix();
        matrix.postScale(desiredScale, desiredScale);
        matrix.postTranslate(left, top);

        Paint paint = new Paint();
        paint.setFilterBitmap(true);

        cv.drawBitmap(bm, matrix, paint);

        loadGLTexture(bitmap);

        // Clean up
        bm.recycle();
    }
}
