package com.canon.ccapisample;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

class ImageProcessor {
    private LruCache<String, Bitmap> mMemoryCache;

    ImageProcessor() {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }
}
