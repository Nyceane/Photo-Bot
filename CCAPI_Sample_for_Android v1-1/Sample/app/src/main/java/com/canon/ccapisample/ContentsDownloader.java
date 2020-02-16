package com.canon.ccapisample;

import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.canon.ccapisample.Constants.CCAPI.Method.GET;

class ContentsDownloader {
    private static final String TAG = ContentsDownloader.class.getSimpleName();
    private File mSaveToDir;
    private ProgressDialogFragment mProgressDialog = null;
    private FileOutputStream mOutputStream = null;
    private Handler mHandler;

    ContentsDownloader(Handler handler){
        mHandler = handler;
    }

    void execute(final FragmentActivity context, final String fileName, final String url, final String dialogTitle, final WebAPIResultListener webAPIResultListener){
        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        mSaveToDir = new File(root, "CCAPI_Sample");
        if(!mSaveToDir.exists()){
            boolean ret = mSaveToDir.mkdirs();
            if(ret) {
                Log.d(TAG, "CCAPI_Sample mkdir.");
            }
        }

        final Bundle args = new Bundle();
        String[] params = new String[]{ GET, url, null };
        args.putStringArray(Constants.RequestCode.ACT_WEB_API.name(), params);

        Log.d(TAG, "Download Start.");

        if (isExternalStorageWritable()) {
            try {
                if(mOutputStream != null){
                    mOutputStream.close();
                }

                File file = new File(mSaveToDir, fileName);
                mOutputStream = new FileOutputStream(file);
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog = ProgressDialogFragment.newInstance(ProgressDialogFragment.Type.Bar, dialogTitle, url);
                    FragmentTransaction fragmentTransaction = context.getSupportFragmentManager().beginTransaction();
                    fragmentTransaction.add(mProgressDialog, null);
                    fragmentTransaction.commitAllowingStateLoss();

                    // Request to get images.
                    // Execute the save processing of images in the onHttpProgressing().
                    WebAPI.getInstance().enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.ACT_WEB_API, args, new WebAPIResultListener() {
                        @Override
                        public void onWebAPIResult(WebAPIResultDataSet result) {
                            Log.d(TAG, "Download End.");

                            if (mOutputStream != null) {
                                try {
                                    mOutputStream.close();
                                    mOutputStream = null;
                                }
                                catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            if(mProgressDialog != null) {
                                if (result.isError()) {
                                    // The dialog does not close when an error occurred.
                                    String message = "";
                                    mProgressDialog.stopProgress();

                                    if (result.isCancel()) {
                                        message = "Download Canceled.";
                                    }
                                    else {
                                        message = result.getErrorMsg();
                                    }

                                    File file = new File(mSaveToDir, fileName);
                                    if(!file.delete()){
                                        Log.d(TAG, fileName + " : delete error.");
                                    }
                                    mProgressDialog.setMessage(message);
                                }
                                else {
                                    // Scan a media to let the system recognize the saved file.
                                    File file = new File(mSaveToDir, fileName);
                                    String[] paths = { file.getAbsolutePath() };
                                    MediaScannerConnection.scanFile(context,
                                            paths,
                                            null,
                                            new MediaScannerConnection.OnScanCompletedListener() {
                                                @Override
                                                public void onScanCompleted(String path, Uri uri) {
                                                    Log.d("MediaScannerConnection", "Scanned " + path + ":");
                                                    Log.d("MediaScannerConnection", "-> uri=" + uri);
                                                }
                                            }
                                    );

                                    mProgressDialog.dismissAllowingStateLoss();
                                    mProgressDialog = null;
                                }
                            }

                            if(webAPIResultListener != null) {
                                webAPIResultListener.onWebAPIResult(result);
                            }
                        }
                    }, new HttpProgressListener() {
                        @Override
                        public void onHttpProgressing(int max, int progress, byte[] progressBytes) {

                            if (progressBytes != null && progressBytes.length != 0 && mOutputStream != null){
                                try {
                                    mOutputStream.write(progressBytes);
                                }
                                catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (mProgressDialog != null) {
                                mProgressDialog.updateProgress(max, progress);
                            }
                            else{
                                Log.d(TAG, "mProgressDialog is NULL");
                            }
                        }
                    }));
                }
            });
        }
    }

    void execute(FragmentActivity context, final String fileName, String url){
        execute(context, fileName, url, fileName, null);
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}
