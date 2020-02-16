package com.canon.ccapisample;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Locale;

public class ProgressDialogFragment extends DialogFragment{
    enum Type{
        Circular,
        Bar,
    }

    private static final String TAG = ProgressDialogFragment.class.getSimpleName();
    private static final String CANCELABLE = "Cancelable";
    private static final String TYPE = "Type";
    private static final String TITLE = "Title";
    private static final String URL = "Url";

    private ProgressBar mProgressBar = null;
    private TextView mProgressTextView = null;
    private TextView mProgressPercentTextView = null;
    private Handler mHandler = null;
    private Boolean mDismiss = false;

    public static ProgressDialogFragment newInstance(Type type, String title){
        ProgressDialogFragment instance = new ProgressDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean(CANCELABLE, false);
        arguments.putSerializable(TYPE, type);
        arguments.putString(TITLE, title);
        instance.setArguments(arguments);
        return instance;
    }

    public static ProgressDialogFragment newInstance(Type type, String title, String requestUrl){
        ProgressDialogFragment instance = new ProgressDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean(CANCELABLE, true);
        arguments.putSerializable(TYPE, type);
        arguments.putString(TITLE, title);
        arguments.putString(URL, requestUrl);
        instance.setArguments(arguments);
        return instance;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle arguments) {
        Log.d(TAG, "onCreateDialog");

        mHandler = new Handler();
        Boolean cancelable = getArguments().getBoolean(CANCELABLE);
        Type type = (Type) getArguments().getSerializable(TYPE);
        String title = getArguments().getString(TITLE);
        final String requestUrl = getArguments().getString(URL);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle(title);

        if(arguments != null){
            // Discard the dialog which is reconstructed by the onResume() when the Activity is discarded.
            Log.d(TAG, "onCreateDialog : arguments != null");
            mDismiss = true;
        }
        else{
            Log.d(TAG, "onCreateDialog : arguments == null");
        }

        if (type != null) {
            View view;
            switch (type) {
                case Bar:
                    view = getActivity().getLayoutInflater().inflate(R.layout.fragment_dialog_progress_bar, null);
                    break;
                case Circular:
                default:
                    view = getActivity().getLayoutInflater().inflate(R.layout.fragment_dialog_progress_circular, null);
                    break;
            }
            mProgressBar = view.findViewById(R.id.ProgressBar);
            mProgressTextView = view.findViewById(R.id.ProgressTextView);
            mProgressPercentTextView = view.findViewById(R.id.ProgressPercentTextView);
            dialogBuilder.setView(view);
        }

        if(cancelable) {
            dialogBuilder.setNegativeButton("Cancel", null);
        }

        Dialog dialog = dialogBuilder.create();
        this.setCancelable(false);

        if(cancelable) {
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    Button negative = ((AlertDialog)dialogInterface).getButton(AlertDialog.BUTTON_NEGATIVE);

                    negative.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (mProgressBar.getProgress() < mProgressBar.getMax()) {
                                WebAPI.getInstance().cancelFileDownload(requestUrl);
                            }
                            else{
                                dismiss();
                            }
                        }
                    });
                }
            });
        }

        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        if(mDismiss){
            Log.d(TAG, "onResume : mDismiss !!");
            dismiss();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    public void onStop(){
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
    }

    public void updateProgress(final int max, final int progress) {
        if(mHandler != null) {
            mHandler.post(new Runnable() {
                public void run() {
                    if (mProgressBar != null) {
                        mProgressBar.setMax(max);
                        mProgressBar.setProgress(progress);

                        if (mProgressPercentTextView != null) {
                            float percent = (float) progress / (float) max * 100;
                            mProgressPercentTextView.setText(String.format(Locale.US, "%d%%", (int) percent));
                        }
                    }
                }
            });
        }
    }

    public void stopProgress(){
        if(getFragmentManager() != null && mProgressBar != null){
            Log.d(TAG, "stopProgress : getFragmentManager() != null");
            this.setCancelable(true);
            mProgressBar.setProgress(mProgressBar.getMax());
        }
    }

    public void setMessage(String message){
        if(mProgressTextView != null) {
            mProgressTextView.setText(message);
        }
    }
}
