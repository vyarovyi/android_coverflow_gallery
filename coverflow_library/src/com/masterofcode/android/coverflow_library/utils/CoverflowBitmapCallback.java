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

package com.masterofcode.android.coverflow_library.utils;

import android.content.Context;
import android.graphics.*;
import android.media.ExifInterface;
import android.view.View;
import android.widget.ImageView;
import com.androidquery.AQuery;
import com.androidquery.callback.AbstractAjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.util.AQUtility;
import com.androidquery.util.BitmapCache;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;


/**
 * The callback handler for handling Aquery.image() methods.
 *
 * @author skynet67
*/
public class CoverflowBitmapCallback extends AbstractAjaxCallback<Bitmap, CoverflowBitmapCallback> {

    private static int SMALL_MAX = 20;
    private static int BIG_MAX = 20;
    private static int SMALL_PIXELS = 50 * 50;
    private static int BIG_PIXELS = 400 * 400;
    private static int BIG_TPIXELS = 1000000;

    private static boolean DELAY_WRITE = false;

    private static Map<String, Bitmap> smallCache;
    private static Map<String, Bitmap> bigCache;
    private static Map<String, Bitmap> invalidCache;

    private static HashMap<String, WeakHashMap<ImageView, CoverflowBitmapCallback>> queueMap = new HashMap<String, WeakHashMap<ImageView, CoverflowBitmapCallback>>();

    private int targetWidth;
    private int fallback;
    private File imageFile;
    private Bitmap bm;
    private int animation;
    private Bitmap preset;
    private float ratio;
    private int round;
    private boolean targetDim = true;
    private float anchor = AQuery.ANCHOR_DYNAMIC;
    private boolean invalid;
    private boolean rotate;

    private Context mContext;

    private ImageLoadCallback mCallbackRunnable;

    private static boolean showBlackBars = true;

    /**
     * Instantiates a new bitmap ajax callback.
     */
    public CoverflowBitmapCallback(Context context, boolean showBlackBars, ImageLoadCallback callbackRunnable){
        this.mContext = context;
        this.mCallbackRunnable = callbackRunnable;
        this.showBlackBars = showBlackBars;
        type(Bitmap.class).memCache(true).fileCache(true).url("");
    }


    /**
     * Set the target width for downsampling.
     *
     * @param targetWidth the target width
     * @return self
     */
    public CoverflowBitmapCallback targetWidth(int targetWidth){
        this.targetWidth = targetWidth;
        return this;
    }


    /**
     * Set the image source file.
     *
     * @param imageFile the image file
     * @return self
     */
    public CoverflowBitmapCallback file(File imageFile){
        this.imageFile = imageFile;
        return this;
    }

    /**
     * Set the preset bitmap. This bitmap will be shown immediately until the ajax callback returns the final image from the url.
     *
     * @param preset the preset
     * @return self
     */
    public CoverflowBitmapCallback preset(Bitmap preset){

        this.preset = preset;
        return this;
    }

    /**
     * Set the bitmap. This bitmap will be shown immediately with aspect ratio.
     *
     * @param bm
     * @return self
     */
    public CoverflowBitmapCallback bitmap(Bitmap bm){
        this.bm = bm;
        return this;
    }

    /**
     * Set the fallback image in resource id.
     *
     * @param resId the res id
     * @return self
     */
    public CoverflowBitmapCallback fallback(int resId){
        this.fallback = resId;
        return this;
    }

    /**
     * Set the animation resource id, or AQuery.FADE_IN.
     *
     * @param animation the animation
     * @return self
     */
    public CoverflowBitmapCallback animation(int animation){
        this.animation = animation;
        return this;
    }

    /**
     * Set the image aspect ratio (height / width).
     *
     * @param ratio the ratio
     * @return self
     */
    public CoverflowBitmapCallback ratio(float ratio){
        this.ratio = ratio;
        return this;
    }

    /**
     * Set auto rotate to respect image Exif orientation.
     *
     * @param rotate rotate
     * @return self
     */
    public CoverflowBitmapCallback rotate(boolean rotate){
        this.rotate = rotate;
        return this;
    }


    /**
     * Set the image aspect ratio anchor.
     *
     * Value of 1 implies show top end of the image, 0 implies at the center, -1 implies show at the bottom.
     *
     * A special value AQuery.ANCHOR_DYNAMIC will adjust the anchor base.
     * This setting will add up from 0 to 0.5 bias and it's suitable for portraits and common photos.
     *
     * Default value is ANCHOR_DYNAMIC.
     *
     * @param anchor the anchor
     * @return self
     */

    public CoverflowBitmapCallback anchor(float anchor){
        this.anchor = anchor;

        return this;
    }

    /**
     * Set the round corner radius.
     *
     * Note that the current implementation transform the image to a new one and will use more transient resources.
     *
     * @param radius
     * @return self
     */


    public CoverflowBitmapCallback round(int radius){
        this.round = radius;
        return this;
    }


    private static Bitmap decode(String path, byte[] data, BitmapFactory.Options options, boolean rotate){

        Bitmap result = null;


        if(path != null){

            result = decodeFile(path, options, rotate);

        }else if(data != null){

            result = BitmapFactory.decodeByteArray(data, 0, data.length, options);

        }

        if(result == null && options != null && !options.inJustDecodeBounds){
            AQUtility.debug("decode image failed", path);
        }

        return result;
    }

    private static Bitmap decodeFile(String path, BitmapFactory.Options options, boolean rotate){

        Bitmap result = null;

        if(options == null){
            options = new BitmapFactory.Options();
        }

        options.inInputShareable = true;
        options.inPurgeable = true;

        FileInputStream fis = null;

        try{

            fis = new FileInputStream(path);
            FileDescriptor fd = fis.getFD();
            result = BitmapFactory.decodeFileDescriptor(fd, null, options);

            if(result != null && rotate){
                result = rotate(path, result);
            }

        }catch(IOException e){
            AQUtility.report(e);
        }finally{
            AQUtility.close(fis);
        }

        return result;

    }

    private static Bitmap rotate(String path, Bitmap bm){

        if(bm == null) return null;

        Bitmap result = bm;

        int ori = ExifInterface.ORIENTATION_NORMAL;

        try{
            ExifInterface ei = new ExifInterface(path);
            ori = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        }catch(Exception e){
            //simply fallback to normal orientation
            AQUtility.debug(e);
        }

        if(ori > 0){

            Matrix matrix = getRotateMatrix(ori);
            result = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);

            AQUtility.debug("before", bm.getWidth() + ":" + bm.getHeight());
            AQUtility.debug("after", result.getWidth() + ":" + result.getHeight());

            if(bm != result){
                bm.recycle();
            }
        }


        return result;
    }

    private static Matrix getRotateMatrix(int ori){

        Matrix matrix = new Matrix();
        switch (ori) {
            case 2:
                matrix.setScale(-1, 1);
                break;
            case 3:
                matrix.setRotate(180);
                break;
            case 4:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case 5:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case 6:
                matrix.setRotate(90);
                break;
            case 7:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case 8:
                matrix.setRotate(-90);
                break;

        }

        return matrix;

    }

    public static Bitmap getResizedImage(String path, byte[] data, int target, boolean width, int round, boolean showBlackBars){
        return getResizedImage(path, data, target, width, round, false, showBlackBars);
    }

    /**
     * Utility method for downsampling images.
     *
     * @param path the file path
     * @param data if file path is null, provide the image data directly
     * @param target the target dimension
     * @param width use width as target, otherwise use the higher value of height or width
     * @param round corner radius
     * @param rotate auto rotate with exif data
     * @return the resized image
     */
    public static Bitmap getResizedImage(String path, byte[] data, int target, boolean width, int round, boolean rotate, boolean showBlackBars){

        if(path == null && data == null) return null;

        BitmapFactory.Options options = null;

        float desiredScale = 1.0f;

        if(target > 0){

            BitmapFactory.Options info = new BitmapFactory.Options();
            info.inJustDecodeBounds = true;

            decode(path, data, info, rotate);

//            int dim = info.outWidth;
//            if(!width) dim = Math.max(dim, info.outHeight);
//            int ssize = sampleSize(dim, target);
//
            int ssize = sampleSize(info.outWidth, info.outHeight, target);

            options = new BitmapFactory.Options();
            options.inSampleSize = ssize;
            options.inJustDecodeBounds = false;
            options.inDither = false;
            options.inScaled = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        }

        try{
            Bitmap bm = decode(path, data, options, rotate);

            int w = bm.getWidth();
            int h = bm.getHeight();

            float desiredScaleWidth = (float) target / w;
            float desiredScaleHeight = (float) target/ h;
            desiredScale = Math.min(desiredScaleWidth, desiredScaleHeight);

            float realTexWidth =  w * desiredScale;
            float realTexHeight = h * desiredScale;

            if((w == target || h == target)){
                realTexWidth = w;
                realTexHeight = h;
                desiredScale = 1.0f;
            }

            Bitmap bmpWithBorders = Bitmap.createBitmap(target, target, showBlackBars ? Bitmap.Config.RGB_565 : Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(bmpWithBorders);

            float left = (target - realTexWidth) / 2;
            float top = (target - realTexHeight) / 2;

            Matrix matrix = new Matrix();
            matrix.postScale(desiredScale, desiredScale);
            matrix.postTranslate(left, top);

            Paint paint = new Paint();
            paint.setFilterBitmap(true);

            cv.drawBitmap(bm, matrix, paint);

            bm.recycle();

            if(round > 0){
                bmpWithBorders = getRoundedCornerBitmap(bmpWithBorders , round);
            }

            return bmpWithBorders;

        }catch(OutOfMemoryError e){
            clearCache();
            AQUtility.report(e);
        }

        return null;

    }


    private static int sampleSize(int width, int target){


        int result = 1;

        for(int i = 0; i < 10; i++){

            if(width < target * 2){
                break;
            }

            width = width / 2;
            result = result * 2;

        }

        return result;
    }

    private static int sampleSize(int width, int height, int target){
        double factorW = (float)width/(float)(target);
        double factorH = (float)height/(float)(target);

        double factor = Math.max(factorW, factorH);

        return (int) Math.pow(2, Math.floor(Math.sqrt(factor)));
    }

    private Bitmap bmGet(String path, byte[] data){
        return getResizedImage(path, data, targetWidth, targetDim, round, rotate, true);

    }

    @Override
    protected File accessFile(File cacheDir, String url){

        if(imageFile != null && imageFile.exists()){
            return imageFile;
        }

        return super.accessFile(cacheDir, url);
    }


    @Override
    protected Bitmap fileGet(String url, File file, AjaxStatus status) {
        return bmGet(file.getAbsolutePath(), null);
    }



    @Override
    public Bitmap transform(String url, byte[] data, AjaxStatus status) {

        String path = null;

        File file = null;

        try {
            Field field = status.getClass().getDeclaredField("file"); //NoSuchFieldException
            field.setAccessible(true);
            file = (File) field.get(status); //IllegalAccessException
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }


//        File file = status.getFile();
        if(file != null){
            path = file.getAbsolutePath();
        }

        Bitmap bm = bmGet(path, data);

        if(bm == null){

            if(fallback > 0){
                bm = getFallback();
            }else if(fallback == AQuery.GONE || fallback == AQuery.INVISIBLE){
                bm = dummy;
            }else if(fallback == AQuery.PRESET){
                bm = preset;
            }

            if(status.getCode() != 200){
                invalid = true;
            }
        }


        return bm;
    }




    private Bitmap getFallback(){

        Bitmap bm = null;

        String key = Integer.toString(fallback);
        bm = memGet(key);

        if(bm == null){
            bm = BitmapFactory.decodeResource(mContext.getResources(), fallback);

            if(bm != null){
                memPut(key, bm);
            }
        }

        return bm;
    }


    public static Bitmap getMemoryCached(Context context, int resId){

        String key = Integer.toString(resId);
        Bitmap bm = memGet(key, 0, 0);

        if(bm == null){
            bm = BitmapFactory.decodeResource(context.getResources(), resId);

            if(bm != null){
                memPut(key, 0, 0, bm, false);
            }
        }

        return bm;
    }

    private static Bitmap empty = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8);
    public static Bitmap getEmptyBitmap(){
        return empty;
    }

    private static Bitmap dummy = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8);



    @Override
    public final void callback(String url, Bitmap bm, AjaxStatus status) {
        if(mCallbackRunnable != null){
            mCallbackRunnable.onLoad(bm);
        }
    }

    @Override
    protected void skip(String url, Bitmap bm, AjaxStatus status){
        queueMap.remove(url);
    }

    /**
     * Sets the icon cache size in count. Icons are images less than 50x50 pixels.
     *
     * @param limit the new icon cache limit
     */
    public static void setIconCacheLimit(int limit){
        SMALL_MAX = limit;
        clearCache();
    }

    /**
     * Sets the cache limit in count.
     *
     * @param limit the new cache limit
     */
    public static void setCacheLimit(int limit){
        BIG_MAX = limit;
        clearCache();
    }

    /**
     * Sets the file cache write policy. If set to true, images load from network will be served quicker before caching to disk,
     * this however increase the chance of out of memory due to memory allocation.
     *
     * Default is false.
     *
     */
    public static void setDelayWrite(boolean delay){
        DELAY_WRITE = delay;
    }



    /**
     * Sets the pixel limit per image. Image larger than limit will not be memcached.
     *
     * @param pixels the new pixel limit
     */
    public static void setPixelLimit(int pixels){
        BIG_PIXELS = pixels;
        clearCache();
    }

    /**
     * Sets the pixel criteria for small images. Small images are cached in a separate cache.
     *
     * Default is 50x50 (2500 pixels)
     *
     * @param pixels the small image pixel criteria
     */
    public static void setSmallPixel(int pixels){
        SMALL_PIXELS = pixels;
        clearCache();
    }

    /**
     * Sets the max pixel limit for the entire memcache. LRU images will be expunged if max pixels limit is reached.
     *
     * @param pixels the new max pixel limit
     */
    public static void setMaxPixelLimit(int pixels){
        BIG_TPIXELS = pixels;
        clearCache();
    }

    /**
     * Clear the bitmap memcache.
     */
    public static void clearCache(){
        bigCache = null;
        smallCache = null;
        invalidCache = null;
    }

    protected static void clearTasks(){
        queueMap.clear();
    }

    private static Map<String, Bitmap> getBCache(){
        if(bigCache == null){
            bigCache = Collections.synchronizedMap(new BitmapCache(BIG_MAX, BIG_PIXELS, BIG_TPIXELS));
        }
        return bigCache;
    }


    private static Map<String, Bitmap> getSCache(){
        if(smallCache == null){
            smallCache = Collections.synchronizedMap(new BitmapCache(SMALL_MAX, SMALL_PIXELS, 250000));
        }
        return smallCache;
    }

    private static Map<String, Bitmap> getICache(){
        if(invalidCache == null){
            invalidCache = Collections.synchronizedMap(new BitmapCache(100, BIG_PIXELS, 250000));
        }
        return invalidCache;
    }

    @Override
    protected Bitmap memGet(String url){
        if(bm != null) return bm;
        if(!memCache) return null;
        return memGet(url, targetWidth, round);
    }

    /**
     * Check if the bitmap is memory cached.
     *
     * @param url the url
     * @return if the url is memcached
     */
    public static boolean isMemoryCached(String url){
        return getBCache().containsKey(url) || getSCache().containsKey(url) || getICache().containsKey(url);
    }

    /**
     * Gets the memory cached bitmap.
     *
     * @param url the url
     * @param targetWidth the target width, 0 for non downsampling
     * @return the memory cached bitmap
     */
    public static Bitmap getMemoryCached(String url, int targetWidth){
        return memGet(url, targetWidth, 0);
    }

    private static Bitmap memGet(String url, int targetWidth, int round){

        url = getKey(url, targetWidth, round);

        Map<String, Bitmap> cache = getBCache();
        Bitmap result = cache.get(url);

        if(result == null){
            cache = getSCache();
            result = cache.get(url);
        }

        if(result == null){
            cache = getICache();
            result = cache.get(url);

            if(result != null){

                if(getLastStatus() == 200){
                    invalidCache = null;
                    result = null;
                }

            }
        }

        return result;
    }

    private static String getKey(String url, int targetWidth, int round){

        if(targetWidth > 0){
            url += "#" + targetWidth;
        }

        if(round > 0){
            url += "#" + round;
        }

        return url;
    }

    private static void memPut(String url, int targetWidth, int round, Bitmap bm, boolean invalid){

        if(bm == null) return;

        int pixels = bm.getWidth() * bm.getHeight();

        Map<String, Bitmap> cache = null;

        if(invalid){
            cache = getICache();
        }else if(pixels <= SMALL_PIXELS){
            cache = getSCache();
        }else{
            cache = getBCache();
        }

        if(targetWidth > 0 || round > 0){

            String key = getKey(url, targetWidth, round);
            cache.put(key, bm);

            //to indicate that the variant of that url is cached by puting and empty value
            if(!cache.containsKey(url)){
                cache.put(url, null);
            }

        }else{
            cache.put(url, bm);
        }



    }


    @Override
    protected void memPut(String url, Bitmap bm){
        memPut(url, targetWidth, round, bm, invalid);
    }


    private static Bitmap filter(View iv, Bitmap bm, int fallback){
        //ignore 1x1 pixels
        if(bm != null && bm.getWidth() == 1 && bm.getHeight() == 1 && bm != empty){
            bm = null;
        }

        if(bm != null){
            iv.setVisibility(View.VISIBLE);
        }else if(fallback == AQuery.GONE){
            iv.setVisibility(View.GONE);
        }else if(fallback == AQuery.INVISIBLE){
            iv.setVisibility(View.INVISIBLE);
        }

        return bm;
    }


     @Override
    public void async(Context context){


        String url = getUrl();

        if(url == null){
            showProgress(false);
            return;
        }

        Bitmap bm = memGet(url);
        if(bm != null){
            status = new CoverflowAjaxStatus().source(AjaxStatus.MEMORY).done();
            callback(url, bm, status);
            return;
        }


        if(!queueMap.containsKey(url)){
            super.async(context);
        }else{
            showProgress(true);
        }


    }

    @Override
    protected boolean isStreamingContent(){
        return !DELAY_WRITE;
    }

    private void addQueue(String url, ImageView iv){


        WeakHashMap<ImageView, CoverflowBitmapCallback> ivs = queueMap.get(url);

        if(ivs == null){

            if(queueMap.containsKey(url)){
                //already a image view fetching
                ivs = new WeakHashMap<ImageView, CoverflowBitmapCallback>();
                ivs.put(iv, this);
                queueMap.put(url, ivs);
            }else{
                //register a view by putting a url with no value
                queueMap.put(url, null);
            }

        }else{
            //add to list of image views
            ivs.put(iv, this);

        }

    }

    private static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = pixels;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    public interface ImageLoadCallback{
        public void onLoad(Bitmap bitmap);
    }
}
