package com.canon.ccapisample;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

public class MessageDialogFragment extends DialogFragment {
    private static final String TITLE = "Title";
    private static final String MESSAGE = "Message";
    private static final String PARAMS = "Params";

    public static MessageDialogFragment newInstance(String title, String message){
        MessageDialogFragment instance = new MessageDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(TITLE, title);
        arguments.putString(MESSAGE, message);
        instance.setArguments(arguments);
        return instance;
    }

    public static MessageDialogFragment newInstance(String title, String message, Bundle params, Fragment target){
        MessageDialogFragment instance = new MessageDialogFragment();
        instance.setTargetFragment(target, 0);
        Bundle arguments = new Bundle();
        arguments.putString(TITLE, title);
        arguments.putString(MESSAGE, message);
        arguments.putBundle(PARAMS, params);
        instance.setArguments(arguments);
        return instance;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle arguments) {
        String title = getArguments().getString(TITLE);
        String message = getArguments().getString(MESSAGE);
        final Bundle params = getArguments().getBundle(PARAMS);
        final Fragment target = getTargetFragment();

        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
        dialog.setTitle(title);
        dialog.setMessage(message);

        if (target != null && params != null) {
            dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });

            dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent data = new Intent();
                    data.putExtra(Intent.EXTRA_RESTRICTIONS_BUNDLE, params);
                    target.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, data);
                }
            });
        }

        return dialog.create();
    }

    @Override
    public void onPause() {
        super.onPause();
        dismiss();
    }
}
