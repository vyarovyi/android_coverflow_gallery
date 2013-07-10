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

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import com.androidquery.AQuery;

import java.io.File;

/**
 * Wrapper around main AQuery.
 *
 * @author skynet67
*/
public class CoverflowQuery extends AQuery {

    boolean showBlackBars = true;

    public CoverflowQuery(Activity act) {
        super(act);
    }

    public CoverflowQuery(View view) {
        super(view);
    }

    public CoverflowQuery(Context context) {
        super(context);
    }

    public CoverflowQuery(Activity act, View root) {
        super(act, root);
    }

    public CoverflowQuery setShowBlackBars(boolean value){
        this.showBlackBars = value;
        return this;
    }

    /**
     * Set the image of an ImageView with a custom callback.
     *
     * @param callback Callback handler for setting the image.
     * @return self
     */


    /**
     * Set the image of an ImageView with a custom callback.
     *
     * @param url The image url.
     * @param memCache Use memory cache.
     * @param fileCache Use file cache.
     * @param targetWidth Target width for down sampling when reading large images.
     * @param resId Fallback image if result is network fetch and image convert failed.
     * @param callback Callback handler for setting the image.
     * @return self
     *
     */
    public CoverflowQuery image(String url, boolean memCache, boolean fileCache, int targetWidth, int resId, CoverflowBitmapCallback callback){

        callback.targetWidth(targetWidth).fallback(resId)
                .url(url).memCache(memCache).fileCache(fileCache);

        return image(callback);
    }

    public CoverflowQuery image(CoverflowBitmapCallback callback){

        invoke(callback);
        return this;
    }

    /**
     * Return bitmap cached by image requests. Returns null if url is not cached.
     *
     * @param url
     * @param targetWidth The desired downsampled width.
     *
     * @return Bitmap
     */
    @Override
    public Bitmap getCachedImage(String url, int targetWidth){

        Bitmap result = CoverflowBitmapCallback.getMemoryCached(url, targetWidth);
        if(result == null){
            File file = getCachedFile(url);
            if(file != null){
                result = CoverflowBitmapCallback.getResizedImage(file.getAbsolutePath(), null, targetWidth, true, 0, showBlackBars);
            }
        }

        return result;
    }

    /**
     * Return cached bitmap with a resourceId. Returns null if url is not cached.
     *
     * Use this method instead of BitmapFactory.decodeResource(getResources(), resId) for caching.
     *
     * @param resId
     *
     * @return Bitmap
     */
    @Override
    public Bitmap getCachedImage(int resId){
        return CoverflowBitmapCallback.getMemoryCached(getContext(), resId);
    }
}
