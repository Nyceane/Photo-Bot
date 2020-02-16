package com.canon.ccapisample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.List;

import static com.canon.ccapisample.Constants.CCAPI.Method.GET;

class ContentsAdapter extends ArrayAdapter<ContentsDataSet> implements GridView.OnScrollListener{
    private static final String TAG = ContentsAdapter.class.getSimpleName();
    private LayoutInflater mLayoutInflater;
    private ImageProcessor mImageProcessor;
    private Boolean mScrolling = false;

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        switch (scrollState) {
            case GridView.OnScrollListener.SCROLL_STATE_IDLE:
                int first = view.getFirstVisiblePosition();
                int count = view.getChildCount();

                Log.d(TAG, String.format("onScrollStateChanged : first=%d, count=%d", first, count));

                for (int i = 0; i < count; i++) {
                    View v = view.getChildAt(i);
                    ContentsDataSet content = super.getItem(first + i);
                    ViewHolder holder = (ViewHolder) v.getTag();

                    if(content != null) {
                        if (content.getKind() != ContentsDataSet.Kind.DIR) {
                            String url = content.getUrl() + "?kind=thumbnail";
                            holder.mImageView.setTag(url);

                            final Bitmap bitmap = mImageProcessor.getBitmapFromMemCache(url);
                            if (bitmap != null) {
                                Log.d(TAG, "onScrollStateChanged Cache enabled : " + url);
                                holder.mImageView.setImageBitmap(bitmap);
                            }
                            else {
                                Log.d(TAG, "onScrollStateChanged Cache disabled : " + url);
                                holder.mImageView.setImageBitmap(createVoidImage());
                                setImage(url, holder.mImageView);
                            }

                            holder.mTextView.setText(content.getFolder() + "/" + content.getName());
                        }
                        else {
                            holder.mTextView.setText(content.getName());
                            holder.mImageView.setImageBitmap(createVoidImage());
                        }
                    }
                    mScrolling = false;
                }
                break;
            case GridView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                mScrolling = true;
                break;
            case GridView.OnScrollListener.SCROLL_STATE_FLING:
                mScrolling = true;
                break;
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }

    private static class ViewHolder {
        ImageView mImageView;
        TextView mTextView;
        ContentsDataSet mContentsDataSet;
    }

    ContentsAdapter(Context context, List<ContentsDataSet> contentsList, ImageProcessor imageProcessor){
        super(context, 0, contentsList);
        this.mImageProcessor = imageProcessor;
        this.mLayoutInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        ContentsDataSet content = super.getItem(position);

        if(content != null) {
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.grid_contents, null);
                holder = new ViewHolder();
                holder.mImageView = convertView.findViewById(R.id.ContentsImageView);
                holder.mTextView = convertView.findViewById(R.id.ContentsTextView);
                holder.mContentsDataSet = content;
                convertView.setTag(holder);
            }
            else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (content.getKind() != ContentsDataSet.Kind.DIR) {
                String url = content.getUrl() + "?kind=thumbnail";
                holder.mImageView.setTag(url);

                if( !mScrolling ) {
                    final Bitmap bitmap = mImageProcessor.getBitmapFromMemCache(url);

                    if (bitmap != null) {
                        Log.d(TAG, "getView Cache enabled : " + url);
                        holder.mImageView.setImageBitmap(bitmap);
                    }
                    else {
                        Log.d(TAG, "getView Cache disabled : " + url);
                        holder.mImageView.setImageBitmap(createVoidImage());
                        setImage(url, holder.mImageView);
                    }
                }
                else{
                    holder.mImageView.setImageBitmap(createVoidImage());
                }

                holder.mTextView.setText(content.getFolder() + "/" + content.getName());
            }
            else {
                holder.mTextView.setText(content.getName());
                holder.mImageView.setImageBitmap(createVoidImage());
            }
        }

        return convertView;
    }

    private void setImage(String url, ImageView imageView){
        final WeakReference<ImageView> imageViewWeakReference = new WeakReference<ImageView>(imageView);

        WebAPI webAPI = WebAPI.getInstance();
        Bundle args = new Bundle();
        String[] params = new String[]{ GET, url, null };
        args.putStringArray(Constants.RequestCode.ACT_WEB_API.name(), params);

        webAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.ACT_WEB_API, args, new WebAPIResultListener() {
            @Override
            public void onWebAPIResult(WebAPIResultDataSet result) {
                final ImageView imageView = imageViewWeakReference.get();

                Bitmap bitmap;
                if(result.isError()){
                    Log.d(TAG, "Error : " + result.getUrl());
                    bitmap = createVoidImage();
                }
                else {
                    bitmap = result.getImageResponseBody();
                    if (bitmap != null) {
                        Log.d(TAG, String.format("Add Cache : %s", result.getUrl()));
                        mImageProcessor.addBitmapToMemoryCache(result.getUrl(), bitmap);
                    }
                    else{
                        Log.d(TAG, String.format("Binary cannot decode : %s", result.getUrl()));
                        bitmap = createVoidImage();
                    }
                }

                if (imageView != null && result.getUrl().equals(imageView.getTag())) {
                    Log.d(TAG, String.format("setImageBitmap : %s", result.getUrl()));
                    imageView.setImageBitmap(bitmap);
                }
                else {
                    Log.d(TAG, String.format("Reuse ImageView : %s", result.getUrl()));
                }
            }
        }));
    }

    private Bitmap createVoidImage(){
        Bitmap bitmap = Bitmap.createBitmap( 160, 120, Bitmap.Config.RGB_565 );
        Paint paint = new Paint();
        Canvas canvas = new Canvas(bitmap);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), paint);
        return bitmap;
    }
}
